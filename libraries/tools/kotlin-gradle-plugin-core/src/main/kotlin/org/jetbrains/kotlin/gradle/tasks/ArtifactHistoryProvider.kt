package org.jetbrains.kotlin.gradle.tasks

import org.jetbrains.kotlin.incremental.LookupSymbol
import java.io.File

interface ArtifactHistoryProvider {
    operator fun get(artifact: File): Iterable<ArtifactHistoryEntry>?
}

interface ArtifactHistoryEntry {
    val timestamp: Long
    val changedFqNames: Iterable<LookupSymbol>
}