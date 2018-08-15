package net.polyacovyury.hoppersfix.mixin;

import com.google.common.collect.Lists;
import net.minecraft.block.BlockHopper;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.tileentity.IHopper;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.tileentity.TileEntityLockableLoot;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.polyacovyury.hoppersfix.HoppersFix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(TileEntityHopper.class)
public abstract class MixinTileEntityHopper extends TileEntityLockableLoot implements IHopper, ITickable {

    private static final String World = "Lnet/minecraft/world/World;";
    private static final String List = "Ljava/util/List;";
    private int entityLookupCooldown = -1;

    @Shadow
    abstract boolean isOnTransferCooldown();

    public boolean isOnEntityLookupCooldown() {return this.entityLookupCooldown > 0;}

    @Inject(method = "update()V", at = @At("HEAD"))
    public void update(CallbackInfo info) {
        if (this.world != null && !this.world.isRemote) {
            --this.entityLookupCooldown;
            if (!this.isOnEntityLookupCooldown()) {
                this.entityLookupCooldown = 0;
            }
        }
    }

    // overwriting this, so that the same list gets iterated over just 2 times instead of 50
    @Inject(method = "getCaptureItems(" + World + "DDD)" + List, at = @At("HEAD"), cancellable = true)
    private static void getCaptureItems(
            World worldIn, double x, double y, double z, CallbackInfoReturnable<List<EntityItem>> info) {
        List<EntityItem> list = Lists.newArrayList();
        Chunk chunk = worldIn.getChunk(new BlockPos(x, y, z));
        TileEntity hopper = chunk.getTileEntity(new BlockPos(x, y, z), Chunk.EnumCreateEntityType.CHECK);
        if (hopper instanceof MixinTileEntityHopper && !((MixinTileEntityHopper)hopper).isOnEntityLookupCooldown()) {
            chunk.getEntitiesOfTypeWithinAABB(
                    EntityItem.class, new AxisAlignedBB(x - 0.5D, y, z - 0.5D, x + 0.5D, y + 1.5D, z + 0.5D), list,
                    EntitySelectors.IS_ALIVE);
            //HoppersFix.logger.info("entity lookup processed");
        }
        //HoppersFix.logger.info("entity lookup rewritten");
        info.setReturnValue(list);
    }

    @Inject(method = "updateHopper()Z", at = @At("RETURN"), cancellable = true)
    private void updateHopper(CallbackInfoReturnable<Boolean> info) {
        boolean result = info.getReturnValue();
        if (this.world != null && !this.world.isRemote) {
            if (!this.isOnTransferCooldown() && BlockHopper.isEnabled(this.getBlockMetadata())) {
                if (!this.isOnEntityLookupCooldown()) {
                    this.entityLookupCooldown = 8;  // entity lookup cooldown gets reset even if nothing happened
                    //HoppersFix.logger.info("entity lookup cooldown reset");
                }
            }
        }
    }
}