package com.mememan.nexus.internal.loader;

import com.mememan.nexus.NexusConstants;
import com.mememan.nexus.asm.ClassFinder;
import com.mememan.nexus.loader.ModData;
import com.mememan.nexus.loader.ModMetadata;
import com.mememan.nexus.loader.ModSide;
import cpw.mods.jarhandling.SecureJar;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraftforge.forgespi.language.IModInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Forge-specific implementation of {@link ModData}.
 * <br></br>
 * Pre-emptively caches mod data on startup, using {@link IModInfo} as the backing source for mod meta/data. Additionally
 * handles mapping forge-specific metadata and logs general operations executed on object construction.
 */
public class ForgeModData implements ModData {
    private final IModInfo ownerModInfo;
    private final ModMetadata ownerModMetadata;
    private final ObjectArrayList<String> allFilePaths;
    public final ConcurrentHashMap<String, ObjectArrayList<String>> cachedAnnotatedClasses = new ConcurrentHashMap<>(); // Stored as strings to avoid unnecessary classloading

    public ForgeModData(IModInfo targetMod) {
        long startTime = System.currentTimeMillis();

        this.ownerModInfo = targetMod;

        String authorInput = targetMod.getConfig().<String>getConfigElement("authors")
                .orElse(targetMod.getConfig().<String>getConfigElement("credits")
                        .orElse("None"));

        this.ownerModMetadata = new ModMetadata(targetMod.getModId(), targetMod.getDisplayName(),
                targetMod.getVersion().getQualifier(), ownerModInfo.getOwningFile().getLicense(),
                targetMod.getDescription(), ObjectArrayList.of(authorInput.contains(",") ? authorInput.split(",") : authorInput.split("\\s+")), // Really presumptuous split
                targetMod.getConfig().<Boolean>getConfigElement("clientSideOnly").orElse(false)
                        ? ModSide.CLIENT
                        : ModSide.COMMON);
        this.allFilePaths = mapAllFilePaths(ownerModInfo.getOwningFile().getFile().getSecureJar());

        ownerModInfo.getOwningFile().getFile().getScanResult().getAnnotations().forEach(curAnnotData -> {
            cachedAnnotatedClasses.computeIfAbsent(
                            curAnnotData.annotationType().getClassName().replace('/', '.'),
                            (ok) -> new ObjectArrayList<>())
                    .add(curAnnotData.clazz().getClassName().replace('/', '.'));
        });

        long endTime = System.currentTimeMillis();
        long milliDuration = endTime - startTime;

        NexusConstants.LOGGER.info("Loaded mod data for mod {} (within file {}) in {} ms", targetMod.getModId(), targetMod.getOwningFile().getFile().getFileName(), milliDuration);
    }

    public IModInfo getOwnerModInfo() {
        return ownerModInfo;
    }

    /**
     * Repurposed variant of {@link SecureJar#getPackages()} that gets all file paths.
     *
     * @param targetJar The {@link SecureJar} to index the paths of. Typically defaults to the owning JAR file of this
     *                  instance's {@link #ownerModInfo}
     *
     * @return A newly-computed {@link ObjectArrayList} of all formatted paths within a mod's {@link SecureJar} file, or
     * an empty {@link ObjectArrayList} if some exception is caught.
     */
    public ObjectArrayList<String> mapAllFilePaths(SecureJar targetJar) {
        try (Stream<Path> rootJarStream = Files.walk(targetJar.getRootPath())) {
            return rootJarStream
                    .filter(curPath -> curPath.getNameCount() > 0)
                    .filter(Files::isRegularFile)
                    .map(curPath -> curPath.toString().replace('/','.'))
                    .filter(pkg-> !pkg.isEmpty())
                    .collect(Collectors.toCollection(ObjectArrayList::new));
        } catch (IOException e) {
            NexusConstants.LOGGER.error("Failed to map paths for SecureJar: {}", targetJar.name(), e);
            return ObjectArrayList.of();
        }
    }

    @Override
    public @NotNull ModMetadata getModMetadata() {
        return ownerModMetadata;
    }

    @Override
    public ObjectArrayList<String> getAllFilePaths() {
        return allFilePaths;
    }

    @Override
    public List<Class<?>> discoverAnnotatedClasses(Class<? extends Annotation> annotationTypeClazz, @Nullable Comparator<String> classLoadingSorter) {
        return cachedAnnotatedClasses.get(annotationTypeClazz.getName()) == null ? ObjectArrayList.of() : cachedAnnotatedClasses.get(annotationTypeClazz.getName())
                .stream()
                .sorted(classLoadingSorter != null ? classLoadingSorter : String::compareTo)
                .map(ClassFinder::forName)
                .collect(Collectors.toCollection(ObjectArrayList::new));
    }
}
