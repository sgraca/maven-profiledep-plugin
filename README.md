# Maven Profile Dependencies Plugin

This plugin allows to inject dependencies and EAR modules from Maven profiles.

## Usage

To integration in your POM:

```xml
<project>
  ...
  <build>
    ...
    <plugins>
      <plugin>
	<groupId>cchantep</groupId>
	<artifactId>maven-profiledep-plugin</artifactId>
	<version>1.0</version>
	
	<executions>
	  <execution>
	    <id>inject-dependencies</id>
	    <goals>
	      <goal>inject</goal>
	    </goals>

	    <configuration>
	      <prefix>my.deps.</prefix>
	    </configuration>
	  </execution>
	  <execution>
	    <id>inject-ear-modules</id>
	    <goals>
	      <goal>inject-ear-module</goal>
	    </goals>

	    <configuration>
	      <prefix>my.mods.</prefix>
	    </configuration>
	  </execution>
	</executions>
      </plugin>

      ...
    </plugins>
  </build>
</project>
```

For previous sample POM, profile properties prefixed by `my.deps.` and by `my.mods` are used with the 2 goals provided by this Maven plugin.

So in Maven profile (`.m2/settings.xml`), you can defined properties in following way:

```xml
<settings>
  ...
  <profiles>
    <profile>
      <id>my-activated-profile</id>
      <properties>
        <my.deps.xxx>groupId:artifactId:version:packaging:scope<!-- ... --></my.deps.xxx>
        <my.mods.xxx>groupId:artifactId:moduleType:entryName groupId:artifactId:moduleType:entryName:/</my.mods.xxx>
      </properties>
    </profile>
  </profiles>
  ...
</settings>
```

These two kind of profile properties define space-separated values, plain dependencies for `my.deps.` prefixed one (here `my.deps.xxx`), EAR module specifications for for `my.mods.` one (here `my.mods.xxx`).

For EAR module injection, `moduleType` part of module specification is the type of included module, like `ejb`, `java` or `web`. The `entryName` is used to customized the module filename. When module is `web` one, specification is ended by context path (here `/`).