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
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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
    @Parameter(defaultValue = "https://github.com/sass/dart-sass/releases/download/{sassVersion}/dart-sass-{sassVersion}-{os}-{arch}.{archiveExtension}")
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
    @Parameter(defaultValue = "1.62.0", property = "sass.version")
    String sassVersion;

    /**
     * Skips execution of this plugin
     */
    @Parameter(property = "sass.skip", defaultValue = "false")
    Boolean skip;

    /**
     * Adds the "--watch" argument to enable continuous watch mode
     */
    @Parameter(property = "sass.watch")
    Boolean watch;

    /**
     * Arguments that are passed to the commandline tool.
     * Watch can be added using the watch mojo.
     */
    @Parameter(required = true)
    List<String> args;

    public void execute() throws MojoExecutionException {
        if (skip != null && skip) {
            getLog().info("Skipping sass execution");
            return;
        }

        Path homeDir = downloadDirectory != null ? downloadDirectory.toAbsolutePath() :
                Paths.get(System.getProperty("user.home"), ".hebi", "sass");
        Path extractDir = homeDir.resolve(replaceTemplate("dart-sass-{sassVersion}-{os}-{arch}"));
        Path executable = extractDir.resolve(nestedDirectory).resolve(PlatformUtil.toScriptName("sass"));

        // Download required files
        if (!Files.exists(executable)) {
            try {
                String url = replaceTemplate(downloadUrlTemplate);
                getLog().info("Downloading sass archive from " + url);
                PlatformUtil.downloadAndExtractArchive(new URL(url), extractDir);
            } catch (MalformedURLException e) {
                throw new MojoExecutionException("Invalid url", e);
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to download dart sass archive", e);
            }
        }

        // Sanity check
        if (!Files.isExecutable(executable)) {
            throw new MojoExecutionException("Target file is not executable: " + executable);
        }

        getLog().debug("sass path: " + executable);
        List<String> commandArgs = new ArrayList<>();
        commandArgs.add(String.valueOf(executable));
        commandArgs.addAll(args);
        if (watch != null && watch) {
            commandArgs.add("--watch");
        }

        try {
            getLog().info("Executing sass");
            Commandline commandline = new Commandline();
            commandline.addArguments(commandArgs.toArray(new String[0]));
            int returnCode = CommandLineUtils.executeCommandLine(commandline,
                    System.out::println,
                    System.err::println);
            if (returnCode != 0) {
                throw new MojoExecutionException("Dart sass executable returned error code " + returnCode);
            }
        } catch (CommandLineException e) {
            throw new MojoExecutionException("Failed to execute dart sass", e);
        }

    }

    String replaceTemplate(String input) {
        return input.replaceAll("\\{sassVersion}", sassVersion)
                .replaceAll("\\{os}", getOsString())
                .replaceAll("\\{arch}", getArchString())
                .replaceAll("\\{archiveExtension}", PlatformUtil.getDefaultArchiveExtension());
    }

    String getOsString() {
        switch (PlatformUtil.getOsFamily()) {
            case Windows:
                return "windows";
            case Linux:
                return "linux";
            case macOS:
                return "macos";
        }
        throw new AssertionError("Unsupported OS: " + PlatformUtil.OS_NAME);
    }

    String getArchString() {
        switch (PlatformUtil.getArch()) {
            case x86_32:
                return "ia32";
            case x86_64:
                return "x64";
            case arm_64:
                return "arm64";
        }
        throw new AssertionError("Unsupported arch: " + PlatformUtil.OS_ARCH);
    }

}
