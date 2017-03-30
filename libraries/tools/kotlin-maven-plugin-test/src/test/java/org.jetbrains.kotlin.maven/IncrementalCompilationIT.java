package org.jetbrains.kotlin.maven;

import org.junit.Test;

import java.io.File;

public class IncrementalCompilationIT {
    @Test
    public void testSimpleCompile() throws Exception {
        MavenProject project = new MavenProject("kotlinSimple");
        project.exec("install")
               .succeeded()
               .compiledKotlin("src/A.kt", "src/useA.kt", "src/Dummy.kt");
    }

    @Test
    public void testNoChanges() throws Exception {
        MavenProject project = new MavenProject("kotlinSimple");
        project.exec("install");

        project.exec("install")
               .succeeded()
               .compiledKotlin();
    }

    @Test
    public void testCompileError() throws Exception {
        MavenProject project = new MavenProject("kotlinSimple");
        project.exec("install");

        File aKt = project.file("src/A.kt");
        String original = "class A";
        String replacement = "private class A";
        MavenTestUtils.replaceFirstInFile(aKt, original, replacement);

        project.exec("install")
               .failed();

        MavenTestUtils.replaceFirstInFile(aKt, replacement, original);
        project.exec("install")
               .succeeded()
               .compiledKotlin("src/A.kt", "src/useA.kt");

    }

    @Test
    public void testFunctionVisibilityChanged() throws Exception {
        MavenProject project = new MavenProject("kotlinSimple");
        project.exec("install");

        File aKt = project.file("src/A.kt");
        MavenTestUtils.replaceFirstInFile(aKt, "fun foo", "internal fun foo");

        project.exec("install")
               .succeeded()
               .compiledKotlin("src/A.kt", "src/useA.kt");
    }
}
