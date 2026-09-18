package tn.cafe.pos.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * jpackage strips {@code bin/java} from the runtime image it generates, so the
 * backend launcher must not assume {@code <java.home>/bin/java} exists.
 */
class StandalonePosLauncherTest {

    @TempDir
    Path temp;

    @Test
    void prefersBundledJavaHomeBinary() throws Exception {
        Path bin = Files.createDirectories(temp.resolve("runtime/bin"));
        Path java = Files.createFile(bin.resolve("java"));
        java.toFile().setExecutable(true);
        assertEquals(java, StandalonePosLauncher.resolveJavaBinary(temp.resolve("runtime"), null));
    }

    @Test
    void fallsBackToJavaHomeEnvWhenBundledStripped() throws Exception {
        Path envBin = Files.createDirectories(temp.resolve("jdk/bin"));
        Path java = Files.createFile(envBin.resolve("java"));
        java.toFile().setExecutable(true);
        Path strippedRuntime = Files.createDirectories(temp.resolve("stripped"));
        assertEquals(java, StandalonePosLauncher.resolveJavaBinary(
                strippedRuntime, temp.resolve("jdk").toString()));
    }

    @Test
    void fallsBackToPathWhenNoJavaHomeHasBinary() throws Exception {
        Path strippedRuntime = Files.createDirectories(temp.resolve("stripped"));
        assertEquals(Path.of("java"), StandalonePosLauncher.resolveJavaBinary(
                strippedRuntime, temp.resolve("missing-jdk").toString()));
    }

    @Test
    void currentJvmResolvesToSomething() throws Exception {
        Path resolved = StandalonePosLauncher.resolveJavaBinary();
        assertTrue(resolved.toString().endsWith("java")
                || resolved.toString().endsWith("java.exe"));
    }
}
