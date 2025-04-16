package com.mememan.nexus;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.concurrent.CompletableFuture;

@Mod(NexusConstants.MOD_ID)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class NexusForge {

    public NexusForge() {
        Nexus.initialize();
    }

    @SubscribeEvent
    public static void onFMLCommonSetupEvent(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            Nexus.initializeDeferred();
        });
    }

    @SubscribeEvent
    public static void onGatherDataEvent(final GatherDataEvent event) {
        DataGenerator primaryGen = event.getGenerator();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        boolean onClient = event.includeClient();
        boolean onServer = event.includeServer();

        // Resource Pack

        // Data Pack
    }
}