package com.mememan.nexus.datagen.standard.tag;

import com.mememan.nexus.NexusConstants;
import com.mememan.nexus.damage_type.DamageTypePropertyWrapper;
import com.mememan.nexus.datagen.DuplicateDataPolicy;
import com.mememan.nexus.datagen.NexusProviderTypes;
import com.mememan.nexus.datagen.ProviderType;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class StandardDamageTypeTagProvider extends DynamicTagProvider<DamageType> {
    protected final Object2ObjectOpenHashMap<Supplier<ResourceKey<DamageType>>, DamageTypePropertyWrapper> mappedModDTPWs;

    public StandardDamageTypeTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> objectLookupProvider, String modId, boolean validateAllEntries, DuplicateDataPolicy dupeStrat) {
        super(output, Registries.DAMAGE_TYPE, objectLookupProvider, modId, validateAllEntries, dupeStrat);

        this.mappedModDTPWs = DamageTypePropertyWrapper.getMappedDtpws().entrySet()
                .stream()
                .filter(curEntry -> curEntry.getKey().get().location().getNamespace().equals(modId))
                .filter(curEntry -> !curEntry.getValue().excludeFromNativeDatagen())
                .peek(curEntry -> {
                    ResourceKey<DamageType> curDamageTypeKey = curEntry.getKey().get();
                    DamageTypePropertyWrapper curDTPW = curEntry.getValue();

                    if ((validateAllEntries() || curDTPW.getProviderTypeRequisites().getOrDefault(getProviderType(), false)) && curDTPW.getParentTags().isEmpty()) {
                        throw new IllegalStateException(String.format("No tags found for damage type: %s (Required by mod of ID: %s), either because validateAllEntries is set to true for this provider or the damage type itself requires validation through DamageTypePropertyWrapper#getProviderTypeRequisites().", curDamageTypeKey.location(), modId));
                    }
                })
                .collect(Object2ObjectOpenHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), Object2ObjectOpenHashMap::putAll);
    }

    @Override
    protected void addObjectTags(HolderLookup.Provider provider) {
        if (!mappedModDTPWs.isEmpty()) {
            mappedModDTPWs.forEach((damageTypeSupEntry, curEpw) -> {
                List<TagKey<DamageType>> parentTags = curEpw.getParentTags().stream().map(Supplier::get).collect(Collectors.toCollection(ObjectArrayList::new));

                if (!parentTags.isEmpty()) {
                    parentTags.forEach(curDamageTypeTag -> {
                        if (curDamageTypeTag != null) {
                            if (validateDupeObjectTag(damageTypeSupEntry.get(), curDamageTypeTag)) {
                                NexusConstants.LOGGER.debug("[Tagging Damage Type]: {} -> {} (For mod of ID: {})", damageTypeSupEntry.get().location(), curDamageTypeTag, modId);

                                tag(curDamageTypeTag).add(damageTypeSupEntry.get());
                            }
                        }
                    });
                }
            });
        }
    }

    @Override
    public @NotNull ProviderType getProviderType() {
        return NexusProviderTypes.DAMAGE_TYPE_TAGS_PROVIDER;
    }
}
