package com.mememan.nexus.damage_type;

import com.google.common.collect.ImmutableSortedMap;
import com.mememan.nexus.datagen.ProviderType;
import com.mememan.nexus.datagen.standard.ModDataProvider;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A wrapper {@code class} used to store information referenced in datagen to simplify creating data entries for damage
 * types.
 */
public class DamageTypePropertyWrapper {
    private static final Object2ObjectLinkedOpenHashMap<Supplier<ResourceKey<DamageType>>, DamageTypePropertyWrapper> MAPPED_DTPWS = new Object2ObjectLinkedOpenHashMap<>();
    private final Supplier<ResourceKey<DamageType>> ownerDamageTypeHolder;
    @Nullable
    private DTPWBuilder builder;
    private final boolean isTemplate;

    private DamageTypePropertyWrapper(Supplier<ResourceKey<DamageType>> ownerDamageTypeHolder, boolean isTemplate) {
        this.ownerDamageTypeHolder = ownerDamageTypeHolder;
        this.isTemplate = isTemplate;
    }

    private DamageTypePropertyWrapper(Supplier<ResourceKey<DamageType>> ownerDamageTypeHolder) {
        this(ownerDamageTypeHolder, false);
    }

    private DamageTypePropertyWrapper() {
        this(null, true);
    }

    /**
     * Creates a new {@link DamageTypePropertyWrapper} instance. This is usually where you'll begin chaining {@link #builder()}
     * method calls if needed. Use this variant if you want to create a DTPW instance with a stored registration call,
     * such that its parent {@link Supplier<ResourceKey<DamageType>>} is not a generic delegate/{@code null}.
     *
     * @param ownerDamageTypeHolder The parent {@link Supplier<ResourceKey<DamageType>>} stored in the newly-initialized 
     *                              DTPW instance, typically obtained through {@link Level#registryAccess()}.
     *
     * @return A new {@link DamageTypePropertyWrapper} instance.
     */
    public static DamageTypePropertyWrapper create(Supplier<ResourceKey<DamageType>> ownerDamageTypeHolder) {
        return new DamageTypePropertyWrapper(ownerDamageTypeHolder);
    }

    /**
     * Creates a new {@link DamageTypePropertyWrapper} instance as a template. Template DTPWs are not stored in
     * {@link #MAPPED_DTPWS} and do not store a parent {@link DamageType}. They're particularly useful for re-using across
     * multiple {@linkplain DamageType DamageTypes}.
     *
     * @return A new {@link DamageTypePropertyWrapper} instance, set as a template.
     *
     * @see #of(DamageTypePropertyWrapper, Supplier)
     * @see #isTemplate()
     * @see #ofTemplate(DamageTypePropertyWrapper)
     */
    public static DamageTypePropertyWrapper createTemplate() {
        return new DamageTypePropertyWrapper();
    }

    /**
     * Creates a new {@link DamageTypePropertyWrapper} instance as a template, inheriting data from the provided DTPW
     * template. Template DTPWs are not stored in {@link #MAPPED_DTPWS} and do not store a parent {@link DamageType}.
     * They're particularly useful for re-using across multiple {@linkplain DamageType DamageTypes}.
     *
     * @param parentTemplateWrapper The parent {@link DamageTypePropertyWrapper} template from which {@link #builder()}
     *                              data should be copied.
     *
     * @return A new {@link DamageTypePropertyWrapper} instance, set as a template, inheriting from the provided DTPW template.
     * If the provided DTPW template is {@code null}, returns {@link #createTemplate()}.
     *
     * @see #createTemplate()
     * @see #isTemplate()
     */
    public static DamageTypePropertyWrapper ofTemplate(DamageTypePropertyWrapper parentTemplateWrapper) {
        if (parentTemplateWrapper != null) {
            DamageTypePropertyWrapper newTemplateWrapper = new DamageTypePropertyWrapper();

            return copyProperties(parentTemplateWrapper, newTemplateWrapper);
        } else return createTemplate();
    }

