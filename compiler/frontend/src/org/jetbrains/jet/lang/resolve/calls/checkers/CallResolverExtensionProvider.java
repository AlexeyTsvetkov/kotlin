/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.calls.checkers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;

import java.lang.ref.WeakReference;
import java.util.*;

public class CallResolverExtensionProvider {

    private final static CompositeChecker DEFAULT =
            new CompositeChecker(Arrays.asList(
                    new NeedSyntheticChecker(),
                    new ReifiedTypeParameterSubstitutionChecker(),
                    new CapturingInClosureChecker(),
                    new InlineCheckerWrapper()
            ));

    @NotNull
    public CallChecker createExtension(@Nullable DeclarationDescriptor descriptor, boolean isAnnotationContext) {
        return DEFAULT;
    }
}
