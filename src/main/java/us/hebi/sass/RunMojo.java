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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.archiver.AbstractUnArchiver;
import org.codehaus.plexus.archiver.tar.TarGZipUnArchiver;
import org.codehaus.plexus.archiver.tar.TarUnArchiver.UntarCompressionMethod;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A mojo that downloads an appropriate binary release for sass and calls the executable
 */
@Mojo(name = "run")
@Execute(phase = LifecyclePhase.GENERATE_RESOURCES)
public class RunMojo extends AbstractMojo {

    /**
     * Template for the download URL. Settable in case the
     * download location changes or to support mirrors
     */
    @Parameter(defaultValue = "https://github.com/sass/dart-sass/releases/download/${sassVersion}/dart-sass-${sassVersion}-${os}-${arch}.${extension}")
    String downloadUrlTemplate;

    /**
     * Location where the downloads and executables will be located.
     * Defaults to ${user.home}/.hebi/sass
     */
    @Parameter
    Path downloadDirectory;

    /**
     * The path to the "sass" executable within the zip file.
     * Settable in case the format changes.
     */
    @Parameter(defaultValue = "dart-sass")
    String nestedDirectory;

    /**
     * Dart-SCSS version. See https://github.com/sass/dart-sass/releases/
     */
    @Parameter(defaultValue = "1.53.0", property = "sass.version")
    String sassVersion;

    /**
     * Arguments that are passed to the commandline tool.
     * Watch can be added using the watch mojo.
     */
    @Parameter(required = true)
    List<String> args;

    public void execute() throws MojoExecutionException {
        if (downloadDirectory == null) {
            downloadDirectory = Paths.get(System.getProperty("user.home"), ".hebi", "sass");
        }
        downloadDirectory = downloadDirectory.toAbsolutePath();

        Path homeDir = downloadDirectory.resolve(replaceVars("dart-sass-${sassVersion}-${os}-${arch}"));
        Path tmpArchive = homeDir.resolve(replaceVars("archive.${extension}"));
        Path executable = homeDir.resolve(nestedDirectory).resolve(getExecutableName());

        if (!Files.exists(executable)) {

            // Download archive
            String downloadUrl = replaceVars(downloadUrlTemplate);
            getLog().info("Downloading sass from " + downloadUrl);
            try (InputStream downloadFile = new URL(downloadUrl).openStream()) {
                Files.createDirectories(homeDir);
                Files.copy(downloadFile, tmpArchive, StandardCopyOption.REPLACE_EXISTING);
            } catch (MalformedURLException e) {
                throw new MojoExecutionException("Invalid url", e);
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to download dart sass archive", e);
            }

            // Extract
            getLog().info("Extracting sass");
            AbstractUnArchiver unArchiver = getUnarchiver();
            unArchiver.setSourceFile(tmpArchive.toFile());
            unArchiver.setOverwrite(true);
            unArchiver.setDestDirectory(homeDir.toFile());
            unArchiver.extract();

            // Cleanup
            try {
                Files.delete(tmpArchive);
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to delete temporary archive", e);
            }

        }

        // Sanity check
        if (!Files.isExecutable(executable)) {
            throw new MojoExecutionException("Target file is not executable: " + executable);
        }

        getLog().info("Executing sass");
        getLog().debug("sass path: " + executable);
        List<String> processArgs = new ArrayList<>();
        processArgs.add(String.valueOf(executable));
        processArgs.addAll(args);

        try {
            int returnCode = new ProcessBuilder(processArgs)
                    .inheritIO()
                    .start()
                    .waitFor();
            if (returnCode != 0) {
                throw new MojoExecutionException("Dart sass executable returned error code " + returnCode);
            }
        } catch (IOException | InterruptedException e) {
            throw new MojoExecutionException("Failed to execute dart sass", e);
        }

    }

    private String replaceVars(String input) {
        return input.replaceAll("\\$\\{sassVersion\\}", sassVersion)
                .replaceAll("\\$\\{os\\}", getOs())
                .replaceAll("\\$\\{arch\\}", getArch())
                .replaceAll("\\$\\{extension\\}", getExtension());
    }

    private AbstractUnArchiver getUnarchiver() throws MojoExecutionException {
        switch (getExtension()) {
            case "zip":
                return new ZipUnArchiver();
            case "tar.gz":
                TarGZipUnArchiver untar = new TarGZipUnArchiver();
                untar.setCompression(UntarCompressionMethod.GZIP);
                return untar;
            default:
                throw new MojoExecutionException("Unsupported extension: " + getExtension());
        }
    }


    private String getOs() {
        if (isWindows()) return "windows";
        if (OS_NAME.startsWith("linux")) return "linux";
        if (OS_NAME.contains("mac")) return "macos";
        throw new AssertionError("Unsupported OS: " + System.getProperty("os.name"));
    }

    private String getArch() {
        // https://stackoverflow.com/a/36926327/3574093
        switch (OS_ARCH) {
            case "aarch64":
                return "arm64";
            case "amd64":
            case "ia64":
            case "x86_64":
                return "x64";
            case "x86":
            case "i386":
            case "i486":
            case "i586":
            case "i686":
                return "ia32";
            default:
                throw new AssertionError("Unsupported arch: " + System.getProperty("os.arch"));
        }
    }

    private String getExtension() {
        return isWindows() ? "zip" : "tar.gz";
    }

    private String getExecutableName() {
        return isWindows() ? "sass.bat" : "sass";
    }

    private static boolean isWindows() {
        return OS_NAME.startsWith("win");
    }

    private static final String OS_NAME = System.getProperty("os.name").toLowerCase(Locale.US);
    private static final String OS_ARCH = System.getProperty("os.arch").toLowerCase(Locale.US);

}
