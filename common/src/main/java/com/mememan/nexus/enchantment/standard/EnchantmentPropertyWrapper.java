package com.mememan.nexus.enchantment.standard;

import com.google.common.collect.ImmutableMap;
import com.mememan.nexus.item.standard.ItemPropertyWrapper;
import com.mememan.nexus.platform.NexusServices;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Wrapper {@code class} used to store information referenced in datagen, as well as some other hardcoded enchantment-
 * oriented info, to simplify enchantment creation/registration.
 */
public class EnchantmentPropertyWrapper {
    private static final Object2ObjectLinkedOpenHashMap<Supplier<Enchantment>, EnchantmentPropertyWrapper> MAPPED_EPWS = new Object2ObjectLinkedOpenHashMap<>();
    @Nullable
    private final ResourceLocation enchantmentRegName;
    private final Supplier<Enchantment> parentEnchantment;
    private final boolean isTemplate;
    @Nullable
    private EPWBuilder builder;

    private EnchantmentPropertyWrapper(@Nullable ResourceLocation enchantmentRegName, Supplier<Enchantment> parentEnchantment) {
        this.enchantmentRegName = enchantmentRegName;
        this.parentEnchantment = parentEnchantment;
        this.isTemplate = false;
    }

    private EnchantmentPropertyWrapper(Supplier<Enchantment> parentEnchantment) {
        this(null, parentEnchantment);
    }

    private EnchantmentPropertyWrapper() {
        this.enchantmentRegName = null;
        this.parentEnchantment = null;
        this.isTemplate = true; // Otherwise can't set with constructor overloading (Laziness:tm:)
    }

    /**
     * Creates a new {@link EnchantmentPropertyWrapper} instance. This is usually where you'll begin chaining {@link #builder()}
     * method calls if needed. Use this variant if you want to directly create an EPW during a registration call rather
     * than after it/without storing it.
     *
     * @param enchantmentRegName The registry name of the {@code parentEnchantment}. Used when access to
     *                           the {@code parentEnchantment} returns a fortune delegate (I.E. It's too early to access
     *                           the parent enchantment, see {@link BuiltInRegistries#ENCHANTMENT}).
     * @param parentEnchantment The parent {@code Supplier<Enchantment>} stored in the newly-initialized EPW instance.
     *
     * @return A new {@link EnchantmentPropertyWrapper} instance.
     *
     * @apiNote You shouldn't have to use this method for 95% of cases since you'd be inlining a registration call for
     * {@link #parentEnchantment}, see {@link #create(Supplier)} instead.
     *
     * @see #create(Supplier)
     * @see #of(ResourceLocation, Supplier)
     * @see #of(EnchantmentPropertyWrapper, Supplier)
     * @see #of(Supplier, Supplier)
     */
    public static EnchantmentPropertyWrapper create(ResourceLocation enchantmentRegName, Supplier<Enchantment> parentEnchantment) {
        return new EnchantmentPropertyWrapper(enchantmentRegName, parentEnchantment);
    }

    /**
     * Creates a new {@link EnchantmentPropertyWrapper} instance. This is usually where you'll begin chaining {@link #builder()}
     * method calls if needed. Use this variant if you want to create an EPW instance with a stored registration call,
     * such that its parent {@code Supplier<Enchantment>} is not a fortune enchantment delegate (see {@link BuiltInRegistries#ENCHANTMENT}).
     *
     * @param parentEnchantment The parent {@code Supplier<Enchantment>} stored in the newly-initialized EPW instance.
     *
     * @return A new {@link EnchantmentPropertyWrapper} instance.
     *
     * @see #of(Supplier, Supplier)
     * @see #of(ResourceLocation, Supplier)
     * @see #of(EnchantmentPropertyWrapper, Supplier)
     */
    public static EnchantmentPropertyWrapper create(Supplier<Enchantment> parentEnchantment) {
        return new EnchantmentPropertyWrapper(parentEnchantment);
    }

    /**
     * Creates a new {@link EnchantmentPropertyWrapper} instance as a template. Template EPWs are not stored in
     * {@link #MAPPED_EPWS} and do not store a parent {@link Enchantment}. They're particularly useful for re-using across
     * multiple {@linkplain Enchantment Enchantments}.
     *
     * @return A new {@link EnchantmentPropertyWrapper} instance, set as a template.
     *
     * @see #of(EnchantmentPropertyWrapper, Supplier)
     * @see #isTemplate()
     * @see #ofTemplate(EnchantmentPropertyWrapper)
     */
    public static EnchantmentPropertyWrapper createTemplate() {
        return new EnchantmentPropertyWrapper();
    }

