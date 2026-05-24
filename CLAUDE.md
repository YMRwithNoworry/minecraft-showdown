# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Minecraft Forge mod called "showdown" for Minecraft 1.20.1 (Forge 47.4.20). The mod provides server-side game control features including mob feud systems, team management, and various gameplay toggles. All user-facing strings are in Chinese.

## Build Commands

```bash
# Build the mod
./gradlew build

# Compile only (faster feedback)
./gradlew compileJava

# Run client for testing
./gradlew runClient

# Run server for testing
./gradlew runServer

# Run data generation
./gradlew runData

# Clean build
./gradlew clean build
```

## Architecture

**Package:** `alku.showdown` (mod_id: `showdown`)

**Core Classes:**

- `Showdown.java` - Main mod class with `@Mod` annotation. Registers event handlers for entity spawns, damage, targeting, and commands. Manages runtime feature toggles (`noItemDrops`, `noExpDrops`, `peacefulMode`, `godMode`).

- `ModCommand.java` - Registers the `/showdown` command tree with Brigadier. Provides subcommands: `hostile`, `cancel`, `nodrops`, `noexps`, `peaceful`, `godmode`, and `team` (with create/delete/add/remove/list/apply/clear operations). Commands require permission level 2.

- `ModFeudManager.java` - Manages mob feud state between mods. Scans `ForgeRegistries.ENTITY_TYPES` to find all entities belonging to a mod namespace. Maintains hostile relationship caches for performance.

- `ModFeudTargetGoal.java` - Custom AI goal extending `NearestAttackableTargetGoal`. Makes mobs target entities from hostile mods only when feud is active.

- `ModTeamManager.java` - Persists team configurations to `config/showdown-teams.json`. Teams group multiple mods together; feuds then occur between different teams.

- `Config.java` - Forge config spec (mostly template defaults). Real feature toggles are managed via commands at runtime.

**Data Flow:**
1. `/showdown hostile mod1 mod2` or `/showdown team apply` activates feuds
2. `ModFeudManager` scans entity registry for matching mod namespaces
3. New mobs get `ModFeudTargetGoal` added via `EntityJoinLevelEvent`
4. Existing mobs get goal added immediately when feud starts

## Key Technical Details

- Java 17 toolchain required
- Uses Forge event bus (not NeoForge)
- Team data persists across server restarts via JSON file
- Entity scanning is cached per mod ID for performance
- The `clearingTarget` flag in `Showdown.java` prevents infinite recursion when clearing mob targets in peaceful mode
