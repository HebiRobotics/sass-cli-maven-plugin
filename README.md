# sass-cli-maven-plugin

This Maven plugin is a thin wrapper around the [Sass](https://sass-lang.com/) commandline interface for generating CSS files. It downloads an appropriate native executable and executes it with the specified arguments. Please refer to [Sass CLI](https://sass-lang.com/documentation/cli/dart-sass) for documentation.

## Maven Example

```xml
<build>
    <plugins>
        <plugin>
            <groupId>us.hebi.sass</groupId>
            <artifactId>sass-cli-maven-plugin</artifactId>
            <version>1.0.2</version>
            <configuration>
                <sassVersion>1.57.1</sassVersion>
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

## Sass Version

Sass updates more often than this plugin, so it is recommended to manually specify the latest version. You can do this via the `sassVersion` argument or the `sass.version` property. Available versions can be found on [Github Releases](https://github.com/sass/dart-sass/releases/).

```xml
<properties>
    <sass.version>1.57.1</sass.version>
</properties>
```

<table>
  <tr>
    <td valign="middle">
      The latest release is
    </td>
    <td valign="middle">
      <a href="https://pub.dartlang.org/packages/sass"><img alt="Pub version" src="https://img.shields.io/pub/v/sass.svg"></a>
    </td>
  </tr>
</table>


