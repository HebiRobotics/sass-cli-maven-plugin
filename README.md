# sass-exec-maven-plugin

This Maven plugin is a thin wrapper around the Dart based [SASS](https://sass-lang.com/) commandline tool for generating CSS files.

It works by downloading an appropriate native executable, and by calling it with the desired arguments. Please refer to the [SASS CLI documentation](https://sass-lang.com/documentation/cli/dart-sass) for available commands.

# Maven Example

```xml
<build>
    <plugins>
        <plugin>
            <groupId>us.hebi.sass</groupId>
            <artifactId>sass-exec-maven-plugin</artifactId>
            <version>1.0-SNAPSHOT</version>
            <configuration>
                <args> <!-- Any argument that should be forwarded to the sass executable -->
                    <arg>${project.basedir}/src/scss/input.scss:${project.basedir}/target/classes/output.css</arg>
                    <arg>${project.basedir}/src/scss/input2.scss:${project.basedir}/target/classes/output2.css</arg>
                </args>
            </configuration>
            <executions>
                <execution>
                    <id>run-sass</id>
                    <phase>generate-resources</phase>
                    <goals>
                        <goal>run</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

# Enabling Watch Mode

The `sass` CLI tool includes a `--watch` mode that continuously triggers a re-compilation whenever a used file changes. You can add the argument manually, or automatically using the `sass:watch` goal.

```shell
mvn sass:watch
```

# Custom Versions

Given that `sass` will likely update more often than this plugin, you can specify a custom version using the `sass.version` property. You can find available versions on the [Github Releases](https://github.com/sass/dart-sass/releases/) page.

```xml
<properties>
    <sass.version>SOME NEWER VERSION</sass.version>
</properties>
```