/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.messages;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public interface MessageRenderer {

    String PROPERTY_KEY = "org.jetbrains.kotlin.cliMessageRenderer";

    MessageRenderer WITHOUT_PATHS = new PlainTextMessageRenderer() {
        @Nullable
        @Override
        protected String getPath(@NotNull CompilerMessageLocation location) {
            return null;
        }

        @Override
        public String getName() {
            return "Pathless";
        }
    };

    MessageRenderer PLAIN_FULL_PATHS = new PlainTextMessageRenderer() {
        @NotNull
        @Override
        protected String getPath(@NotNull CompilerMessageLocation location) {
            return location.getPath();
        }

        @Override
        public String getName() {
            return "FullPath";
        }
    };

    MessageRenderer PLAIN_RELATIVE_PATHS = new PlainTextMessageRenderer() {
        private final File cwd = new File(".").getAbsoluteFile();

        @NotNull
        @Override
        protected String getPath(@NotNull CompilerMessageLocation location) {
            return FileUtilKt.descendantRelativeTo(new File(location.getPath()), cwd).getPath();
        }

        @Override
        public String getName() {
            return "RelativePath";
        }
    };

    MessageRenderer GRADLE_STYLE = new GradleStyleMessageRenderer();

    String renderPreamble();

    String render(@NotNull CompilerMessageSeverity severity, @NotNull String message, @Nullable CompilerMessageLocation location);

    String renderUsage(@NotNull String usage);

    String renderConclusion();

    String getName();
}
