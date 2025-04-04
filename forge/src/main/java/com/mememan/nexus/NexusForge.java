package com.mememan.nexus;

import com.mememan.nexus.platform.NexusServices;
import net.minecraftforge.fml.common.Mod;

@Mod(NexusConstants.MOD_ID)
public class NexusForge {
    
    public NexusForge() {
        Nexus.initialize();

        NexusServices.PLATFORM_MANAGER.getModData().forEach(d -> {
            NexusConstants.LOGGER.info("Mod {} has dependencies: {}", d.getModMetadata().modId(), d.getModMetadata().modDependencies());
            NexusConstants.LOGGER.info("Mod {} has paths: {}", d.getModMetadata().modId(), d.getAllFilePaths());
        });
    }
}