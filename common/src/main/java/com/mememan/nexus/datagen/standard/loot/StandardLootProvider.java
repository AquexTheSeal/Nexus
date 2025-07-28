package com.mememan.nexus.datagen.standard.loot;

import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Set;

public class StandardLootProvider extends LootTableProvider {

    public StandardLootProvider(PackOutput targetOutput, Set<ResourceLocation> requiredTables, List<SubProviderEntry> subProviders) {
        super(targetOutput, requiredTables, subProviders);
    }
}
