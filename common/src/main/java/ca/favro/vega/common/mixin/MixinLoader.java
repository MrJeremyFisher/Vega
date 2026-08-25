package ca.favro.vega.common.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class MixinLoader implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {

    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // TODO: Think up a better way to do this lol
        return switch (mixinClassName.substring(34)) {
            case "VoxelMixin",
                 "VoxelWaypointManagerAccessor",
                 "VoxelWaypointContainerAccessor",
                 "VoxelCompressibleMapRegionMixin" -> FabricLoader.getInstance().isModLoaded("voxelmap");
            case "CivModernPlayerWaypointsMixin",
                 "CivModernPlayerWaypointsAccessor",
                 "CivModernMapScreenMixin",
                 "CivModernMinimapMixin" -> FabricLoader.getInstance().isModLoaded("civmodern");
            case "JourneymapWaypointStoreMixin" -> FabricLoader.getInstance().isModLoaded("journeymap");
            default -> true;
        };
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }
}
