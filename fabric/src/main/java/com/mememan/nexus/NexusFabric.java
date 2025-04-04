package com.mememan.nexus;

import com.mememan.nexus.internal.FabricServerHooks;
import net.fabricmc.api.ModInitializer;

public class NexusFabric implements ModInitializer {
    
    @Override
    public void onInitialize() {
        Nexus.initialize();

        FabricServerHooks.handleServerLifecycleHooks();
    }
}
