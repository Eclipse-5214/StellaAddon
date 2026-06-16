# Stella Addon

A [Stella](https://github.com/Eclipse-5214/stella) addon — built on the same Fabric/Kotlin/Stonecutter stack, consuming Stella as a library dependency instead of duplicating its framework.

See [API.md](API.md) for a reference of the Stella APIs available to addons (modules, config DSL, commands, events, handlers, rendering), with short usage examples. Look there before reaching for KDoc.

## Structure

```
src/main/kotlin/co/stellarskys/stellaaddon/
  StellaAddon.kt        client entrypoint
  features/             @Module feature classes
  commands/             @Command Atlas command classes
```

## Setup

**Prerequisites:**

- JDK 25 (Temurin recommended — matches CI)
- Git

**Get the source:**

```
git clone https://github.com/Eclipse-5214/stella-addon.git
cd stella-addon
```

Stella itself isn't cloned alongside this repo — it's pulled as a versioned dependency from JitPack (`gradle/libs.versions.toml`, `stella = "<commit>"`). Bump that line to pick up newer Stella commits.

**Run a dev client:**

```
./gradlew runClient
```

Builds against whichever Minecraft version Stonecutter has active (`stonecutter active "..."` in `stonecutter.gradle.kts`, currently `26.1`) and launches Minecraft with the addon (and Stella) loaded, using DevAuth so you don't need a real session.

**IDE:**

Open the repo root in IntelliJ — Loom generates run configs automatically (`ideConfigGenerated(true)`). The [Stonecutter IntelliJ plugin](https://stonecutter.kikugie.dev/stonecutter/setup) makes switching/managing active MC versions easier if you ever add more.

## Building

```
./gradlew build
```

Targets the Minecraft version Stonecutter is currently active on (`stonecutter.gradle.kts`). Output jars land in `build/libs/<version>/`.

## Requirements

- Stella installed alongside this addon (declared in `fabric.mod.json` as a hard dependency)
- Fabric API + Fabric Language Kotlin