    /**
     * Creates a new {@link EnchantmentPropertyWrapper} instance as a template, inheriting data from the provided EPW
     * template. Template EPWs are not stored in {@link #MAPPED_EPWS} and do not store a parent {@link Enchantment}.
     * They're particularly useful for re-using across multiple {@linkplain Enchantment Enchantments}.
     *
     * @param parentTemplateWrapper The parent {@link EnchantmentPropertyWrapper} template from which {@link #builder()}
     *                              data should be copied.
     *
     * @return A new {@link ItemPropertyWrapper} instance, set as a template, inheriting from the provided EPW template.
     * If the provided EPW template is {@code null}, returns {@link #createTemplate()}.
     *
     * @see #createTemplate()
     * @see #isTemplate()
     */
    public static EnchantmentPropertyWrapper ofTemplate(EnchantmentPropertyWrapper parentTemplateWrapper) {
        if (parentTemplateWrapper != null) {
            EnchantmentPropertyWrapper newTemplateWrapper = new EnchantmentPropertyWrapper();

            return copyProperties(parentTemplateWrapper, newTemplateWrapper);
        } else return createTemplate();
    }

    /**
     * Creates a new {@link EnchantmentPropertyWrapper} instance based on the provided {@link EnchantmentPropertyWrapper}.
     * If the provided EPW instance is {@code null}, returns {@link #create(Supplier)}. You'd typically use this if you
     * have an EPW template you want multiple registered {@linkplain Enchantment Enchantments} to inherit from.
     *
     * @param parentWrapper The parent {@link EnchantmentPropertyWrapper} instance from which {@link #builder()} should
     *                      be copied.
     * @param newEnchantment The new registry entry to use for the newly constructed EPW instance.
     *
     * @return A new {@link EnchantmentPropertyWrapper} instance with copied properties based on the provided EPW, or an
     * entirely new/clean instance if the provided EPW is {@code null}.
     *
     * @see #of(Supplier, Supplier)
     * @see #of(ResourceLocation, Supplier)
     * @see #create(Supplier)
     * @see #create(ResourceLocation, Supplier)
     * @see #createTemplate()
     */
    public static EnchantmentPropertyWrapper of(EnchantmentPropertyWrapper parentWrapper, Supplier<Enchantment> newEnchantment) {
        if (parentWrapper != null) {
            EnchantmentPropertyWrapper newWrapper = new EnchantmentPropertyWrapper(newEnchantment);

            return copyProperties(parentWrapper, newWrapper);
        } else return create(newEnchantment);
    }

    /**
     * Creates a new {@link EnchantmentPropertyWrapper} instance from an existing {@link EnchantmentPropertyWrapper}
     * instance based on the provided {@code Supplier<Enchantment>}. If no such existing EPW instance exists, returns
     * {@link #create(Supplier)}.
     *
     * @param parentEnchantment The parent {@code Supplier<Enchantment>} stored in {@link #MAPPED_EPWS}. Copies its EPW instance's
     *                   {@link EPWBuilder} properties if it exists, or creates a clean new EPW instance if it doesn't.
     * @param newEnchantment The new registry entry to use for the newly constructed EPW instance.
     *
     * @return A new {@link EnchantmentPropertyWrapper} instance with copied properties based on the provided
     * {@code Supplier<Enchantment>}, or an entirely new/clean instance if no such EPW exists.
     *
     * @see #create(Supplier)
     * @see #create(ResourceLocation, Supplier)
     * @see #of(ResourceLocation, Supplier)
     * @see #of(EnchantmentPropertyWrapper, Supplier)
     */
    public static EnchantmentPropertyWrapper of(Supplier<Enchantment> parentEnchantment, Supplier<Enchantment> newEnchantment) {
        if (MAPPED_EPWS.containsKey(parentEnchantment)) {
            EnchantmentPropertyWrapper originalWrapper = MAPPED_EPWS.get(parentEnchantment);
            EnchantmentPropertyWrapper newWrapper = new EnchantmentPropertyWrapper(newEnchantment);

            return copyProperties(originalWrapper, newWrapper);
        } else return create(newEnchantment);
    }

    /**
     * Overloaded variant of {@link #of(Supplier, Supplier)} that copies the parent enchantment's
     * {@linkplain Enchantment#getRarity() rarity}, {@linkplain Enchantment#category category}, and
     * {@linkplain Enchantment#slots slots}. Note that this does not copy the provided enchantment's actual behaviour.
     *
     * @param newEnchantmentRegName The new registry name by which the newly constructed {@code Supplier<Enchantment>} instance
     *                              will be stored.
     * @param parentEnchantment The parent {@code Supplier<Enchantment>} stored in {@link #MAPPED_EPWS}. Must already be
     *                          a registered {@link Enchantment}.
     *
     * @return A new {@link EnchantmentPropertyWrapper} instance with copied properties (rarity, slots, category)
     * based on the provided {@code Supplier<Enchantment>}, or an entirely new/clean instance if no such EPW exists.
     *
     * @see #of(EnchantmentPropertyWrapper, Supplier)
     * @see #of(Supplier, Supplier)
     * @see #create(Supplier)
     * @see #create(ResourceLocation, Supplier)
     */
    public static EnchantmentPropertyWrapper of(ResourceLocation newEnchantmentRegName, Supplier<Enchantment> parentEnchantment) {
        Enchantment parentAsEnchantment = parentEnchantment.get();

        return of(parentEnchantment, NexusServices.REGISTRAR.registerObject(newEnchantmentRegName, () -> new Enchantment(parentAsEnchantment.getRarity(), parentAsEnchantment.category, parentAsEnchantment.slots) {}, BuiltInRegistries.ENCHANTMENT));
    }

