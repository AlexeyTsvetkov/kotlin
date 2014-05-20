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

import com.intellij.patterns.InitialPatternCondition;
import com.intellij.patterns.PsiJavaElementPattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetCallElement;

public class KotlinElementPattern<T extends JetCallElement,
                                 Self extends KotlinElementPattern<T, Self>> extends PsiJavaElementPattern<T, Self> {
    public KotlinElementPattern(Class<T> tClass) {
        super(tClass);
    }

    public KotlinElementPattern(@NotNull InitialPatternCondition<T> tCondition) {
        super(tCondition);
    }



    public static class Capture<T extends JetCallElement> extends KotlinElementPattern<T, Capture<T>> {

        public Capture(Class<T> aClass) {
            super(aClass);
        }

        public Capture(@NotNull InitialPatternCondition<T> condition) {
            super(condition);
        }
    }
}