    /**
     * Creates a new {@link DamageTypePropertyWrapper} instance based on the provided {@link DamageTypePropertyWrapper}.
     * If the provided DTPW instance is {@code null}, returns {@link #create(Supplier)}. You'd typically use this if you
     * have an DTPW template you want multiple registered {@linkplain DamageType DamageTypes} to inherit from.
     *
     * @param parentWrapper The parent {@link DamageTypePropertyWrapper} instance from which {@link #builder()} should
     *                      be copied.
     * @param newDamageType The new registry entry to use for the newly constructed DTPW instance.
     *
     * @return A new {@link DamageTypePropertyWrapper} instance with copied properties based on the provided DTPW, or an
     * entirely new/clean instance if the provided DTPW is {@code null}.
     *
     * @see #of(Supplier, Supplier)
     * @see #create(Supplier)
     * @see #createTemplate()
     */
    public static DamageTypePropertyWrapper of(DamageTypePropertyWrapper parentWrapper, Supplier<ResourceKey<DamageType>> newDamageType) {
        if (parentWrapper != null) {
            DamageTypePropertyWrapper newWrapper = new DamageTypePropertyWrapper(newDamageType);

            return copyProperties(parentWrapper, newWrapper);
        } else return create(newDamageType);
    }

    /**
     * Creates a new {@link DamageTypePropertyWrapper} instance from an existing {@link DamageTypePropertyWrapper}
     * instance based on the provided {@code Supplier<ResourceKey<DamageType>>}. If no such existing DTPW instance exists, returns
     * {@link #create(Supplier)}.
     *
     * @param parentDamageType The parent {@code Supplier<ResourceKey<DamageType>>} stored in {@link #MAPPED_DTPWS}. Copies
     *                         its DTPW instance's {@link DTPWBuilder} properties if it exists, or creates a clean new
     *                         DTPW instance if it doesn't.
     * @param newDamageType The new registry entry to use for the newly constructed DTPW instance.
     *
     * @return A new {@link DamageTypePropertyWrapper} instance with copied properties based on the provided
     * {@code Supplier<ResourceKey<DamageType>>}, or an entirely new/clean instance if no such DTPW exists.
     *
     * @see #create(Supplier)
     * @see #of(DamageTypePropertyWrapper, Supplier)
     */
    public static DamageTypePropertyWrapper of(Supplier<ResourceKey<DamageType>> parentDamageType, Supplier<ResourceKey<DamageType>> newDamageType) {
        if (MAPPED_DTPWS.containsKey(parentDamageType)) {
            DamageTypePropertyWrapper originalWrapper = MAPPED_DTPWS.get(parentDamageType);
            DamageTypePropertyWrapper newWrapper = new DamageTypePropertyWrapper(newDamageType);

            return copyProperties(originalWrapper, newWrapper);
        } else return create(newDamageType);
    }

    /**
     * Shortcut utility method centered around copying builder properties over from one DTPW instance to another.
     *
     * @param from The DTPW instance to copy properties from.
     * @param to The DTPW instance to copy properties to.
     *
     * @return The provided DTPW instance with copied properties.
     */
    public static DamageTypePropertyWrapper copyProperties(DamageTypePropertyWrapper from, DamageTypePropertyWrapper to) {
        if (from.builder == null) return to;
        return to.builder()
                .withLocalizedDeathMessageComponent(from.builder.localizedDeathMessageComponent)
                .excludeFromNativeDatagen(from.builder.excludeFromNativeDatagen)
                .withSetTags(List.copyOf(from.builder.parentTags))
                .requiresSetDatagenEntries(Map.copyOf(from.builder.mappedProviderRequisites))
                .build(); // Direct setting of the builder would copy the entire object itself, which would in-turn overwrite it if any calls are made to the copied DTPW afterward
    }

    /**
     * Constructs a builder chain in which certain datagen properties can be assigned and re-built with in this DTPW instance.
     * Also sets this DTPW instance's {@link #builder} to the newly-constructed {@link DTPWBuilder} instance.
     *
     * @return A new {@link DTPWBuilder} instance from the {@link #builder} field.
     */
    public DTPWBuilder builder() {
        return this.builder = new DTPWBuilder(this, ownerDamageTypeHolder);
    }

    /**
     * Gets the cached {@link DTPWBuilder} instance from the {@link #builder} if it exists. May be {@code null}. Useful
     * for overriding specific properties after having copied another DTPW instance/already set a DTPWBuilder.
     *
     * @return The cached {@link DTPWBuilder} instance, or {@code null} if the {@link #builder} is {@code null}.
     */
    public DTPWBuilder cachedBuilder() {
        return builder;
    }

