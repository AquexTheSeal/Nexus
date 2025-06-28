package com.mememan.nexus.mixins.forge;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.registries.DataPackRegistriesHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * Accessor {@code interface} primarily designed to access {@link DataPackRegistriesHooks#NETWORKABLE_REGISTRIES} without
 * weird reflection/asm shenanigans.
 */
@Mixin(DataPackRegistriesHooks.class)
public interface DataPackRegistriesHooksAccessor {

    @Accessor("NETWORKABLE_REGISTRIES")
    static Map<ResourceKey<? extends Registry<?>>, RegistrySynchronization.NetworkedRegistryData<?>> getNetworkableRegistries() {
        throw new AssertionError();
    }
}
