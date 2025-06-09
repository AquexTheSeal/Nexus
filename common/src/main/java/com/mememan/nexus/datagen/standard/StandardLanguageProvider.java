package com.mememan.nexus.datagen.standard;

import com.mememan.nexus.NexusConstants;
import com.mememan.nexus.block.standard.BlockPropertyWrapper;
import com.mememan.nexus.datagen.DuplicateDataPolicy;
import com.mememan.nexus.datagen.NexusProviderTypes;
import com.mememan.nexus.datagen.ProviderType;
import com.mememan.nexus.entity.EntityTypePropertyWrapper;
import com.mememan.nexus.item.standard.ItemPropertyWrapper;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectRBTreeMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class StandardLanguageProvider implements ModDataProvider {
    protected final Object2ObjectRBTreeMap<String, String> localizationEntries = new Object2ObjectRBTreeMap<>();
    protected final PackOutput output;
    protected final String modId;
    protected final String locale;
    protected final boolean validateAllEntries;
    protected final DuplicateDataPolicy dupeStrat;
    protected final Path outputPath;
    protected final Object2ObjectOpenHashMap<Supplier<Block>, BlockPropertyWrapper> mappedModBPWs;
    protected final Object2ObjectOpenHashMap<Supplier<Item>, ItemPropertyWrapper> mappedModIPWs;
    protected final Object2ObjectOpenHashMap<Supplier<? extends EntityType<?>>, EntityTypePropertyWrapper<?>> mappedModETPWs;

    public StandardLanguageProvider(PackOutput output, String modId, String locale, boolean validateAllEntries, DuplicateDataPolicy dupeStrat) {
        this.output = output;
        this.modId = modId;
        this.locale = locale;
        this.validateAllEntries = validateAllEntries;
        this.dupeStrat = dupeStrat;

        this.outputPath = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve(modId).resolve("lang").resolve(locale + ".json");

        this.mappedModBPWs = BlockPropertyWrapper.getMappedBpws().entrySet()
                .stream()
                .filter(curEntry -> BuiltInRegistries.BLOCK.getKey(curEntry.getKey().get()).getNamespace().equals(modId))
                .filter(curEntry -> !curEntry.getValue().excludeFromNativeDatagen())
                .collect(Object2ObjectOpenHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), Object2ObjectOpenHashMap::putAll);
        this.mappedModIPWs = ItemPropertyWrapper.getMappedIpws().entrySet()
                .stream()
                .filter(curEntry -> BuiltInRegistries.ITEM.getKey(curEntry.getKey().get()).getNamespace().equals(modId))
                .filter(curEntry -> !curEntry.getValue().excludeFromNativeDatagen())
                .collect(Object2ObjectOpenHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), Object2ObjectOpenHashMap::putAll);
        this.mappedModETPWs = EntityTypePropertyWrapper.getMappedEtpws().entrySet()
                .stream()
                .filter(curEntry -> BuiltInRegistries.ENTITY_TYPE.getKey(curEntry.getKey().get()).getNamespace().equals(modId))
                .filter(curEntry -> !curEntry.getValue().excludeFromNativeDatagen())
                .collect(Object2ObjectOpenHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), Object2ObjectOpenHashMap::putAll);
    }

    protected void addTranslations() {

    }

    protected void validatePossibleTranslationEntries() {

    }

    @Override
    public CompletableFuture<?> run(CachedOutput cachedOutput) {
        addTranslations();



        return CompletableFuture.allOf();
    }

    @Override
    public @NotNull String getModId() {
        return modId;
    }

    @Override
    public @NotNull String getName() {
        return String.format("Language [%s] [%s]", getLocale(), getModId());
    }

    @Override
    public boolean validateAllEntries() {
        return validateAllEntries;
    }

    @Override
    public @NotNull ProviderType getProviderType() {
        return NexusProviderTypes.LANGUAGE_PROVIDER;
    }

    @Override
    public @NotNull DuplicateDataPolicy getDuplicateDataPolicy() {
        return dupeStrat;
    }

    public String getLocale() {
        return locale;
    }

    public void addBlock(Supplier<? extends Block> targetBlockSup, String localizedBlockName) {
        addBlock(targetBlockSup.get(), localizedBlockName);
    }

    public void addBlock(Block targetBlock, String localizedBlockName) {
        add(targetBlock.getDescriptionId(), localizedBlockName);
    }

    public void addEnchantment(Supplier<? extends Enchantment> targetEnchantmentSup, String localizedEnchantmentName) {
        addEnchantment(targetEnchantmentSup.get(), localizedEnchantmentName);
    }

    public void addEnchantment(Enchantment targetEnchantment, String localizedEnchantmentName) {
        add(targetEnchantment.getDescriptionId(), localizedEnchantmentName);
    }

    public void addEntityType(Supplier<? extends EntityType<?>> targetEntityTypeSup, String localizedEntityTypeName) {
        addEntityType(targetEntityTypeSup.get(), localizedEntityTypeName);
    }

    public void addEntityType(EntityType<?> targetEntityType, String localizedEntityTypeName) {
        add(targetEntityType.getDescriptionId(), localizedEntityTypeName);
    }

    public void addItem(Supplier<? extends Item> targetItemSup, String localizedItemName) {
        addItem(targetItemSup.get(), localizedItemName);
    }

    public void addItem(Item targetItem, String localizedItemName) {
        add(targetItem.getDescriptionId(), localizedItemName);
    }

    public void addMobEffect(Supplier<? extends MobEffect> targetMobEffectSup, String localizedMobEffectName) {
        addMobEffect(targetMobEffectSup.get(), localizedMobEffectName);
    }

    public void addMobEffect(MobEffect targetMobEffect, String localizedMobEffectName) {
        add(targetMobEffect.getDescriptionId(), localizedMobEffectName);
    }

    public void add(String unlocalizedKey, String localizedValue) {
        boolean isAlreadyMapped = localizationEntries.containsKey(unlocalizedKey);

        if (isAlreadyMapped) {
            switch (dupeStrat) {
                case CRASH -> throw new IllegalStateException(String.format("Attempted to localize duplicate translation key (%s -> %s) from mod of ID %s, specified DuplicateDataPolicy is CRASH.", unlocalizedKey, localizedValue, getModId()));
                case EXCLUDE_WARN -> NexusConstants.LOGGER.warn("Attempted to localize duplicate translation key ({} -> {}) from mod of ID {}, specified DuplicateDataPolicy is EXCLUDE_WARN. Skipping...", unlocalizedKey, localizedValue, getModId());
                case EXCLUDE_SILENT -> {}
                case OVERRIDE_WARN -> {
                    NexusConstants.LOGGER.warn("Overriding duplicate translation key ({} -> {}) from mod of ID {}, specified DuplicateDataPolicy is OVERRIDE_WARN.", unlocalizedKey, localizedValue, getModId());

                    localizationEntries.put(unlocalizedKey, localizedValue);
                }
                case OVERRIDE_SILENT -> localizationEntries.put(unlocalizedKey, localizedValue);
            }
        } else localizationEntries.put(unlocalizedKey, localizedValue);
    }
}
