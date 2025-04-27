package com.mememan.nexus.internal.datagen;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

/**
 * Central internal implementation for datagen on Fabric. This is where
 * {@link com.mememan.nexus.internal.services.FabricDataGenerator} delegates its work.
 */
public class NexusDataGenFabric implements DataGeneratorEntrypoint {

    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        
    }
}
