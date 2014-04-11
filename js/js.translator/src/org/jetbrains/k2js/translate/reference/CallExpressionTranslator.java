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

package org.jetbrains.k2js.translate.reference;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.common.SourceInfoImpl;
import com.google.gwt.dev.js.JsParser;
import com.google.gwt.dev.js.JsParserException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetCallExpression;
import org.jetbrains.jet.lang.psi.ValueArgument;
import org.jetbrains.k2js.translate.callTranslator.CallTranslator;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.utils.BindingUtils;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public final class CallExpressionTranslator extends AbstractCallExpressionTranslator {

    @NotNull
    public static JsNode translate(
            @NotNull JetCallExpression expression,
            @Nullable JsExpression receiver,
            @NotNull TranslationContext context
    ) {
        if (matchesJsCode(expression, context)) {
            return CallExpressionTranslator.translateNativeJs(expression, context);
        }
        if (InlinedCallExpressionTranslator.shouldBeInlined(expression, context)) {
            return InlinedCallExpressionTranslator.translate(expression, receiver, context);
        }
        return (new CallExpressionTranslator(expression, receiver, context)).translate();
    }

    private static boolean matchesJsCode(JetCallExpression expression, TranslationContext context) {
        if(!expression.getText().startsWith("jsCode")) {
            return false;
        }

        List<? extends ValueArgument> arguments = expression.getValueArguments();
        if (arguments.isEmpty()) {
            return false;
        }

        return true;
    }

    private static JsNode translateNativeJs(@NotNull JetCallExpression expression, @NotNull TranslationContext context) {
        List<? extends ValueArgument> arguments = expression.getValueArguments();
        String jsCode = arguments.get(0).getArgumentExpression().getText();
        jsCode = jsCode.replaceAll("^\"*","").replaceAll("\"*$","");

        List<JsStatement> statements = new ArrayList<JsStatement>();
        try {
            SourceInfoImpl info = new SourceInfoImpl(null, 0, 0, 0, 0);
            JsScope scope = context.scope();
            StringReader reader = new StringReader(jsCode);
            statements.addAll(JsParser.parse(info, scope, reader, /* insideFunction= */ true));
        } catch (JsParserException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new JsBlock(statements);
    }

    private CallExpressionTranslator(
            @NotNull JetCallExpression expression,
            @Nullable JsExpression receiver,
            @NotNull TranslationContext context
    ) {
        super(expression, receiver, context);
    }

    @NotNull
    private JsExpression translate() {
        return CallTranslator.instance$.translate(context(), resolvedCall, receiver);
    }
}
