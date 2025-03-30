package com.mememan.nexus.datagen;

import com.mememan.nexus.NexusConstants;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import org.jetbrains.annotations.Nullable;

/**
 * We don't use Fabric datagen (although like Neo/Forge, it is configured to output generated resources inside :common)
 * yet. In order to see actual used datagen, refer to Nexus' Neo/Forge datagen classes.
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
