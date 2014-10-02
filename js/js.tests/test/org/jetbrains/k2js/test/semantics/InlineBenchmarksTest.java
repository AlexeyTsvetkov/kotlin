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

package org.jetbrains.k2js.test.semantics;

import com.google.dart.compiler.backend.js.ast.JsNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.k2js.config.Config;
import org.jetbrains.k2js.facade.MainCallParameters;
import org.jetbrains.k2js.test.SingleFileTranslationTest;
import org.jetbrains.k2js.test.utils.InlineTestUtils;
import org.jetbrains.k2js.test.utils.JsTestUtils;
import org.jetbrains.k2js.test.utils.TranslationUtils;

import java.io.File;
import java.util.List;

public class InlineBenchmarksTest extends SingleFileTranslationTest {
    private JsNode lastJsNode;

    public InlineBenchmarksTest() {
        super("benchmarks/");
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        lastJsNode = null;
    }

    public void testFunktionalBenchmark() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testReduce() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testReduceNoInline() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testMap() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testMapNoInline() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testFilter() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testFilterNoInline() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testForeachLight() throws Exception {
        checkFooBoxIsOk();
    }

    public void testForeachLightNoInline() throws Exception {
        checkFooBoxIsOk();
    }

    private void checkFooBoxIsOkWithInlineDirectives() throws Exception {
        checkFooBoxIsOk();
        processInlineDirectives();
    }

    private void processInlineDirectives() throws Exception {
        String fileName = getInputFilePath(getTestName(true) + ".kt");
        String fileText = JsTestUtils.readFile(fileName);

        InlineTestUtils.processDirectives(lastJsNode, fileText);
    }

    @Override
    protected void translateFiles(
            @NotNull List<JetFile> jetFiles,
            @NotNull File outputFile,
            @NotNull MainCallParameters mainCallParameters,
            @NotNull Config config
    ) throws Exception {
        lastJsNode = TranslationUtils.translateFilesAndGetAst(mainCallParameters, jetFiles, outputFile, null, null, config);
    }
}