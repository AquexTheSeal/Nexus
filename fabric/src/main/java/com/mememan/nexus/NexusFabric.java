package com.mememan.nexus;

import com.mememan.nexus.platform.NexusServices;
import net.fabricmc.api.ModInitializer;

public class NexusFabric implements ModInitializer {
    
    @Override
    public void onInitialize() {
        Nexus.initialize();

        NexusServices.PLATFORM_MANAGER.getModData().forEach(d -> {
            NexusConstants.LOGGER.info("Mod {} has dependencies: {}", d.getModMetadata().modId(), d.getModMetadata().modDependencies());
            NexusConstants.LOGGER.info("Mod {} has paths: {}", d.getModMetadata().modId(), d.getAllFilePaths());
        });
    }
}
