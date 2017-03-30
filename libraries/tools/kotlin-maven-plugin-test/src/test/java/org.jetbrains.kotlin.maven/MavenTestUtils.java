package org.jetbrains.kotlin.maven;

import org.jetbrains.annotations.NotNull;

import java.io.*;

class MavenTestUtils {
    @NotNull
    static String readText(@NotNull File file) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

        StringBuilder sb = new StringBuilder();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }
        }
        finally {
            reader.close();
        }

        return sb.toString();
    }

    static void writeText(@NotNull File file, @NotNull String text) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));

        try {
            writer.write(text);
        }
        finally {
            writer.close();
        }
    }

    static void replaceFirstInFile(@NotNull File file, @NotNull String regex, @NotNull String replacement) throws IOException {
        String text = readText(file);
        String processedText = text.replaceFirst(regex, replacement);
        writeText(file, processedText);
    }
}
