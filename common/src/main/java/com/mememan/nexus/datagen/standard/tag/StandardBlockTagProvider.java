package com.mememan.nexus.datagen.standard.tag;

import com.mememan.nexus.NexusConstants;
import com.mememan.nexus.block.standard.BlockPropertyWrapper;
import com.mememan.nexus.datagen.DuplicateDataPolicy;
import com.mememan.nexus.datagen.NexusProviderTypes;
import com.mememan.nexus.datagen.ProviderType;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class StandardBlockTagProvider extends StandardTagProvider<Block> {
    protected final Object2ObjectOpenHashMap<Supplier<Block>, BlockPropertyWrapper> mappedModBPWs;

    public StandardBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> regLookup, String modId, boolean validateAllEntries, DuplicateDataPolicy dupeStrat) {
        super(output, Registries.BLOCK, regLookup, block -> block.builtInRegistryHolder().key(), modId, validateAllEntries, dupeStrat);

        this.mappedModBPWs = BlockPropertyWrapper.getMappedBpws().entrySet()
                .stream()
                .filter(curEntry -> BuiltInRegistries.BLOCK.getKey(curEntry.getKey().get()).getNamespace().equals(modId))
                .filter(curEntry -> !curEntry.getValue().excludeFromNativeDatagen())
                .peek(curEntry -> {
                    Block curBlock = curEntry.getKey().get();
                    BlockPropertyWrapper curBPW = curEntry.getValue();

                    if ((validateAllEntries() || curBPW.getProviderTypeRequisites().getOrDefault(getProviderType(), false)) && curBPW.getParentTags().isEmpty() && curBPW.getParentBlockTags().isEmpty()) {
                        throw new IllegalStateException(String.format("No tags found for block: %s (Required by mod of ID: %s), either because validateAllEntries is set to true for this provider or the block itself requires validation through BlockPropertyWrapper#getProviderTypeRequisites().", curBlock.getDescriptionId(), modId));
                    }
                })
                .collect(Object2ObjectOpenHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), Object2ObjectOpenHashMap::putAll);
    }

    @Override
    protected void addObjectTags(HolderLookup.Provider provider) {
        if (!mappedModBPWs.isEmpty()) {
            mappedModBPWs.forEach((blockSupEntry, curBpw) -> {
                List<TagKey<?>> parentTags = curBpw.getParentTags().stream().map(Supplier::get).collect(Collectors.toCollection(ObjectArrayList::new));
                List<TagKey<Block>> parentBlockTags = curBpw.getParentBlockTags().stream().map(Supplier::get).collect(Collectors.toCollection(ObjectArrayList::new));

                if (!parentTags.isEmpty()) {
                    parentTags.forEach(curBlockTag -> {
                        if (curBlockTag != null && curBlockTag.isFor(Registries.BLOCK)) {
                            TagKey<Block> curInferredBlockTag = (TagKey<Block>) curBlockTag;

                            if (validateDupeObjectTag(blockSupEntry.get(), curInferredBlockTag)) {
                                NexusConstants.LOGGER.debug("[Tagging Block]: {} -> {} (For mod of ID: {})", blockSupEntry.get().getDescriptionId(), curBlockTag, modId);

                                tag(curInferredBlockTag).add(blockSupEntry.get());
                            }
                        }
                    });
                }

                if (!parentBlockTags.isEmpty()) {
                    parentBlockTags.forEach(curBlockTag -> {
                        if (curBlockTag != null) {
                            if (validateDupeObjectTag(blockSupEntry.get(), curBlockTag)) {
                                NexusConstants.LOGGER.debug("[Tagging Block]: {} -> {} (For mod of ID: {})", blockSupEntry.get().getDescriptionId(), curBlockTag, modId);

                                tag(curBlockTag).add(blockSupEntry.get());
                            }
                        }
                    });
                }
            });
        }
    }

    @Override
    public @NotNull ProviderType getProviderType() {
        return NexusProviderTypes.BLOCK_TAGS_PROVIDER;
    }

    @Override
    protected @Nullable String getObjectName(Block targetObj) {
        return targetObj.getDescriptionId();
    }
}