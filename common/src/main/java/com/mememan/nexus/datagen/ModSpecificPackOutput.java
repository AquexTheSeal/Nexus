package com.mememan.nexus.datagen;

import com.mememan.nexus.loader.ModData;
import net.minecraft.data.PackOutput;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public class ModSpecificPackOutput extends PackOutput {
    @NotNull
    protected final ModData ownerMod;
    protected final boolean shouldGenerate;

    public ModSpecificPackOutput(Path outputPath, @NotNull ModData ownerMod, boolean shouldGenerate) {
        super(outputPath);

        this.ownerMod = ownerMod;
        this.shouldGenerate = shouldGenerate;
    }

    @Override
    public @NotNull Path getOutputFolder(Target outputTarget) {
        return super.getOutputFolder(outputTarget);
    }

    @NotNull
    public ModData getOwnerMod() {
        return ownerMod;
    }

    public boolean shouldGenerate() {
        return shouldGenerate;
    }
}
