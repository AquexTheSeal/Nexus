package com.mememan.nexus.internal;

import com.mememan.nexus.block.standard.BlockPropertyWrapper;
import com.mememan.nexus.item.standard.ItemPropertyWrapper;
import com.mememan.nexus.tag.TagWrapper;
import it.unimi.dsi.fastutil.ints.IntIntMutablePair;
import it.unimi.dsi.fastutil.objects.ObjectObjectMutablePair;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.registry.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Singleton {@code class} responsible for registering Vanilla compatibility features for blocks/items/tags on Fabric.
 */
public final class FabricVanillaCompat {

    /**
     * Internal method responsible for the registration of hardcoded Vanilla compatibility features from Block/Item/Tag
     * Property Wrappers. Additionally, handles registration of blocks and items to their respective
     * {@linkplain CreativeModeTab CreativeModeTabs}.
     */
    public static void registerVanillaCompat() {
        // Blocks
        BlockPropertyWrapper.getMappedBpws().forEach((parentBlockSup, curBpw) -> {
            IntIntMutablePair flammabilityPair = curBpw.getFlammabilityMappingFunc() == null ? null : curBpw.getFlammabilityMappingFunc().apply(parentBlockSup);
            Supplier<Block> strippedBlockVariant = curBpw.getBlockStrippingMappingFunc() == null ? null : curBpw.getBlockStrippingMappingFunc().apply(parentBlockSup);
            ObjectObjectMutablePair<Predicate<UseOnContext>, Consumer<UseOnContext>> parentBlockTillingBehaviourPair = curBpw.getBlockTillingMappingFunc() == null ? null : curBpw.getBlockTillingMappingFunc().apply(parentBlockSup);
            BlockState flattenedBlockVariant = curBpw.getBlockFlatteningMappingFunc() == null ? null : curBpw.getBlockFlatteningMappingFunc().apply(parentBlockSup);
            Supplier<Block> oxidizedBlockVariant = curBpw.getBlockOxidizationMappingFunc() == null ? null : curBpw.getBlockOxidizationMappingFunc().apply(parentBlockSup);
            Supplier<Block> waxedBlockVariant = curBpw.getBlockWaxingMappingFunc() == null ? null : curBpw.getBlockWaxingMappingFunc().apply(parentBlockSup);
            Float blockCompostChance = curBpw.getBlockCompostingMappingFunc() == null ? null : Math.abs(curBpw.getBlockCompostingMappingFunc().apply(parentBlockSup));
            Integer blockFuelCookTime = curBpw.getBlockFuelMappingFunc() == null ? null : Math.abs(curBpw.getBlockFuelMappingFunc().apply(parentBlockSup));

            if (flammabilityPair != null) FlammableBlockRegistry.getDefaultInstance().add(parentBlockSup.get(), Math.abs(flammabilityPair.leftInt()), Math.abs(flammabilityPair.rightInt()));
            if (strippedBlockVariant != null && strippedBlockVariant.get() != null) StrippableBlockRegistry.register(parentBlockSup.get(), strippedBlockVariant.get());
            if (parentBlockTillingBehaviourPair != null && parentBlockTillingBehaviourPair.left() != null && parentBlockTillingBehaviourPair.right() != null) TillableBlockRegistry.register(parentBlockSup.get(), parentBlockTillingBehaviourPair.left(), parentBlockTillingBehaviourPair.right());
            if (flattenedBlockVariant != null) FlattenableBlockRegistry.register(parentBlockSup.get(), flattenedBlockVariant);
            if (oxidizedBlockVariant != null && oxidizedBlockVariant.get() != null) OxidizableBlocksRegistry.registerOxidizableBlockPair(parentBlockSup.get(), oxidizedBlockVariant.get());
            if (waxedBlockVariant != null && waxedBlockVariant.get() != null) OxidizableBlocksRegistry.registerWaxableBlockPair(parentBlockSup.get(), waxedBlockVariant.get());
            if (blockCompostChance != null && blockCompostChance != 0) CompostingChanceRegistry.INSTANCE.add(parentBlockSup.get(), blockCompostChance);
            if (blockFuelCookTime != null && blockFuelCookTime != 0) FuelRegistry.INSTANCE.add(parentBlockSup.get(), blockFuelCookTime);
        });

        // Items
        ItemPropertyWrapper.getMappedIpws().forEach((parentItemSup, curIpw) -> {
            Float itemCompostChance = curIpw.getCompostingMappingFunc() == null ? null : Math.abs(curIpw.getCompostingMappingFunc().apply(parentItemSup));
            Integer itemFuelCookTime = curIpw.getItemFuelMappingFunc() == null ? null : Math.abs(curIpw.getItemFuelMappingFunc().apply(parentItemSup));

            if (itemCompostChance != null && itemCompostChance != 0) CompostingChanceRegistry.INSTANCE.add(parentItemSup.get(), itemCompostChance);
            if (itemFuelCookTime != null && itemFuelCookTime != 0) FuelRegistry.INSTANCE.add(parentItemSup.get(), itemFuelCookTime);
        });

        // Tags
        TagWrapper.getCachedTWEntries().forEach(curTw -> {
            int tagFuelCookTime = Math.abs(curTw.getCookTime());
            IntIntMutablePair tagFlammabilitySettings = curTw.getFlammabilitySettings();
            TagKey<?> curTagKey = curTw.getParentTag().get();

            if (tagFuelCookTime != 0 && curTagKey.isFor(Registries.ITEM)) FuelRegistry.INSTANCE.add((TagKey<Item>) curTagKey, tagFuelCookTime);
            if (tagFlammabilitySettings != null && curTagKey.isFor(Registries.BLOCK)) FlammableBlockRegistry.getDefaultInstance().add((TagKey<Block>) curTagKey, Math.abs(tagFlammabilitySettings.leftInt()), Math.abs(tagFlammabilitySettings.rightInt()));
        });

        // Creative Mode Tabs
        BuiltInRegistries.CREATIVE_MODE_TAB.entrySet().forEach(tabEntry -> {
            ResourceKey<CreativeModeTab> targetTabKey = tabEntry.getKey();
            CreativeModeTab targetTab = tabEntry.getValue();

            // Block CMTs
            BlockPropertyWrapper.getMappedBpws().entrySet().stream()
                    .filter(curBpwEntry -> curBpwEntry.getValue().getParentCreativeModeTabs().stream().map(Supplier::get).anyMatch(targetTab::equals) && !targetTab.getDisplayItems().contains(curBpwEntry.getKey().get().asItem().getDefaultInstance()))
                    .forEach(curBpwEntry -> ItemGroupEvents.modifyEntriesEvent(targetTabKey).register(tabEntries -> tabEntries.accept(curBpwEntry.getKey().get().asItem().getDefaultInstance())));

            // Item CMTs
            ItemPropertyWrapper.getMappedIpws().entrySet().stream()
                    .filter(curIpwEntry -> curIpwEntry.getValue().getParentCreativeModeTabs().stream().map(Supplier::get).anyMatch(targetTab::equals) && !targetTab.getDisplayItems().contains(curIpwEntry.getKey().get().getDefaultInstance()))
                    .forEach(curIpwEntry -> ItemGroupEvents.modifyEntriesEvent(targetTabKey).register(tabEntries -> tabEntries.accept(curIpwEntry.getKey().get().getDefaultInstance())));
        });
    }
}
