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
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetCallExpression;
import org.jetbrains.jet.lang.psi.ValueArgument;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.k2js.translate.callTranslator.CallTranslator;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.intrinsic.functions.patterns.DescriptorPredicate;
import org.jetbrains.k2js.translate.intrinsic.functions.patterns.PatternBuilder;
import org.jetbrains.k2js.translate.utils.BindingUtils;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public final class CallExpressionTranslator extends AbstractCallExpressionTranslator {

    @NotNull
    private final static DescriptorPredicate JSCODE_PATTERN = PatternBuilder.pattern("js", "jsCode");

    @NotNull
    public static JsNode translate(
            @NotNull JetCallExpression expression,
            @Nullable JsExpression receiver,
            @NotNull TranslationContext context
    ) {
        if (matchesJsCode(expression, context)) {
            return (new CallExpressionTranslator(expression, receiver, context)).translateNativeJs();
        }
        if (InlinedCallExpressionTranslator.shouldBeInlined(expression, context)) {
            return InlinedCallExpressionTranslator.translate(expression, receiver, context);
        }
        return (new CallExpressionTranslator(expression, receiver, context)).translate();
    }

    private static boolean matchesJsCode(
            @NotNull JetCallExpression expression,
            @NotNull TranslationContext context
    ) {
        ResolvedCall<? extends FunctionDescriptor> resolvedCall =
                BindingUtils.getResolvedCallForCallExpression(context.bindingContext(), expression);

        FunctionDescriptor descriptor = resolvedCall.getResultingDescriptor();

        if (!JSCODE_PATTERN.apply(descriptor)) {
            return false;
        }

        List<? extends ValueArgument> arguments = expression.getValueArguments();
        if (arguments.isEmpty()) {
            return false;
        }

        return true;
    }

    private JsNode translateNativeJs() {
        List<? extends ValueArgument> arguments = expression.getValueArguments();
        String jsCode = arguments.get(0).getArgumentExpression().getText();
        jsCode = removeQuotes(jsCode);

        List<JsStatement> statements = new ArrayList<JsStatement>();
        try {
            SourceInfoImpl info = new SourceInfoImpl(null, 0, 0, 0, 0);
            JsScope scope = context().scope();
            StringReader reader = new StringReader(jsCode);
            statements.addAll(JsParser.parse(info, scope, reader, /* insideFunction= */ true));
        } catch (JsParserException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return simplifyStatements(statements);
    }

    private JsNode simplifyStatements(List<JsStatement> statements) {
        int size = statements.size();
        if (size > 1) {
            return new JsBlock(statements);
        }

        JsStatement resultStatement;
        if (size == 0) {
            resultStatement = program().getEmptyStatement();
        } else {
            resultStatement = statements.get(0);
        }

        if (resultStatement instanceof JsExpressionStatement) {
            return ((JsExpressionStatement) resultStatement).getExpression();
        }

        return resultStatement;
    }

    private static String removeQuotes(String jsCode) {
        String singleQuote = "\"";
        String tripleQuote = "\"\"\"";

        if (startsEndsWith(jsCode, tripleQuote)) {
            jsCode = jsCode.substring(3, jsCode.length() - 3);
        } else if (startsEndsWith(jsCode, singleQuote)) {
            jsCode = jsCode.substring(1, jsCode.length() - 1);
        } else {
            throw new RuntimeException("String argument must have either 1 or 3 quotes");
        }

        return jsCode;
    }

    private static boolean startsEndsWith(String string, String pattern) {
        return string.startsWith(pattern) && string.endsWith(pattern);
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
