/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.cli.js;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.SmartList;
import org.jetbrains.kotlin.cli.CliBaseTest;
import org.jetbrains.kotlin.js.test.rhino.RhinoFunctionResultChecker;
import org.jetbrains.kotlin.js.test.rhino.RhinoUtils;
import org.jetbrains.kotlin.test.MockLibraryUtil;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.List;

public class K2JsCliTest extends CliBaseTest {
    private final RhinoFunctionResultChecker CHECK_FOO_BOX_IS_OK = new RhinoFunctionResultChecker("out", "foo", "box", "OK");

    @Test
    public void simple2js() throws Exception {
        executeCompilerCompareOutputJS();

        Assert.assertTrue(new File(tmpdir.getTmpDir(), "out.js").isFile());
    }

    @Test
    public void outputIsDirectory() throws Exception {
        executeCompilerCompareOutputJS();

        Assert.assertFalse(new File(tmpdir.getTmpDir(), "out.js").exists());
    }

    @Test
    public void nonExistingSourcePath() throws Exception {
        executeCompilerCompareOutputJS();

        Assert.assertFalse(new File(tmpdir.getTmpDir(), "out.js").exists());
    }

    @Test
    public void emptySources() throws Exception {
        executeCompilerCompareOutputJS();

        Assert.assertFalse(new File(tmpdir.getTmpDir(), "out.js").exists());
    }

    @Test
    public void outputPrefixFileNotFound() throws Exception {
        executeCompilerCompareOutputJS();

        Assert.assertFalse(new File(tmpdir.getTmpDir(), "out.js").exists());
    }

    @Test
    public void outputPostfixFileNotFound() throws Exception {
        executeCompilerCompareOutputJS();

        Assert.assertFalse(new File(tmpdir.getTmpDir(), "out.js").exists());
    }

    @Test
    public void wrongAbiVersion() throws Exception {
        executeCompilerCompareOutputJS();

        Assert.assertFalse(new File(tmpdir.getTmpDir(), "out.js").exists());
    }

    @Test
    public void jsHelp() throws Exception {
        executeCompilerCompareOutputJS();
    }

    @Test
    public void withLib() throws Exception {
        executeCompilerCompareOutputJS();

        Assert.assertTrue(new File(tmpdir.getTmpDir(), "out.js").isFile());
    }

    @Test
    public void withFolderAsLib() throws Exception {
        executeCompilerCompareOutputJS();

        Assert.assertTrue(new File(tmpdir.getTmpDir(), "out.js").isFile());
    }

    @Test
    public void createMetadata() throws Exception {
        executeCompilerCompareOutputJS();

        Assert.assertTrue(new File(tmpdir.getTmpDir(), "jslib-example.meta.js").isFile());
        Assert.assertTrue(new File(tmpdir.getTmpDir(), "jslib-example.js").isFile());
    }

    @Test
    public void libraryDirNotFound() throws Exception {
        executeCompilerCompareOutputJS();

        Assert.assertFalse(new File(tmpdir.getTmpDir(), "out.js").isFile());
    }

    @Test
    public void notValidLibraryDir() throws Exception {
        executeCompilerCompareOutputJS();

        Assert.assertFalse(new File(tmpdir.getTmpDir(), "out.js").isFile());
    }

    @Test
    public void inlineFromJarLib() throws Exception {
        File utilsKt = new File(getJsTestDataDir(), "libWithInlineFun.kt");
        String jarName = "libWithInlineFun";
        File jar = MockLibraryUtil.compileJsLibraryToJar(utilsKt.getPath(), jarName, false);
        FileUtil.copy(jar, new File(tmpdir.getTmpDir(), jarName + ".jar"));

        executeCompilerCompareOutputJS();
        File outJs = new File(tmpdir.getTmpDir(), "out.js");
        Assert.assertTrue(outJs.isFile());

        List<String> jsFiles = new SmartList<String>(outJs.getPath());
        RhinoUtils.runRhinoTest(jsFiles, CHECK_FOO_BOX_IS_OK);
    }
}