    /**
     * Gets the parent {@code Supplier<ResourceKey<DamageType>>} of this DTPW instance.
     *
     * @return The parent {@code Supplier<ResourceKey<DamageType>>} stored in this DTPW instance.
     */
    public Supplier<ResourceKey<DamageType>> getOwnerDamageType() {
        return ownerDamageTypeHolder;
    }

    /**
     * Gets the localized death message for the parent damage type. This is typically parsed and
     * formatted for value mapping inside the language file. May be {@code null}.
     *
     * @return The localized death message component, or {@code null} if the {@link #builder} is {@code null} or the
     * value itself is {@code null}.
     */
    @Nullable
    public String getLocalizedDeathMessageComponent() {
        return builder != null ? builder.localizedDeathMessageComponent : null;
    }

    /**
     * Gets the defined parent {@linkplain Supplier<TagKey<DamageType>> Tags} from the {@link #builder()} if the builder
     * exists.
     *
     * @return The defined parent {@linkplain Supplier<TagKey<DamageType>> Tags}, or an empty {@link ObjectArrayList} if
     * the {@link #builder()} is {@code null}.
     */
    public List<Supplier<TagKey<DamageType>>> getParentTags() {
        return builder == null ? ObjectArrayList.of() : builder.parentTags;
    }

    /**
     * Whether data present in {@link #builder()} (if not {@code null}) should automatically be handled/generated by
     * Nexus API.
     *
     * @return {@code true} if {@link #builder} isn't {@code null} and {@link DTPWBuilder#excludeFromNativeDatagen} is
     * set to {@code true}, {@code false} otherwise.
     */
    public boolean excludeFromNativeDatagen() {
        return builder != null && builder.excludeFromNativeDatagen;
    }

    /**
     * Gets a {@link Map} (usually {@link Object2BooleanOpenHashMap}) specifying the {@linkplain ProviderType ProviderTypes}
     * for which this DTPW instance requires data to present for generation. May be empty if {@link #builder} is {@code null}
     * or the underlying {@link Map} is also empty.
     *
     * @return The {@link Map} representing different {@linkplain ProviderType ProviderTypes} and their requirements for
     * datagen. May be empty.
     */
    public Map<ProviderType, Boolean> getProviderTypeRequisites() {
        return builder == null ? new Object2BooleanOpenHashMap<>() : builder.mappedProviderRequisites;
    }

    /**
     * Whether this DTPW instance is a template. Templates are not stored in {@link #getMappedDtpws()} ()} and have no
     * parent {@link DamageType}.
     *
     * @return Whether this DTPW instance is a template.
     *
     * @see #of(DamageTypePropertyWrapper, Supplier)
     * @see #createTemplate()
     */
    public boolean isTemplate() {
        return isTemplate;
    }

    /**
     * Gets an immutable view (via {@link ImmutableSortedMap}) of {@link #MAPPED_DTPWS}.
     *
     * @return An immutable view (via {@link ImmutableSortedMap}) of {@link #MAPPED_DTPWS}.
     */
    public static ImmutableSortedMap<Supplier<ResourceKey<DamageType>>, DamageTypePropertyWrapper> getMappedDtpws() {
        return ImmutableSortedMap.copyOf(MAPPED_DTPWS);
    }

    /**
     * A builder {@code class} used to construct certain damage type-related data for datagen (e.g. localized death
     * messages).
     */
    public static class DTPWBuilder {
        private final DamageTypePropertyWrapper ownerWrapper;
        private final Supplier<ResourceKey<DamageType>> ownerDamageType;
        @Nullable
        private String localizedDeathMessageComponent;
        private final List<Supplier<TagKey<DamageType>>> parentTags = ObjectArrayList.of();
        private boolean excludeFromNativeDatagen = false;
        private final Map<ProviderType, Boolean> mappedProviderRequisites = new Object2BooleanOpenHashMap<>();

        private DTPWBuilder(DamageTypePropertyWrapper ownerWrapper, Supplier<ResourceKey<DamageType>> ownerDamageType) {
            this.ownerWrapper = ownerWrapper;
            this.ownerDamageType = ownerDamageType;
        }

