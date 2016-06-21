/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.tasks.*
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.api.tasks.incremental.InputFileDetails
import org.jetbrains.kotlin.gradle.plugin.kotlinDebug
import java.io.File
import java.util.*
import kotlin.properties.Delegates

open class SyncOutputTask : SourceTask() {
    @get:InputDirectory
    var kotlinOutputDir: File by Delegates.notNull()
    var javaOutputDir: File by Delegates.notNull()

    private val workingDir = File(project.buildDir, name).apply { mkdirs() }
    private val snapshotFile = File(workingDir, SNAPSHOT_FILE_NAME)
    private val kotlinClassesInJavaOutputDir: MutableSet<File> = readSnapshot(snapshotFile)

    @Suppress("unused")
    @OutputFiles
    fun getOutputFiles(): Collection<File> =
            kotlinClassesInJavaOutputDir

    @Suppress("unused")
    @TaskAction
    fun execute(inputs: IncrementalTaskInputs): Unit {
        // todo make it work incrementally
        processNonIncrementally()
        saveSnapshot(snapshotFile, kotlinClassesInJavaOutputDir)
    }

    private fun processNonIncrementally() {
        logger.kotlinDebug { "Non-incremental copying files from $kotlinOutputDir to $javaOutputDir" }

        kotlinClassesInJavaOutputDir.forEach {
            it.delete()
        }
        snapshotFile.delete()
        kotlinClassesInJavaOutputDir.clear()

        kotlinOutputDir.walkTopDown().forEach {
            if (it.isFile) {
                copy(it, it.siblingInJavaDir)
            }
        }
    }

    @Suppress("unused")
    private fun processIncrementally(input: InputFileDetails) {
        val fileInKotlinDir = input.file
        val fileInJavaDir = fileInKotlinDir.siblingInJavaDir

        // TODO: check if snapshot is consistent (if file is modified or removed its sibling must be in snapshot)

        if (input.isRemoved) {
            // file was removed in kotlin dir, remove from java as well
            fileInJavaDir.delete()
            kotlinClassesInJavaOutputDir.remove(fileInJavaDir)
        }
        else {
            // copy modified or added file from kotlin to java
            copy(fileInKotlinDir, fileInJavaDir)
        }
    }

    private fun copy(fileInKotlinDir: File, fileInJavaDir: File) {
        fileInJavaDir.parentFile.mkdirs()
        fileInKotlinDir.copyTo(fileInJavaDir, overwrite = true)
        kotlinClassesInJavaOutputDir.add(fileInJavaDir)
    }

    private val File.siblingInJavaDir: File
            get() = File(javaOutputDir, this.relativeTo(kotlinOutputDir).path)

    companion object {
        private val SNAPSHOT_FILE_NAME = "kotlin-files-in-java-snapshot.txt"

        private fun readSnapshot(snapshotFile: File): MutableSet<File> {
            val files = HashSet<File>()

            if (snapshotFile.exists()) {
                val lines = snapshotFile.readLines()
                lines.map { File(it) }.filterTo(files) { it.exists() }
            }

            return files
        }

        private fun saveSnapshot(snapshotFile: File, files: Iterable<File>) {
            if (!snapshotFile.exists()) {
                snapshotFile.parentFile.mkdirs()
                snapshotFile.createNewFile()
            }
            else {
                snapshotFile.delete()
            }

            snapshotFile.bufferedWriter().use { writer ->
                for (it in files) {
                    writer.write(it.canonicalPath)
                    writer.newLine()
                }
            }
        }
    }
}