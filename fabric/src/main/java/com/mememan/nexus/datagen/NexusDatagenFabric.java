package com.mememan.nexus.datagen;

import com.mememan.nexus.NexusConstants;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import org.jetbrains.annotations.Nullable;

/**
 * Central internal implementation for datagen on Fabric. This is where
 * {@link com.mememan.nexus.internal.services.FabricDataGenerator} delegates its work.
 */
public class NexusDatagenFabric implements DataGeneratorEntrypoint {

    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {

    }

    @Override
    public @Nullable String getEffectiveModId() {
        return NexusConstants.MOD_ID;
    }
}
