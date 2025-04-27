package com.mememan.nexus;

import com.mememan.nexus.internal.FabricServerHooks;
import com.mememan.nexus.internal.FabricVanillaCompat;
import net.fabricmc.api.ModInitializer;

public class NexusFabric implements ModInitializer {
    
    @Override
    public void onInitialize() {
        Nexus.initialize();

        FabricServerHooks.handleServerLifecycleHooks();
        FabricVanillaCompat.registerVanillaCompat();
    }
}
