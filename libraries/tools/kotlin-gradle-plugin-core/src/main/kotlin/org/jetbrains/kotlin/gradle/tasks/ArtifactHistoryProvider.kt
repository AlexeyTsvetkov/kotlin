package org.jetbrains.kotlin.gradle.tasks

import java.io.File

interface ArtifactHistoryProvider {
    operator fun get(artifact: File): Iterable<ArtifactHistoryEntry>?
}

interface ArtifactHistoryEntry {
    val timestamp: Long
    val changedFqNames: Iterable<String>
}