# sass-cli-maven-plugin

This Maven plugin is a thin wrapper around the [Sass](https://sass-lang.com/) commandline interface for generating CSS files.

It works by downloading an appropriate native executable and calling it with the specified arguments. Please refer to [Sass CLI](https://sass-lang.com/documentation/cli/dart-sass) for documentation.

## Maven Example

```xml
<build>
    <plugins>
        <plugin>
            <groupId>us.hebi.sass</groupId>
            <artifactId>sass-cli-maven-plugin</artifactId>
            <version>1.0.1</version>
            <configuration>
                <args> <!-- Any argument that should be forwarded to the sass cli -->
                    <arg>${project.basedir}/src/scss/input.scss:${project.basedir}/target/classes/output.css</arg>
                    <arg>${project.basedir}/src/scss/input2.scss:${project.basedir}/target/classes/output2.css</arg>
                    <arg>--no-source-map</arg>
                </args>
            </configuration>
            <executions>
                <execution>
                    <id>sass-exec</id>
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

## Enabling Watch Mode

The `sass` CLI tool includes a `--watch` mode that continuously triggers a re-compilation whenever a used file changes. You can add the argument by running the `sass:watch` goal, or by specifying the `sass.watch` property.

```shell
# goal
mvn sass-cli:watch

# property
mvn package -Dsass.watch
```

## Custom Versions

Given that `sass` will likely update more often than this plugin, you can specify the version using the `sass.version` property. You can find available versions on the [Github Releases](https://github.com/sass/dart-sass/releases/) page.

```xml
<properties>
    <sass.version>THE DESIRED RELEASE</sass.version>
</properties>
```
