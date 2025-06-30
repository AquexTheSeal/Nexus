package com.mememan.nexus.datagen.standard;

import com.mememan.nexus.NexusConstants;
import com.mememan.nexus.block.standard.BlockPropertyWrapper;
import com.mememan.nexus.datagen.DuplicateDataPolicy;
import com.mememan.nexus.datagen.NexusProviderTypes;
import com.mememan.nexus.datagen.ProviderType;
import com.mememan.nexus.tag.TagWrapper;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class StandardBlockTagProvider extends IntrinsicHolderTagsProvider<Block> implements ModDataProvider {
    protected final String modId;
    protected final boolean validateAllEntries;
    protected final DuplicateDataPolicy dupeStrat;
    protected final Object2ObjectOpenHashMap<Supplier<Block>, BlockPropertyWrapper> mappedModBPWs;
    protected final ObjectArrayList<TagWrapper<? extends Block, TagKey<Block>>> mappedModBlockTags;
    protected final Object2ObjectOpenHashMap<Block, ObjectArrayList<TagKey<Block>>> trackedTaggedBlocks = new Object2ObjectOpenHashMap<>();

    public StandardBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> regLookup, String modId, boolean validateAllEntries, DuplicateDataPolicy dupeStrat) {
        super(output, Registries.BLOCK, regLookup, block -> block.builtInRegistryHolder().key());

        this.modId = modId;
        this.validateAllEntries = validateAllEntries;
        this.dupeStrat = dupeStrat;

        this.mappedModBPWs = BlockPropertyWrapper.getMappedBpws().entrySet()
                .stream()
                .filter(curEntry -> BuiltInRegistries.BLOCK.getKey(curEntry.getKey().get()).getNamespace().equals(modId))
                .filter(curEntry -> !curEntry.getValue().excludeFromNativeDatagen())
                .collect(Object2ObjectOpenHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), Object2ObjectOpenHashMap::putAll);
        this.mappedModBlockTags =TagWrapper.getCachedTWEntries().stream()
                .filter(curTW -> curTW.getParentTag().get().isFor(Registries.BLOCK) && curTW.getParentTag().get().location().getNamespace().equals(modId))
                .map(curTW -> (TagWrapper<? extends Block, TagKey<Block>>) curTW)

                .collect(Collectors.toCollection(ObjectArrayList::new));
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // Blocks
        if (!mappedModBPWs.isEmpty()) {
            mappedModBPWs.forEach((blockSupEntry, curBpw) -> {
                List<TagKey<?>> parentTags = curBpw.getParentTags().stream().map(Supplier::get).collect(Collectors.toCollection(ObjectArrayList::new));
                List<TagKey<Block>> parentBlockTags = curBpw.getParentBlockTags().stream().map(Supplier::get).collect(Collectors.toCollection(ObjectArrayList::new));

                if (!parentTags.isEmpty()) {
                    parentTags.forEach(curBlockTag -> {
                        if (curBlockTag != null && curBlockTag.isFor(Registries.BLOCK)) {
                            NexusConstants.LOGGER.debug("[Tagging Block]: {} -> {} (For mod of ID: {})", blockSupEntry.get().getDescriptionId(), curBlockTag, modId);

                            tag((TagKey<Block>) curBlockTag).add(blockSupEntry.get());
                        }
                    });
                }

                if (!parentBlockTags.isEmpty()) {
                    parentBlockTags.forEach(curBlockTag -> {
                        if (curBlockTag != null) {
                            NexusConstants.LOGGER.debug("[Tagging Block]: {} -> {} (For mod of ID: {})", blockSupEntry.get().getDescriptionId(), curBlockTag, modId);

                            tag(curBlockTag).add(blockSupEntry.get());
                        }
                    });
                }
            });
        }

        // Block Tags
        if (!mappedModBlockTags.isEmpty()) {
            mappedModBlockTags.forEach(twEntry -> {
                TagKey<Block> parentTagKey = twEntry.getParentTag().get();

                twEntry.getPredefinedTagEntries().forEach(tagEntry -> {
                    Block blockTagEntry = tagEntry.get();

                    if (blockTagEntry != null) {
                        NexusConstants.LOGGER.debug("[Tagging Block]: {} -> {} (For mod of ID: {})", blockTagEntry.getDescriptionId(), parentTagKey, modId);

                        tag(parentTagKey).add(blockTagEntry);
                    }
                });

                twEntry.getStoredTags().forEach(tagKeyEntry -> {
                    TagKey<Block> storedTagKeyEntry = tagKeyEntry.get();

                    if (storedTagKeyEntry != null) {
                        NexusConstants.LOGGER.debug("[Tagging Block Tag]: {} -> {} (For mod of ID: {})", storedTagKeyEntry, parentTagKey, modId);

                        tag(storedTagKeyEntry);
                        tag(parentTagKey).addTag(storedTagKeyEntry);
                    }
                });

                twEntry.getParentTags().forEach(parentTagKeyEntry -> {
                    TagKey<Block> storedParentTagKeyEntry = parentTagKeyEntry.get();

                    if (storedParentTagKeyEntry != null) {
                        NexusConstants.LOGGER.debug("[Tagging Block Tag]: {} -> {} (For mod of ID: {})", parentTagKey, storedParentTagKeyEntry, modId);

                        tag(parentTagKey);
                        tag(storedParentTagKeyEntry).addTag(parentTagKey);
                    }
                });
            });
        }
    }

    @Override
    public @NotNull String getName() {
        return String.format("Block Tags [%s]",getModId());
    }

    @Override
    public @NotNull String getModId() {
        return modId;
    }

    @Override
    public boolean validateAllEntries() {
        return validateAllEntries;
    }

    @Override
    public @NotNull ProviderType getProviderType() {
        return NexusProviderTypes.BLOCK_TAGS_PROVIDER;
    }

    @Override
    public @NotNull DuplicateDataPolicy getDuplicateDataPolicy() {
        return dupeStrat;
    }

    protected boolean validateDupeBlockTag(Block targetBlock, TagKey<Block> targetTagKey) {
        ObjectArrayList<TagKey<Block>> targetBlockTags = trackedTaggedBlocks.computeIfAbsent(targetBlock, oK -> new ObjectArrayList<>());
        ResourceLocation tagLoc = targetTagKey.location();

        if (targetBlockTags.stream().map(TagKey::location).anyMatch(tagLoc::equals)) {
            switch (getDuplicateDataPolicy()) {
                case CRASH -> throw new IllegalStateException(String.format("Attempted to tag duplicate block %s with key %s from mod of ID %s, specified DuplicateDataPolicy is CRASH.", targetBlock.getDescriptionId(), tagLoc, getModId()));
                case EXCLUDE_WARN -> {
                    NexusConstants.LOGGER.warn("Attempted to tag duplicate block {} with key {} from mod of ID {}, specified DuplicateDataPolicy is EXCLUDE_WARN. Skipping...", targetBlock.getDescriptionId(), tagLoc, getModId());
                    return false;
                }
                case EXCLUDE_SILENT -> {
                    return false;
                }
                case OVERRIDE_WARN -> {
                    NexusConstants.LOGGER.warn("Overriding duplicate block tag {} for block {} from mod of ID {}, specified DuplicateDataPolicy is OVERRIDE_WARN.", tagLoc, targetBlock.getDescriptionId(), getModId());
                    return true;
                }
                case OVERRIDE_SILENT -> {
                    return true;
                }
            }
        } else targetBlockTags.add(targetTagKey);

        return true;
    }
}