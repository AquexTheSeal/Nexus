package com.mememan.nexus.util;

import com.mememan.nexus.datagen.standard.StandardDatapackRegistryProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Utility {@code class} containing helper/shortcut methods to help with formatting
 * {@linkplain ResourceLocation ResourceLocations}.
 */
public final class ResourceLocationUtil {

    private ResourceLocationUtil() {
        throw new IllegalAccessError("Attempted to construct instance of utility class! (ResourceLocationUtil)");
    }

    /**
     * Formats a given {@link ResourceLocation} as a path rather than {@code namespace:some/path} if it's not Minecraft.
     * Only really used in {@link StandardDatapackRegistryProvider} to dump registry entries based on their parent
     * registries.
     *
     * @param targetLoc The {@link ResourceLocation} to format.
     *
     * @return The formatted {@link ResourceLocation}.
     */
    public static String formatPath(ResourceLocation targetLoc) {
        return targetLoc.getNamespace().equals("minecraft") ? targetLoc.getPath() : targetLoc.getNamespace() + "/" + targetLoc.getPath();
    }
}
