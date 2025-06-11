package com.mememan.nexus.template;

import com.mememan.nexus.NexusConstants;
import com.mememan.nexus.asm.annotations.RegistrarEntry;
import com.mememan.nexus.item.standard.ItemPropertyWrapper;
import com.mememan.nexus.platform.NexusServices;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.function.Supplier;

@RegistrarEntry
public class TestItemRegistrar {
    private static final ObjectArrayList<Supplier<? extends Item>> ITEMS = new ObjectArrayList<>();

    public static final Supplier<Item> TEST_ITEM = ItemPropertyWrapper.create(registerItem(NexusConstants.prefix("test_item"), () -> new Item(new Item.Properties())))
            .builder()
            .asCompostable(I -> 20.0F)
            .asFuel(I -> 200)
            .withParentCreativeModeTab(() -> CreativeModeTabs.allTabs().get(3))
            .withTag(() -> ItemTags.ACACIA_LOGS)
            .withRecipe(r -> result -> ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, result.get())
                    .requires(Items.ACACIA_BOAT)
                    .unlockedBy("has_" + BuiltInRegistries.ITEM.getKey(Items.ACACIA_BOAT).getPath(), InventoryChangeTrigger.TriggerInstance.hasItems(Items.ACACIA_BOAT))
                    .save(r, new ResourceLocation("nexus", "test_item")))
            .bypassDefaultTranslation()
            .build()
            .getParentItem();
    public static final Supplier<Item> TEST_ITEM_2 = ItemPropertyWrapper.create(registerItem(new ResourceLocation("forge", "test_item_2"), () -> new Item(new Item.Properties())))
            .builder()
            .asCompostable(I -> 20.0F)
            .asFuel(I -> 200)
            .withParentCreativeModeTab(() -> CreativeModeTabs.allTabs().get(3))
            .withTag(() -> ItemTags.ACACIA_LOGS)
            .withRecipe(r -> result -> ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, result.get())
                    .requires(Items.ACACIA_BOAT)
                    .unlockedBy("has_" + BuiltInRegistries.ITEM.getKey(Items.ACACIA_BOAT).getPath(), InventoryChangeTrigger.TriggerInstance.hasItems(Items.ACACIA_BOAT))
                    .save(r, new ResourceLocation("forge", "test_item")))
            .build()
            .getParentItem();
    
    private static <I extends Item> Supplier<I> registerItem(ResourceLocation name, Supplier<I> item) {
        Supplier<I> registeredItem = NexusServices.REGISTRAR.registerObject(name, item, BuiltInRegistries.ITEM);
        ITEMS.add(registeredItem);
        return registeredItem;
    }
}
