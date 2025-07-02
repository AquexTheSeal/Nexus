package com.mememan.nexus.datagen.standard.tag;

import com.mememan.nexus.NexusConstants;
import com.mememan.nexus.datagen.DuplicateDataPolicy;
import com.mememan.nexus.datagen.NexusProviderTypes;
import com.mememan.nexus.datagen.ProviderType;
import com.mememan.nexus.entity.standard.EntityTypePropertyWrapper;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class StandardEntityTypeTagProvider extends StandardTagProvider<EntityType<?>> {
    protected final Object2ObjectOpenHashMap<Supplier<? extends EntityType<?>>, EntityTypePropertyWrapper<?>> mappedModETPWs;

    public StandardEntityTypeTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> objectLookupProvider, String modId, boolean validateAllEntries, DuplicateDataPolicy dupeStrat) {
        super(output, Registries.ENTITY_TYPE, objectLookupProvider, entityType -> entityType.builtInRegistryHolder().key(), modId, validateAllEntries, dupeStrat);

        this.mappedModETPWs = EntityTypePropertyWrapper.getMappedEtpws().entrySet()
                .stream()
                .filter(curEntry -> BuiltInRegistries.ENTITY_TYPE.getKey(curEntry.getKey().get()).getNamespace().equals(modId))
                .filter(curEntry -> !curEntry.getValue().excludeFromNativeDatagen())
                .peek(curEntry -> {
                    EntityType<?> curType = curEntry.getKey().get();
                    EntityTypePropertyWrapper<?> curETPW = curEntry.getValue();

                    if (curETPW.getProviderTypeRequisites().getOrDefault(getProviderType(), false) && curETPW.getParentTags().isEmpty()) {
                        throw new NullPointerException(String.format("No tags found for entity type: %s (Required by mod of ID: %s), either because validateAllEntries is set to true for this provider or the entity type itself requires validation through EntityTypePropertyWrapper#getProviderTypeRequisites().", curType.getDescriptionId(), modId));
                    }
                })
                .collect(Object2ObjectOpenHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), Object2ObjectOpenHashMap::putAll);
    }

    @Override
    protected void addObjectTags(HolderLookup.Provider provider) {
        if (!mappedModETPWs.isEmpty()) {
            mappedModETPWs.forEach((itemSupEntry, curIpw) -> {
                List<TagKey<? extends EntityType<?>>> parentTags = curIpw.getParentTags().stream().map(Supplier::get).collect(Collectors.toCollection(ObjectArrayList::new));

                if (!parentTags.isEmpty()) {
                    parentTags.forEach(curEntityTypeTag -> {
                        if (curEntityTypeTag != null) {
                            NexusConstants.LOGGER.debug("[Tagging Entity Type]: {} -> {} (For mod of ID: {})", itemSupEntry.get().getDescriptionId(), curEntityTypeTag, modId);

                            if (validateDupeObjectTag(itemSupEntry.get(), (TagKey<EntityType<?>>) curEntityTypeTag)) tag((TagKey<EntityType<?>>) curEntityTypeTag).add(itemSupEntry.get());
                        }
                    });
                }
            });
        }
    }

    @Override
    public @NotNull ProviderType getProviderType() {
        return NexusProviderTypes.ENTITY_TYPE_TAGS_PROVIDER;
    }

    @Override
    protected @Nullable String getObjectName(EntityType<?> targetObj) {
        return targetObj.getDescriptionId();
    }
}
