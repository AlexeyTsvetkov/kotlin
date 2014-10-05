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

package org.jetbrains.k2js.translate.expression;

import com.google.dart.compiler.backend.js.ast.*;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.utils.JsAstUtils;
import org.jetbrains.k2js.translate.utils.TranslationUtils;

import java.util.List;

import static org.jetbrains.k2js.translate.utils.JsAstUtils.convertToBlock;

public final class TryTranslator extends AbstractTranslator {

    private static final String THROWABLE_CLASS_NAME = DescriptorUtils.getFqNameSafe(KotlinBuiltIns.getInstance().getThrowable()).asString();

    @NotNull
    public static JsTry translate(@NotNull JetTryExpression expression, @NotNull TranslationContext context) {
        return (new TryTranslator(expression, context)).translate();
    }

    @NotNull
    private final JetTryExpression expression;

    private TryTranslator(@NotNull JetTryExpression expression, @NotNull TranslationContext context) {
        super(context);
        this.expression = expression;
    }

    private JsTry translate() {
        return new JsTry(translateTryBlock(), translateCatches(), translateFinallyBlock());
    }

    @Nullable
    private JsBlock translateFinallyBlock() {
        JetFinallySection finallyBlock = expression.getFinallyBlock();
        if (finallyBlock == null) return null;

        return convertToBlock(Translation.translateAsStatementAndMergeInBlockIfNeeded(finallyBlock.getFinalExpression(), context()));
    }

    @NotNull
    private JsBlock translateTryBlock() {
        return convertToBlock(Translation.translateAsStatementAndMergeInBlockIfNeeded(expression.getTryBlock(), context()));
    }

    @NotNull
    private List<JsCatch> translateCatches() {
        List<JsCatch> result = new SmartList<JsCatch>();

        JsIf resultIf = null;
        JsIf currentIf = null;
        JsNameRef firstParameterRef = null;
        JsStatement lastElseStatement = null;
        JsCatch jsCatch = null;
        PatternTranslator patternTranslator = Translation.patternTranslator(context());

        for(JetCatchClause catchClause : expression.getCatchClauses()) {
            JetTypeReference typeReference = getTypeReference(catchClause);
            JsBlock catchBlock = translateCatchBody(catchClause);

            if (resultIf == null) { // first pass
                firstParameterRef = new JsNameRef(getParameterName(catchClause));
                jsCatch = new JsCatch(context().scope(), firstParameterRef.getIdent());
            }

            if (isThrowableType(typeReference)) {
                if (resultIf == null) { // first pass
                    jsCatch.setBody(catchBlock);
                    result.add(jsCatch);
                } else {
                    lastElseStatement = catchBlock;
                }
                break;
            }

            JsExpression jsIsCheck = patternTranslator.translateIsCheck(firstParameterRef, typeReference);
            JsIf nextIf = new JsIf(jsIsCheck, catchBlock);

            if (resultIf == null) { // first pass
                resultIf = nextIf;
            }
            else {
                JsName parameterName = getParameterName(catchClause);
                catchBlock.getStatements().add(0, JsAstUtils.newVar(parameterName, firstParameterRef));
                currentIf.setElseStatement(nextIf);
            }
            
            currentIf = nextIf;
        }

        if (resultIf == null) {
            return result;
        }

        if (lastElseStatement == null) {
            lastElseStatement = new JsThrow(firstParameterRef);
        }
        currentIf.setElseStatement(lastElseStatement);
        jsCatch.setBody(JsAstUtils.convertToBlock(resultIf));
        result.add(jsCatch);

        return result;
    }

    private boolean isThrowableType(@NotNull JetTypeReference typeReference) {
        JetType type = BindingContextUtils.getNotNull(context().bindingContext(), BindingContext.TYPE, typeReference);
        String typeName = TranslationUtils.getJetTypeFqName(type);
        return typeName.equals(THROWABLE_CLASS_NAME);
    }

    @NotNull
    private static JetParameter getParameter(@NotNull JetCatchClause catchClause) {
        JetParameter catchParameter = catchClause.getCatchParameter();
        assert catchParameter != null : "Valid catch must have a parameter.";

        return catchParameter;
    }

    @NotNull
    private JsName getParameterName(@NotNull JetCatchClause catchClause) {
        return context().getNameForElement(getParameter(catchClause));
    }

    @NotNull
    private static JetTypeReference getTypeReference(@NotNull JetCatchClause catchClause) {
        JetParameter catchParameter = getParameter(catchClause);
        JetTypeReference typeReference = catchParameter.getTypeReference();
        assert typeReference != null : "catch parameter type reference should not be null for " + catchParameter.getText();

        return typeReference;
    }

    @NotNull
    private JsBlock translateCatchBody(@NotNull JetCatchClause catchClause) {
        JetExpression catchBody = catchClause.getCatchBody();
        if (catchBody == null) {
            return convertToBlock(context().getEmptyStatement());
        }
        return convertToBlock(Translation.translateAsStatementAndMergeInBlockIfNeeded(catchBody, context()));
    }
}
