package com.mememan.nexus.template;

import com.mememan.nexus.NexusConstants;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.function.Supplier;

public class AnotherClass {

    public static Map<ResourceLocation, Supplier<ClampedItemPropertyFunction>> g(){
        return Map.of(NexusConstants.prefix("huh"), () -> (a, b, c, d) -> 0.5F);
    }
}
