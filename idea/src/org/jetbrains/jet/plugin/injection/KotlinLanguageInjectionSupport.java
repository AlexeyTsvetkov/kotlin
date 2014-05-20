/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.injection;

import com.intellij.psi.PsiLanguageInjectionHost;
import org.intellij.plugins.intelliLang.inject.AbstractLanguageInjectionSupport;
import org.jetbrains.annotations.NotNull;


public class KotlinLanguageInjectionSupport extends AbstractLanguageInjectionSupport {
    private static final String SUPPORT_ID = "Kotlin";

    @NotNull
    @Override
    public String getId() {
        return SUPPORT_ID;
    }

    @NotNull
    @Override
    public Class[] getPatternClasses() {
        return new Class[]{ KotlinPatterns.class };
    }

    @Override
    public boolean useDefaultInjector(PsiLanguageInjectionHost host) {
        return false;
    }
}
