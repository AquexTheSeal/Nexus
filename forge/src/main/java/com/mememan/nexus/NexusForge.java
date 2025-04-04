package com.mememan.nexus;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(NexusConstants.MOD_ID)
@Mod.EventBusSubscriber
public class NexusForge {
    
    public NexusForge() {
        Nexus.initialize();
    }

    @SubscribeEvent
    public static void onFMLCommonSetupEvent(final FMLCommonSetupEvent event) {
        event.enqueueWork(() ->
                Nexus.initializeDeferred()
        );
    }
}