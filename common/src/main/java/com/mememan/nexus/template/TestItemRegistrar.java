package com.mememan.nexus.template;

import com.mememan.nexus.NexusConstants;
import com.mememan.nexus.asm.annotations.RegistrarEntry;
import com.mememan.nexus.item.standard.ItemPropertyWrapper;
import com.mememan.nexus.platform.NexusServices;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

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
            .build()
            .getParentItem();
    
    private static <I extends Item> Supplier<I> registerItem(ResourceLocation name, Supplier<I> item) {
        Supplier<I> registeredItem = NexusServices.REGISTRAR.registerObject(name, item, BuiltInRegistries.ITEM);
        ITEMS.add(registeredItem);
        return registeredItem;
    }
}
