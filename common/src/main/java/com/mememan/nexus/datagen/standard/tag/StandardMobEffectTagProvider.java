package com.mememan.nexus.datagen.standard.tag;

import com.mememan.nexus.NexusConstants;
import com.mememan.nexus.datagen.DuplicateDataPolicy;
import com.mememan.nexus.datagen.NexusProviderTypes;
import com.mememan.nexus.datagen.ProviderType;
import com.mememan.nexus.mob_effect.standard.MobEffectPropertyWrapper;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class StandardMobEffectTagProvider extends StandardTagProvider<MobEffect> {
    protected final Object2ObjectOpenHashMap<Supplier<MobEffect>, MobEffectPropertyWrapper> mappedModMEPWs;

    public StandardMobEffectTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> objectLookupProvider, String modId, boolean validateAllEntries, DuplicateDataPolicy dupeStrat) {
        super(output, Registries.MOB_EFFECT, objectLookupProvider, mobEffect -> BuiltInRegistries.MOB_EFFECT.getResourceKey(mobEffect).orElseThrow(), modId, validateAllEntries, dupeStrat);

        this.mappedModMEPWs = MobEffectPropertyWrapper.getMappedMepws().entrySet()
                .stream()
                .filter(curEntry -> BuiltInRegistries.MOB_EFFECT.getKey(curEntry.getKey().get()).getNamespace().equals(modId))
                .filter(curEntry -> !curEntry.getValue().excludeFromNativeDatagen())
                .peek(curEntry -> {
                    MobEffect curMobEffect = curEntry.getKey().get();
                    MobEffectPropertyWrapper curMEPW = curEntry.getValue();

                    if ((validateAllEntries() || curMEPW.getProviderTypeRequisites().getOrDefault(getProviderType(), false)) && curMEPW.getParentTags().isEmpty()) {
                        throw new IllegalStateException(String.format("No tags found for mob effect: %s (Required by mod of ID: %s), either because validateAllEntries is set to true for this provider or the mob effect itself requires validation through MobEffectPropertyWrapper#getProviderTypeRequisites().", curMobEffect.getDescriptionId(), modId));
                    }
                })
                .collect(Object2ObjectOpenHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), Object2ObjectOpenHashMap::putAll);
    }

    @Override
    protected void addObjectTags(HolderLookup.Provider provider) {
        if (!mappedModMEPWs.isEmpty()) {
            mappedModMEPWs.forEach((mobEffectSupEntry, curEpw) -> {
                List<TagKey<MobEffect>> parentTags = curEpw.getParentTags().stream().map(Supplier::get).collect(Collectors.toCollection(ObjectArrayList::new));

                if (!parentTags.isEmpty()) {
                    parentTags.forEach(curMobEffectTag -> {
                        if (curMobEffectTag != null) {
                            if (validateDupeObjectTag(mobEffectSupEntry.get(), curMobEffectTag)) {
                                NexusConstants.LOGGER.debug("[Tagging Mob Effect]: {} -> {} (For mod of ID: {})", mobEffectSupEntry.get().getDescriptionId(), curMobEffectTag, modId);

                                tag(curMobEffectTag).add(mobEffectSupEntry.get());
                            }
                        }
                    });
                }
            });
        }
    }

    @Override
    public @NotNull ProviderType getProviderType() {
        return NexusProviderTypes.MOB_EFFECT_TAGS_PROVIDER;
    }

    @Override
    protected @Nullable String getObjectName(MobEffect targetObj) {
        return targetObj.getDescriptionId();
    }
}