    /**
     * Shortcut utility method centered around copying builder properties over from one EPW instance to another.
     *
     * @param from The EPW instance to copy properties from.
     * @param to The EPW instance to copy properties to.
     *
     * @return The provided EPW instance with copied properties.
     */
    public static EnchantmentPropertyWrapper copyProperties(EnchantmentPropertyWrapper from, EnchantmentPropertyWrapper to) {
        if (from.builder == null) return to;
        return to.builder()
                .build(); // Direct setting of the builder would copy the entire object itself, which would in-turn overwrite it if any calls are made to the copied EPW afterward
    }

    /**
     * Constructs a builder chain in which certain datagen properties can be assigned and re-built with in this
     * EnchantmentPropertyWrapper instance. Also sets this EPW instance's {@link #builder} to the newly-constructed
     * {@link EPWBuilder} instance.
     * <br></br>
     * <b>NOTE: THIS WILL OVERRIDE {@link #builder} ENTIRELY EVEN IF IT'S NOT {@code null} (e.g. you're inheriting from
     * a template, see {@link #of(EnchantmentPropertyWrapper, Supplier)}).</b>
     *
     * @return A new {@link EPWBuilder} instance from the {@link #builder} field.
     *
     * @see #cachedBuilder()
     */
    public EPWBuilder builder() {
        return this.builder = new EPWBuilder(this, parentEnchantment);
    }

    /**
     * Gets the cached {@link EPWBuilder} instance from the {@link #builder} if it exists. May be {@code null}. Useful
     * for overriding specific properties after having copied another EPW instance/already set an EPWBuilder.
     *
     * @return The cached {@link EPWBuilder} instance, or {@code null} if the {@link #builder} is {@code null}.
     *
     * @see #of(ResourceLocation, Supplier)
     * @see #of(Supplier, Supplier)
     * @see #builder()
     */
    @Nullable
    public EPWBuilder cachedBuilder() {
        return builder;
    }

    /**
     * Gets the parent {@code Supplier<Enchantment>} of this EPW instance.
     *
     * @return The parent {@code Supplier<Enchantment>} stored in this EPW instance.
     */
    public Supplier<Enchantment> getParentEnchantment() {
        return parentEnchantment;
    }

    /**
     * Whether this EPW instance is a template. Templates are not stored in {@link #getMappedEpws()} and have no parent
     * {@link Enchantment}.
     *
     * @return Whether this EPW instance is a template.
     *
     * @see #of(EnchantmentPropertyWrapper, Supplier)
     * @see #createTemplate()
     */
    public boolean isTemplate() {
        return isTemplate;
    }

    /**
     * Gets an immutable view (via {@link ImmutableMap}) of {@link #MAPPED_EPWS}.
     *
     * @return An immutable view (via {@link ImmutableMap}) of {@link #MAPPED_EPWS}.
     */
    public static ImmutableMap<Supplier<Enchantment>, EnchantmentPropertyWrapper> getMappedEpws() {
        return ImmutableMap.copyOf(MAPPED_EPWS);
    }

    /**
     * A builder class used to construct certain enchantment-related data for datagen and other data related to hardcoded
     * enchantment settings, such as enchantment name and level.
     */
    public static class EPWBuilder {
        private final EnchantmentPropertyWrapper ownerWrapper;
        private final Supplier<Enchantment> enchantmentParent;

        public EPWBuilder(EnchantmentPropertyWrapper ownerWrapper, Supplier<Enchantment> enchantmentParent) {
            this.ownerWrapper = ownerWrapper;
            this.enchantmentParent = enchantmentParent;
        }

        /**
         * Builds a new {@link EnchantmentPropertyWrapper} using this builder's data. Also maps the owner
         * {@link EnchantmentPropertyWrapper} to the parent {@link Enchantment} if the owner is not a template.
         *
         * @return The newly data-populated {@link EnchantmentPropertyWrapper}.
         *
         * @see EnchantmentPropertyWrapper#isTemplate()
         */
        public EnchantmentPropertyWrapper build() {
            if (!ownerWrapper.isTemplate) MAPPED_EPWS.putIfAbsent(ownerWrapper.enchantmentRegName == null ? ownerWrapper.parentEnchantment : () -> BuiltInRegistries.ENCHANTMENT.get(ownerWrapper.enchantmentRegName), ownerWrapper);
            return ownerWrapper;
        }
    }
}
