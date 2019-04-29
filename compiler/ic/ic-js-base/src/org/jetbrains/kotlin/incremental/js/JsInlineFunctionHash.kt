package org.jetbrains.kotlin.incremental.js

import java.io.Serializable

class JsInlineFunctionHash(val sourceFilePath: String, val fqName: String, val inlineFunctionMd5Hash: Long) : Serializable
