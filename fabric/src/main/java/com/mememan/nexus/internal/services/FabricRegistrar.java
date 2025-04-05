package com.mememan.nexus.internal.services;

import com.mememan.nexus.platform.services.Registrar;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Fabric-specific implementation of {@link Registrar}.
 */
public class FabricRegistrar implements Registrar {

    @Override
    public void setupRegistrar() {

    }

    @Override
    public <V, T extends V> Supplier<T> registerObject(ResourceLocation objId, Supplier<T> objSup, Registry<V> targetRegistry) {
        return null;
    }

    @Override
    public <T> Supplier<ResourceKey<T>> registerDatapackObject(ResourceLocation objId, Function<BootstapContext<T>, Supplier<T>> objSupMappingFunc, ResourceKey<Registry<T>> targetDatapackRegistry) {
        return null;
    }
}
