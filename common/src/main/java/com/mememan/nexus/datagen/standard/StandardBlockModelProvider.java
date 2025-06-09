package com.mememan.nexus.datagen.standard;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.data.models.ModelProvider;

import java.util.concurrent.CompletableFuture;

public class StandardBlockModelProvider extends ModelProvider {
    protected final PackOutput.PathProvider blockModelPathProvider;

    public StandardBlockModelProvider(PackOutput targetPackOutput) {
        super(targetPackOutput);
        this.blockModelPathProvider = targetPackOutput.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cachedOutput) {
        return super.run(cachedOutput);
    }
}
