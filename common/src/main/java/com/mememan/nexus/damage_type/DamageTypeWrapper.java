package com.mememan.nexus.damage_type;

import com.google.common.collect.ImmutableSortedMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * A wrapper {@code class} used to store information referenced in datagen to simplify creating data entries for damage
 * types.
 */
public class DamageTypeWrapper {
    private static final Object2ObjectLinkedOpenHashMap<Supplier<ResourceKey<DamageType>>, DamageTypeWrapper> MAPPED_DTWS = new Object2ObjectLinkedOpenHashMap<>();
    private final Supplier<ResourceKey<DamageType>> ownerDamageTypeHolder;
    @Nullable
    private DTWBuilder builder;

    private DamageTypeWrapper(Supplier<ResourceKey<DamageType>> ownerDamageTypeHolder) {
        this.ownerDamageTypeHolder = ownerDamageTypeHolder;
    }

    /**
     * Creates a new {@link DamageTypeWrapper} instance. This is usually where you'll begin chaining {@link #builder()}
     * method calls if needed. Use this variant if you want to create a DTW instance with a stored registration call,
     * such that its parent {@link Supplier<ResourceKey<DamageType>>} is not a generic delegate/{@code null}.
     *
     * @param ownerDamageTypeHolder The parent {@link Supplier<ResourceKey<DamageType>>} stored in the newly-initialized DTW instance, typically obtained through {@link Level#registryAccess()}.
     *
     * @return A new {@link DamageTypeWrapper} instance.
     */
    public static DamageTypeWrapper create(Supplier<ResourceKey<DamageType>> ownerDamageTypeHolder) {
        return new DamageTypeWrapper(ownerDamageTypeHolder);
    }

    /**
     * Constructs a builder chain in which certain datagen properties can be assigned and re-built with in this DTW instance.
     * Also sets this DTW instance's {@link #builder} to the newly-constructed {@link DTWBuilder} instance.
     *
     * @return A new {@link DTWBuilder} instance from the {@link #builder} field.
     */
    public DTWBuilder builder() {
        return this.builder = new DTWBuilder(this, ownerDamageTypeHolder);
    }

    /**
     * Gets the cached {@link DTWBuilder} instance from the {@link #builder} if it exists. May be {@code null}. Useful
     * for overriding specific properties after having copied another DTW instance/already set a DTWBuilder.
     *
     * @return The cached {@link DTWBuilder} instance, or {@code null} if the {@link #builder} is {@code null}.
     */
    public DTWBuilder cachedBuilder() {
        return builder;
    }

    /**
     * Gets the parent {@code Supplier<ResourceKey<DamageType>>} of this DTW instance.
     *
     * @return The parent {@code Supplier<ResourceKey<DamageType>>} stored in this DTW instance.
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
     * Gets an immutable view (via {@link ImmutableSortedMap}) of {@link #MAPPED_DTWS}.
     *
     * @return An immutable view (via {@link ImmutableSortedMap}) of {@link #MAPPED_DTWS}.
     */
    public static ImmutableSortedMap<Supplier<ResourceKey<DamageType>>, DamageTypeWrapper> getMappedDtws() {
        return ImmutableSortedMap.copyOf(MAPPED_DTWS);
    }

    /**
     * A builder {@code class} used to construct certain damage type-related data for datagen (e.g. localized death
     * messages).
     */
    public static class DTWBuilder {
        private final DamageTypeWrapper ownerWrapper;
        private final Supplier<ResourceKey<DamageType>> ownerDamageType;
        @Nullable
        private String localizedDeathMessageComponent;

        private DTWBuilder(DamageTypeWrapper ownerWrapper, Supplier<ResourceKey<DamageType>> ownerDamageType) {
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
        public DTWBuilder withLocalizedDeathMessageComponent(String localizedDeathMessageComponent) {
            this.localizedDeathMessageComponent = localizedDeathMessageComponent;
            return this;
        }

        /**
         * Builds a new {@link DamageTypeWrapper} using this builder's data. Also maps the owner
         * {@link DamageTypeWrapper} to the parent {@linkplain DamageType} if isn't already mapped.
         *
         * @return The newly data-populated {@link DamageTypeWrapper}.
         */
        public DamageTypeWrapper build() {
            MAPPED_DTWS.putIfAbsent(ownerDamageType, ownerWrapper);
            return ownerWrapper;
        }
    }
}