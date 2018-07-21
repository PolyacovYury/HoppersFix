package net.polyacovyury.hoppersfix.mixin;

import com.google.common.collect.Lists;
import net.minecraft.block.BlockHopper;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.tileentity.IHopper;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.tileentity.TileEntityLockableLoot;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.polyacovyury.hoppersfix.HoppersFix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(TileEntityHopper.class)
public abstract class MixinTileEntityHopper extends TileEntityLockableLoot implements IHopper, ITickable {

    private static final String World = "Lnet/minecraft/world/World;";
    private static final String List = "Ljava/util/List;";

    // overwriting this, so that the same list gets iterated over just 2 times instead of 50
    @Inject(method = "getCaptureItems(" + World + "DDD)" + List, at = @At("HEAD"), cancellable = true)
    private static void getCaptureItems(
            World worldIn, double x, double y, double z, CallbackInfoReturnable<List<EntityItem>> info) {
        List<EntityItem> list = Lists.newArrayList();
        worldIn.getChunkFromBlockCoords(new BlockPos(x, y, z)).getEntitiesOfTypeWithinAABB(
                EntityItem.class, new AxisAlignedBB(x, y, z, x, y + 1.5D, z), list, EntitySelectors.IS_ALIVE);
        // HoppersFix.logger.info("entity lookup rewritten");
        info.setReturnValue(list);
    }

    @Shadow
    abstract void setTransferCooldown(int ticks);

    @Shadow
    abstract boolean isOnTransferCooldown();

    @Inject(method = "updateHopper()Z", at = @At("RETURN"), cancellable = true)
    protected void updateHopper(CallbackInfoReturnable<Boolean> info) {
        boolean result = info.getReturnValueZ();
        if (this.world != null && !this.world.isRemote) {
            if (!this.isOnTransferCooldown() && BlockHopper.isEnabled(this.getBlockMetadata())) {
                if (!result) {
                    this.setTransferCooldown(8);  // transfer cooldown gets reset even if nothing happened
                    // HoppersFix.logger.info("transfer cooldown reset");
                }
            }
        }
    }
}