        /**
         * Sets the localized death message for the parent damage source.
         *
         * @param localizedDeathMessageComponent The localized death message component.
         *
         * @return {@code this} (builder method)
         */
        public DTPWBuilder withLocalizedDeathMessageComponent(String localizedDeathMessageComponent) {
            this.localizedDeathMessageComponent = localizedDeathMessageComponent;
            return this;
        }

        /**
         * Tags this DTPWBuilder's parent {@link DamageType} with the provided {@link TagKey<DamageType>}.
         *
         * @param parentDamageTypeTag The {@code TagKey<DamageType>} with which this DTPW's parent {@link DamageType}
         *                            will be tagged. May only be of type {@link DamageType}.
         *
         * @return {@code this} (builder method).
         */
        public DTPWBuilder withTag(Supplier<TagKey<DamageType>> parentDamageTypeTag) {
            this.parentTags.add(parentDamageTypeTag);
            return this;
        }

        /**
         * Tags this DTPWBuilder's parent {@link DamageType} with the provided {@linkplain TagKey<DamageType> Tags}.
         * Appends to the existing list.
         *
         * @param parentDamageTypeTags The {@linkplain TagKey<DamageType> TagKeys} with which this DTPW's parent {@link DamageType}
         *                             will be tagged. May only be of type {@link DamageType}.
         *
         * @return {@code this} (builder method).
         *
         * @see #withSetTags(List)
         */
        public DTPWBuilder withTags(List<Supplier<TagKey<DamageType>>> parentDamageTypeTags) {
            this.parentTags.addAll(parentDamageTypeTags);
            return this;
        }

        /**
         * Tags this DTPWBuilder's parent DamageType with the provided {@linkplain TagKey<DamageType> Tags}. Overwrites
         * the existing {@link List}.
         *
         * @param parentDamageTypeTags The {@linkplain TagKey<DamageType> TagKeys} with which this DTPW's parent
         *                             {@link DamageType} will be tagged. May only be of type {@link DamageType}.
         *
         * @return {@code this} (builder method).
         *
         * @see #withTags(List)
         */
        public DTPWBuilder withSetTags(List<Supplier<TagKey<DamageType>>> parentDamageTypeTags) {
            this.parentTags.clear();
            this.parentTags.addAll(parentDamageTypeTags);
            return this;
        }

        /**
         * Determines whether this DTPWBuilder instance should be entirely excluded from Nexus' native datagen.
         * <br></br>
         * Fundamentally, all this does is flag this instance as not needing a data entry to be mapped to it. You may
         * choose to generate data for it yourself if needed, since Nexus won't handle datagen for this particular object.
         * <br></br>
         * If a block-specific data provider has {@link ModDataProvider#validateAllEntries()} set to {@code true}, this
         * instance (and its children, so long as this value isn't modified) will still be excluded from datagen, and thus
         * an exception won't be thrown for it.
         *
         * @param excludeFromNativeDatagen Whether this instance's data should be passed into Nexus' native datagen for
         *                                 data generation.
         *
         * @return {@code this} (builder method).
         *
         * @see #excludeFromNativeDatagen()
         * @see #requiresDatagenEntry(ProviderType, boolean)
         */
        public DTPWBuilder excludeFromNativeDatagen(boolean excludeFromNativeDatagen) {
            this.excludeFromNativeDatagen = excludeFromNativeDatagen;
            return this;
        }

        /**
         * Determines whether this DTPWBuilder instance is required to generate necessary block-related data based on the
         * {@link ProviderType} passed in.
         * <br></br>
         * By default, unmapped providers will not require an entry for this DTPWBuilder to be generated unless
         * {@link ModDataProvider#validateAllEntries()} is set to {@code true}.
         * <br></br>
         * Mapping the related provider passed in here to {@code requiresDatagenEntry}, set to {@code true}, will flag
         * this DTPWBuilder instance for requiring related data regardless of what
         * {@link ModDataProvider#validateAllEntries()} is set to.
         *
         * @param targetProviderType The {@link ProviderType} to modify the data entry requirement for.
         * @param requiresDatagenEntry Whether this DTPWBuilder should require data related to the specified
         *                             {@code targetProviderType} to be present.
         *
         * @return {@code this} (builder method).
         *
         * @see #requiresDatagenEntries(List, boolean)
         * @see #requiresSetDatagenEntries(List, boolean)
         * @see #requiresSetDatagenEntries(Map)
         * @see #excludeFromNativeDatagen(boolean)
         */
        public DTPWBuilder requiresDatagenEntry(ProviderType targetProviderType, boolean requiresDatagenEntry) {
            mappedProviderRequisites.put(targetProviderType, requiresDatagenEntry);
            return this;
        }

