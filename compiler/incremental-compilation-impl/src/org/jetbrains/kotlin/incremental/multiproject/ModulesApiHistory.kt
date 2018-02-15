/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.multiproject

import org.jetbrains.kotlin.daemon.common.GradleModule
import org.jetbrains.kotlin.daemon.common.GradleModulesInfo
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipFile

interface ModulesApiHistory {
    fun historyFileForArtifact(file: File): File?
}

object EmptyModulesApiHistory : ModulesApiHistory {
    override fun historyFileForArtifact(file: File): File? = null
}

class GradleModulesApiHistory(private val modulesInfo: GradleModulesInfo) : ModulesApiHistory {
    private val projectRootPath = Paths.get(modulesInfo.projectRoot.absolutePath)
    private val dirToHistoryFileCache = HashMap<File, File?>()

    override fun historyFileForArtifact(file: File): File? {
        when (file.extension.toLowerCase()) {
            "jar" -> {
                val jarPath = Paths.get(file.absolutePath)
                val modules = getPossibleModuleNamesFromJar(jarPath)
                    .flatMapTo(HashSet<GradleModule>()) { modulesInfo.nameToModules[it] ?: emptySet() }
                for (module in modules) {
                    if (Paths.get(module.buildDir.absolutePath).isParentOf(jarPath)) {
                        return module.buildHistoryFile
                    }
                }
            }
            else -> {
                return getBuildHistoryForDir(file.parentFile)
            }
        }

        return null
    }

    private fun getBuildHistoryForDir(file: File): File? =
        dirToHistoryFileCache.getOrPut(file) {
            val module = modulesInfo.dirToModule[file]
            val parent = file.parentFile

            when {
                module != null ->
                    module.buildHistoryFile
                parent != null && projectRootPath.isParentOf(parent) ->
                    getBuildHistoryForDir(parent)
                else ->
                    null
            }
        }

    private fun getPossibleModuleNamesFromJar(path: Path): Collection<String> {
        // do not try to traverse jar not from project
        if (!projectRootPath.isParentOf(path)) return emptyList()

        val result = HashSet<String>()

        try {
            ZipFile(path.toFile()).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val name = entry.name
                    if (name.endsWith(".kotlin_module", ignoreCase = true)) {
                        result.add(File(name).nameWithoutExtension)
                    }
                }
            }
        } catch (t: Throwable) {
            return emptyList()
        }

        return result
    }

    private fun Path.isParentOf(path: Path) = path.startsWith(this)
    private fun Path.isParentOf(file: File) = this.isParentOf(Paths.get(file.absolutePath))
}