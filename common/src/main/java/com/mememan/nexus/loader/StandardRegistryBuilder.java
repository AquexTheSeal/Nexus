package com.mememan.nexus.loader;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Generalized implementation for loader-specific methods centered around creating new standard
 * {@linkplain Registry Registries}.
 */
public class StandardRegistryBuilder<T, R extends Registry<T>> {
    private final ResourceKey<R> registryKey;
    private boolean synced = false;
    private boolean persistent = false;
    @Nullable
    private ResourceLocation defaultRegistryEntryLocation;

    private StandardRegistryBuilder(ResourceKey<R> registryKey) {
        this.registryKey = registryKey;
    }

    public StandardRegistryBuilder<T, R> synced(boolean synced) {
        this.synced = synced;
        return this;
    }

    public StandardRegistryBuilder<T, R> persistent(boolean persistent) {
        this.persistent = persistent;
        return this;
    }

    public ResourceKey<R> getRegistryKey() {
        return registryKey;
    }

    public boolean isSynced() {
        return synced;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public R createSimpleRegistry() {
        return null; //TODO
    }
}
