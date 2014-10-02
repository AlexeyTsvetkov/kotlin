/*
 * Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.jetbrains.k2js.inline;

import com.google.dart.compiler.backend.js.ast.*;
import org.jetbrains.annotations.NotNull;

import static org.jetbrains.k2js.inline.InlinePackage.aliasArgumentsIfNeeded;

import java.util.*;

class FunctionInlineMutator {
    private JsBlock body;
    private final JsFunction invokedFunction;
    private final List<JsExpression> arguments;
    private final List<JsParameter> parameters;
    private final RenamingContext<JsBlock> renamingContext;
    private final InsertionPoint<JsStatement> insertionPoint;

    public static InlineableResult getInlineableCallReplacement(
            @NotNull JsInvocation call,
            @NotNull InliningContext inliningContext
    ) {
        FunctionInlineMutator mutator = new FunctionInlineMutator(call, inliningContext);
        mutator.process();

        JsStatement inlineableBody = mutator.body;
        return new InlineableResult(inlineableBody, call);
    }

    private FunctionInlineMutator(@NotNull JsInvocation call, @NotNull InliningContext inliningContext) {
        FunctionContext functionContext = inliningContext.getFunctionContext();
        invokedFunction = functionContext.getFunctionDefinition(call);
        body = invokedFunction.getBody().deepCopy();
        arguments = call.getArguments();
        parameters = invokedFunction.getParameters();
        renamingContext = inliningContext.getRenamingContext();
        insertionPoint = inliningContext.getStatementContext().getInsertionPoint();
    }

    private void process() {
        aliasArgumentsIfNeeded(renamingContext, arguments, parameters);
        renameLocals(renamingContext, invokedFunction);
        applyRenaming();
    }

    private void applyRenaming() {
        RenamingResult<JsBlock> renamingResult = renamingContext.applyRename(body);
        body = renamingResult.getRenamed();
        Collection<JsVars> declarations = renamingResult.getDeclarations();
        insertionPoint.insertAllBefore(declarations);
    }
}
