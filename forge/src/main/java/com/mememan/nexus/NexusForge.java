package com.mememan.nexus;

import net.minecraftforge.fml.common.Mod;

@Mod(NexusConstants.MOD_ID)
public class NexusForge {
    
    public NexusForge() {
        Nexus.initialize();
    }
}