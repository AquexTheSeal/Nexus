package com.mememan.nexus;

import com.mememan.nexus.block.standard.BlockPropertyWrapper;
import com.mememan.nexus.internal.ForgeVanillaCompat;
import com.mememan.nexus.internal.event.common.NexusForgeCommonMiscEvents;
import com.mememan.nexus.item.standard.ItemPropertyWrapper;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.function.Supplier;

/**
 * Main Forge entrypoint/mod {@code class} for Nexus API. Handles common mod events and initializes Nexus for Forge.
 */
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
    public static void onBuildCreativeModeTabContentsEvent(BuildCreativeModeTabContentsEvent event) {
        CreativeModeTab curTab = event.getTab();

        // Blocks
        BlockPropertyWrapper.getMappedBpws().entrySet().stream()
                .filter(curBpwEntry -> curBpwEntry.getValue().getParentCreativeModeTabs().stream().map(Supplier::get).anyMatch(curTab::equals) && !event.getEntries().contains(curBpwEntry.getKey().get().asItem().getDefaultInstance()))
                .forEach(curBpwEntry -> event.accept(curBpwEntry.getKey().get()));

        // Items
        ItemPropertyWrapper.getMappedIpws().entrySet().stream()
                .filter(curIpwEntry -> curIpwEntry.getValue().getParentCreativeModeTabs().stream().map(Supplier::get).anyMatch(curTab::equals) && !event.getEntries().contains(curIpwEntry.getKey().get().getDefaultInstance()))
                .forEach(curIpwEntry -> event.accept(curIpwEntry.getKey().get()));
    }
}