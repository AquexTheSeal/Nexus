package com.mememan.nexus;

import com.mememan.nexus.internal.ForgeVanillaCompat;
import com.mememan.nexus.internal.event.common.NexusForgeCommonMiscEvents;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.concurrent.CompletableFuture;

@Mod(NexusConstants.MOD_ID)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class NexusForge {

    public NexusForge() {
        Nexus.initialize();

        IEventBus forgeBus = MinecraftForge.EVENT_BUS;

        forgeBus.register(NexusForgeCommonMiscEvents.class); // Can't use auto-annotation registration since it'd initialize the class before everything else
    }

    @SubscribeEvent
    public static void onFMLCommonSetupEvent(final FMLCommonSetupEvent event) {
        Nexus.initializeDeferred();

        event.enqueueWork(ForgeVanillaCompat::registerVanillaIntegration);
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