        /**
         * Overloaded variant of {@link #requiresDatagenEntry(ProviderType, boolean)}. Maps each of the
         * {@linkplain ProviderType ProviderTypes} passed in to {@code requiresDatagenEntry}.
         *
         * @param targetProviderTypes The {@link List} of {@linkplain ProviderType ProviderTypes} to modify the data
         *                            entry requirements for.
         * @param requiresDatagenEntry Whether this DTPWBuilder should require data related to each of the
         *                             specified {@code targetProviderTypes} to be present.
         *
         * @return {@code this} (builder method).
         *
         * @see #requiresDatagenEntry(ProviderType, boolean)
         * @see #requiresSetDatagenEntries(List, boolean)
         * @see #requiresSetDatagenEntries(Map)
         * @see #excludeFromNativeDatagen(boolean)
         */
        public DTPWBuilder requiresDatagenEntries(List<ProviderType> targetProviderTypes, boolean requiresDatagenEntry) {
            targetProviderTypes.forEach(type -> requiresDatagenEntry(type, requiresDatagenEntry));
            return this;
        }

        /**
         * Overloaded variant of {@link #requiresDatagenEntry(ProviderType, boolean)}. Maps each of the
         * {@linkplain ProviderType ProviderTypes} passed in to {@code requiresDatagenEntry}. Overrides the existing
         * {@link Map}.
         *
         * @param targetProviderTypes The {@link List} of {@linkplain ProviderType ProviderTypes} to modify the data
         *                            entry requirements for.
         * @param requiresDatagenEntry Whether this DTPWBuilder should require data related to each of the
         *                             specified {@code targetProviderTypes} to be present.
         *
         * @return {@code this} (builder method).
         *
         * @see #requiresDatagenEntry(ProviderType, boolean)
         * @see #requiresDatagenEntries(List, boolean)
         * @see #requiresSetDatagenEntries(Map)
         * @see #excludeFromNativeDatagen(boolean)
         */
        public DTPWBuilder requiresSetDatagenEntries(List<ProviderType> targetProviderTypes, boolean requiresDatagenEntry) {
            mappedProviderRequisites.clear();
            targetProviderTypes.forEach(type -> requiresDatagenEntry(type, requiresDatagenEntry));
            return this;
        }

        /**
         * Overloaded variant of {@link #requiresDatagenEntry(ProviderType, boolean)}. Maps each of the
         * {@linkplain ProviderType ProviderTypes} passed in to {@code requiresDatagenEntry}. Overrides the existing
         * {@link Map}.
         *
         * @param mappedProviderRequisites The {@link Map} of provider requisites to override the existing {@link Map}
         *                                 with.
         *
         * @return {@code this} (builder method).
         *
         * @see #requiresDatagenEntry(ProviderType, boolean)
         * @see #requiresDatagenEntries(List, boolean)
         * @see #requiresSetDatagenEntries(List, boolean)
         * @see #excludeFromNativeDatagen(boolean)
         */
        public DTPWBuilder requiresSetDatagenEntries(Map<ProviderType, Boolean> mappedProviderRequisites) {
            this.mappedProviderRequisites.clear();
            this.mappedProviderRequisites.putAll(mappedProviderRequisites);
            return this;
        }

        /**
         * Builds a new {@link DamageTypePropertyWrapper} using this builder's data. Also maps the owner
         * {@link DamageTypePropertyWrapper} to the parent {@linkplain DamageType} if isn't already mapped and
         * {@link #isTemplate()} is {@code false}.
         *
         * @return The newly data-populated {@link DamageTypePropertyWrapper}.
         */
        public DamageTypePropertyWrapper build() {
            MAPPED_DTPWS.putIfAbsent(ownerDamageType, ownerWrapper);
            return ownerWrapper;
        }
    }
}