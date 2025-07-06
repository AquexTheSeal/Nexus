package com.mememan.nexus.datagen.standard.tag;

import com.mememan.nexus.NexusConstants;
import com.mememan.nexus.datagen.DuplicateDataPolicy;
import com.mememan.nexus.datagen.NexusProviderTypes;
import com.mememan.nexus.datagen.ProviderType;
import com.mememan.nexus.item.standard.ItemPropertyWrapper;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class StandardItemTagProvider extends StandardTagProvider<Item> {
    protected final Object2ObjectOpenHashMap<Supplier<Item>, ItemPropertyWrapper> mappedModIPWs;

    public StandardItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> regLookup, String modId, boolean validateAllEntries, DuplicateDataPolicy dupeStrat) {
        super(output, Registries.ITEM, regLookup, item -> item.builtInRegistryHolder().key(), modId, validateAllEntries, dupeStrat);

        this.mappedModIPWs = ItemPropertyWrapper.getMappedIpws().entrySet()
                .stream()
                .filter(curEntry -> BuiltInRegistries.ITEM.getKey(curEntry.getKey().get()).getNamespace().equals(modId))
                .filter(curEntry -> !curEntry.getValue().excludeFromNativeDatagen())
                .peek(curEntry -> {
                    Item curItem = curEntry.getKey().get();
                    ItemPropertyWrapper curIPW = curEntry.getValue();

                    if ((validateAllEntries() || curIPW.getProviderTypeRequisites().getOrDefault(getProviderType(), false)) && curIPW.getParentTags().isEmpty()) {
                        throw new IllegalStateException(String.format("No tags found for item: %s (Required by mod of ID: %s), either because validateAllEntries is set to true for this provider or the item itself requires validation through ItemPropertyWrapper#getProviderTypeRequisites().", curItem.getDescriptionId(), modId));
                    }
                })
                .collect(Object2ObjectOpenHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), Object2ObjectOpenHashMap::putAll);
    }

    @Override
    protected void addObjectTags(HolderLookup.Provider provider) {
        if (!mappedModIPWs.isEmpty()) {
            mappedModIPWs.forEach((itemSupEntry, curIpw) -> {
                List<TagKey<Item>> parentTags = curIpw.getParentTags().stream().map(Supplier::get).collect(Collectors.toCollection(ObjectArrayList::new));

                if (!parentTags.isEmpty()) {
                    parentTags.forEach(curItemTag -> {
                        if (curItemTag != null) {
                            if (validateDupeObjectTag(itemSupEntry.get(), curItemTag)) {
                                NexusConstants.LOGGER.debug("[Tagging Item]: {} -> {} (For mod of ID: {})", itemSupEntry.get().getDescriptionId(), curItemTag, modId);

                                tag(curItemTag).add(itemSupEntry.get());
                            }
                        }
                    });
                }
            });
        }
    }

    @Override
    public @NotNull ProviderType getProviderType() {
        return NexusProviderTypes.ITEM_TAGS_PROVIDER;
    }

    @Override
    protected @Nullable String getObjectName(Item targetObj) {
        return targetObj.getDescriptionId();
    }
}
