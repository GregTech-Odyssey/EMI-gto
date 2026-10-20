# EMI
EMI is a featureful and accessible item and recipe viewer for Minecraft.

![EMI Interface](https://user-images.githubusercontent.com/14813658/224562247-1588064e-39ef-475a-9108-d7a357af6939.png)

![Recipe Tree](https://user-images.githubusercontent.com/14813658/224562258-1a5ee67a-fd7f-489f-9eed-ae67c184ddac.png)

## Build and run

This branch is a Forge-only mod for Minecraft 1.20.1. It uses Java 21, ForgeGradle 6, and Mojang official mappings. The bytecode target remains Java 17 for the Mixin version bundled with Forge 1.20.1.

All Java sources and resources are in `src/main/java` and `src/main/resources`.

```sh
./gradlew build
./gradlew runClient
./gradlew runServer
```

On Windows, use `.\gradlew.bat` in place of `./gradlew`. Set `JAVA_HOME` to a Java 21 JDK.
Build output is in `build/libs/`: the reobfuscated Forge mod, its API jar, and its sources jar.
JEI is optional at runtime; install the matching Forge JEI jar in `run/mods` to test JEI integration.

## Developers

Add the repository and Forge dependency to your `build.gradle`:

```gradle
repositories {
    maven { url = "https://maven.terraformersmc.com/" }
}

dependencies {
    compileOnly fg.deobf("dev.emi:emi-forge:${emi_version}:api")
    runtimeOnly fg.deobf("dev.emi:emi-forge:${emi_version}")
}
```

`emi_version` is the Maven version, for example `1.1.24-crazer.1+1.20.1` for a release or `1.1.24-crazer.1-SNAPSHOT+1.20.1` for a development build. Publish this branch to your Maven repository before consuming its custom version.

Enable Mixin refmap remapping in your ForgeGradle client run:

```gradle
minecraft {
    runs {
        client {
            property 'mixin.env.remapRefMap', 'true'
            property 'mixin.env.refMapRemappingFile', "${projectDir}/build/createSrgToMcp/output.srg"
        }
    }
}
```
