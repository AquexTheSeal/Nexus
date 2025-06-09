package com.mememan.nexus;

import com.mememan.nexus.asm.annotations.RegistrarEntry;
import com.mememan.nexus.item.standard.ItemPropertyWrapper;
import com.mememan.nexus.platform.NexusServices;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.function.Supplier;

@RegistrarEntry
public class Test {

    public static final Supplier<Item> TEST_ITEM = ItemPropertyWrapper.create(NexusServices.REGISTRAR.registerObject(new ResourceLocation("appleskin", "test_gitem"), () -> new Item(new Item.Properties()), BuiltInRegistries.ITEM))
            .builder()
            .withRecipe(r -> result -> ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, result.get())
                    .requires(Items.ACACIA_BOAT)
                    .unlockedBy("has_" + BuiltInRegistries.ITEM.getKey(Items.ACACIA_BOAT).getPath(), InventoryChangeTrigger.TriggerInstance.hasItems(Items.ACACIA_BOAT))
                    .save(r, RecipeBuilder.getDefaultRecipeId(result.get())))
            .build()
            .getParentItem();
}
