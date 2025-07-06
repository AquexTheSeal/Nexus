package com.mememan.nexus.datagen.standard.tag;

import com.mememan.nexus.NexusConstants;
import com.mememan.nexus.datagen.DuplicateDataPolicy;
import com.mememan.nexus.datagen.NexusProviderTypes;
import com.mememan.nexus.datagen.ProviderType;
import com.mememan.nexus.enchantment.standard.EnchantmentPropertyWrapper;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class StandardEnchantmentTagProvider extends StandardTagProvider<Enchantment> {
    protected final Object2ObjectOpenHashMap<Supplier<Enchantment>, EnchantmentPropertyWrapper> mappedModEPWs;

    public StandardEnchantmentTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> objectLookupProvider, String modId, boolean validateAllEntries, DuplicateDataPolicy dupeStrat) {
        super(output, Registries.ENCHANTMENT, objectLookupProvider, enchantment -> BuiltInRegistries.ENCHANTMENT.getResourceKey(enchantment).orElseThrow(), modId, validateAllEntries, dupeStrat);

        this.mappedModEPWs = EnchantmentPropertyWrapper.getMappedEpws().entrySet()
                .stream()
                .filter(curEntry -> BuiltInRegistries.ENCHANTMENT.getKey(curEntry.getKey().get()).getNamespace().equals(modId))
                .filter(curEntry -> !curEntry.getValue().excludeFromNativeDatagen())
                .peek(curEntry -> {
                    Enchantment curEnchantment = curEntry.getKey().get();
                    EnchantmentPropertyWrapper curEPW = curEntry.getValue();

                    if ((validateAllEntries() || curEPW.getProviderTypeRequisites().getOrDefault(getProviderType(), false)) && curEPW.getParentTags().isEmpty()) {
                        throw new IllegalStateException(String.format("No tags found for enchantment: %s (Required by mod of ID: %s), either because validateAllEntries is set to true for this provider or the enchantment itself requires validation through EnchantmentPropertyWrapper#getProviderTypeRequisites().", curEnchantment.getDescriptionId(), modId));
                    }
                })
                .collect(Object2ObjectOpenHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), Object2ObjectOpenHashMap::putAll);
    }

    @Override
    protected void addObjectTags(HolderLookup.Provider provider) {
        if (!mappedModEPWs.isEmpty()) {
            mappedModEPWs.forEach((enchantmentSupEntry, curEpw) -> {
                List<TagKey<Enchantment>> parentTags = curEpw.getParentTags().stream().map(Supplier::get).collect(Collectors.toCollection(ObjectArrayList::new));

                if (!parentTags.isEmpty()) {
                    parentTags.forEach(curEnchantmentTag -> {
                        if (curEnchantmentTag != null) {
                            if (validateDupeObjectTag(enchantmentSupEntry.get(), curEnchantmentTag)) {
                                NexusConstants.LOGGER.debug("[Tagging Enchantment]: {} -> {} (For mod of ID: {})", enchantmentSupEntry.get().getDescriptionId(), curEnchantmentTag, modId);

                                tag(curEnchantmentTag).add(enchantmentSupEntry.get());
                            }
                        }
                    });
                }
            });
        }
    }

    @Override
    public @NotNull ProviderType getProviderType() {
        return NexusProviderTypes.ENCHANTMENT_TAGS_PROVIDER;
    }

    @Override
    protected @Nullable String getObjectName(Enchantment targetObj) {
        return targetObj.getDescriptionId();
    }
}
