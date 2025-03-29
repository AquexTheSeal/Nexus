package com.mememan.nexus.loader;

import java.util.List;

/**
 * Data-holding {@code record} representing mod metadata, commonly defined in each loader's respective MTD files
 * ({@code fabric.mod.json}, {@code mods.toml}, etc.).
 *
 * @param modId The mod's ID, following standard <code>[a-z0-9_.-]</code> convention.
 * @param modName The mod's display name.
 * @param modVersion The mod's version. This is typically a string-ified version of {@code ArtifactVersion},
 *                   since its owning library isn't included with VanillaGradle.
 * @param modLicense The mod's license (only the license's title, NOT the full license!).
 * @param modDescription The mod's description. Usually a multi-line string.
 * @param modAuthors The mod's authors. May be empty if none are defined.
 * @param modSide The mod's target environment side.
 */
public record ModMetadata(String modId, String modName, String modVersion, String modLicense, String modDescription, List<String> modAuthors, ModSide modSide) {
}
