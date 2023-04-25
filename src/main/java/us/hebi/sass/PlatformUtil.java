/*-
 * #%L
 * sass-cli-maven-plugin Maven Mojo
 * %%
 * Copyright (C) 2022 HEBI Robotics
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package us.hebi.sass;

import org.codehaus.plexus.archiver.AbstractUnArchiver;
import org.codehaus.plexus.archiver.tar.TarGZipUnArchiver;
import org.codehaus.plexus.archiver.tar.TarUnArchiver.UntarCompressionMethod;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/**
 * @author Florian Enner
 * @since 30 Jul 2022
 */
public class PlatformUtil {

    public static String getDefaultArchiveExtension() {
        return isWindows() ? "zip" : "tar.gz";
    }

    public static String toScriptName(String name) {
        return isWindows() ? name + ".bat" : name;
    }

    public static void downloadAndExtractArchive(URL url, Path destinationDirectory) throws IOException {
        Path tmpArchive = createTemporaryArchive(url.toExternalForm());

        try {
            // Download
            try (InputStream downloadFile = url.openStream()) {
                Files.createDirectories(destinationDirectory);
                Files.copy(downloadFile, tmpArchive, StandardCopyOption.REPLACE_EXISTING);
            }

            // Extract
            extractArchive(tmpArchive, destinationDirectory);

        } finally {
            // Cleanup
            Files.delete(tmpArchive);
        }
    }

    private static Path createTemporaryArchive(String url) throws IOException {
        final String extension;
        if (url.endsWith(".zip")) {
            extension = ".zip";
        } else if (url.endsWith(".tar.gz")) {
            extension = ".tar.gz";
        } else {
            throw new UnsupportedOperationException("Unsupported archive extension on: " + url);
        }
        return Files.createTempFile("archive", extension);
    }

    public static void extractArchive(Path sourceArchive, Path destinationDirectory) {
        // Create appropriate unarchiver
        final AbstractUnArchiver unArchiver;
        String path = sourceArchive.toString().toLowerCase(Locale.ENGLISH);
        if (path.endsWith(".zip")) {
            unArchiver = new ZipUnArchiver();
        } else if (path.endsWith(".tar.gz")) {
            TarGZipUnArchiver untar = new TarGZipUnArchiver();
            untar.setCompression(UntarCompressionMethod.GZIP);
            unArchiver = untar;
        } else {
            throw new UnsupportedOperationException("Unsupported archive extension on: " + path);
        }

        // Extract to specified directory
        unArchiver.setSourceFile(sourceArchive.toFile());
        unArchiver.setOverwrite(true);
        unArchiver.setDestDirectory(destinationDirectory.toFile());
        unArchiver.extract();
    }

    public enum Architecture {
        x86_32, x86_64, arm_32, arm_64
    }

    public static Architecture getArch() {
        // https://stackoverflow.com/a/36926327/3574093
        switch (OS_ARCH) {
            case "aarch64":
                return Architecture.arm_64;
            case "arm":
                return Architecture.arm_32;
            case "amd64":
            case "ia64":
            case "x86_64":
                return Architecture.x86_64;
            case "x86":
            case "i386":
            case "i486":
            case "i586":
            case "i686":
                return Architecture.x86_32;
            default:
                throw new UnsupportedOperationException("Unsupported arch: " + System.getProperty("os.arch"));
        }
    }

    public enum OsFamily {
        Windows, Linux, macOS;
    }

    public static OsFamily getOsFamily() {
        if (isWindows()) return OsFamily.Windows;
        if (isLinux()) return OsFamily.Linux;
        if (isMac()) return OsFamily.macOS;
        throw new UnsupportedOperationException("Unsupported OS: " + System.getProperty("os.name"));
    }

    public static boolean isWindows() {
        return OS_NAME.startsWith("win");
    }

    public static boolean isMac() {
        return OS_NAME.contains("mac");
    }

    public static boolean isLinux() {
        return OS_NAME.startsWith("linux");
    }

    public static final String OS_NAME = System.getProperty("os.name").toLowerCase(Locale.US);
    public static final String OS_ARCH = System.getProperty("os.arch").toLowerCase(Locale.US);

}
