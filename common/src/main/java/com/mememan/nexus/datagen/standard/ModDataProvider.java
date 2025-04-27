package com.mememan.nexus.datagen.standard;

import net.minecraft.data.DataProvider;
import org.jetbrains.annotations.NotNull;

/**
 * Loader-agnostic mod-oriented version of {@link DataProvider} designed for mod-specific data generation tasks in Nexus API.
 * This {@code interface} ensures that data generation tasks are contextually segregated and tied to a specific mod ID.
 */
public interface ModDataProvider extends DataProvider {

    /**
     * Gets the mod ID of the mod this provider belongs to. Always formatted in locale-lowercase.
     *
     * @return The mod ID of the mod this provider belongs to, e.g. {@code "nexus"}.
     */
    @NotNull
    String getModId();
}
