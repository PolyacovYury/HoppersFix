package net.polyacovyury.hoppersfix.mixin;

import net.minecraft.block.BlockHopper;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.IHopper;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.tileentity.TileEntityLockableLoot;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.world.World;
import net.polyacovyury.hoppersfix.HoppersFix;
import net.polyacovyury.hoppersfix.interfaces.IPaperHopper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TileEntityHopper.class)
public abstract class MixinTileEntityHopper extends TileEntityLockableLoot implements IHopper, ITickable, IPaperHopper {

    private static final String World = "Lnet/minecraft/world/World;";
    //private static final String List = "Ljava/util/List;";
    private static final String IInventory = "Lnet/minecraft/inventory/IInventory;";
    private static final String IHopper = "Lnet/minecraft/tileentity/IHopper;";
    private static final String TEHopper = "Lnet/minecraft/tileentity/TileEntityHopper;";
    //private int entityLookupCooldown = -1;
    private boolean mayAcceptItems = false;

    @Inject(method = "getSourceInventory(" + IHopper + ")" + IInventory, at = @At(value = "HEAD"), cancellable = true)
    private static void getSourceInventory(IHopper hopper, CallbackInfoReturnable<IInventory> cir) {
        cir.setReturnValue(getInventory(hopper, true));
    }

    private static IInventory getInventory(IHopper ihopper, boolean searchForEntities) {
        return IPaperHopper.getInventory(ihopper.getWorld(), ihopper.getXPos(), ihopper.getYPos() + 1.0D, ihopper.getZPos(), searchForEntities);
    }

    @Inject(method = "getInventoryAtPosition(" + World + "DDD)" + IInventory, at = @At(value = "HEAD"), cancellable = true)
    private static void getInventoryAtPosition(World world, double d0, double d1, double d2, CallbackInfoReturnable<IInventory> info) {
        info.setReturnValue(IPaperHopper.getInventory(world, d0, d1, d2, true));
    }

    @Inject(method = "pullItems",
            at = @At(value = "INVOKE", target = TEHopper + "getSourceInventory(" + IHopper + ")" + IInventory), cancellable = true)
    private static void pullItems(IHopper hopper, CallbackInfoReturnable<Boolean> cir) {
        IInventory iinventory = getInventory(hopper, !(hopper instanceof TileEntityHopper));
        cir.setReturnValue(IPaperHopper.acceptItem(hopper, iinventory));
    }

    // tests indicate that this breaks picking up of the items by hopper minecarts on chunk borders.
    /*private boolean isOnEntityLookupCooldown() {
        return this.entityLookupCooldown > 0;
    }
    // overwriting this, so that the same list gets iterated over just 2 times instead of 50
    @Inject(method = "getCaptureItems(" + World + "DDD)" + List, at = @At("HEAD"), cancellable = true)
    private static void getCaptureItems(
            World worldIn, double x, double y, double z, CallbackInfoReturnable<List<EntityItem>> info) {
        List<EntityItem> list = Lists.newArrayList();
        Chunk chunk = worldIn.getChunk(new BlockPos(x, y, z));
        TileEntity hopper = chunk.getTileEntity(new BlockPos(x, y, z), Chunk.EnumCreateEntityType.CHECK);
        // whether this isn't a Hopper block (e.g. EntityMinecartHopper also calls this)
        if (!(hopper instanceof MixinTileEntityHopper)) {
            chunk.getEntitiesOfTypeWithinAABB(
                    EntityItem.class, new AxisAlignedBB(x - 0.5D, y, z - 0.5D, x + 0.5D, y + 1.5D, z + 0.5D), list,
                    EntitySelectors.IS_ALIVE);
            //HoppersFix.logger.info("entity lookup processed");
        }
        //HoppersFix.logger.info("entity lookup rewritten");
        info.setReturnValue(list);
    }*/

    public boolean canAcceptItems() {
        return mayAcceptItems;
    }

    @Inject(method = "update()V", at = @At("HEAD"), cancellable = true)
    private void update(CallbackInfo info) {
        mayAcceptItems = false;
        if (this.world != null && !this.world.isRemote) {
            if (HoppersFix.IGNORE_TILE_UPDATES) info.cancel();
            /*--this.entityLookupCooldown;
            if (!this.isOnEntityLookupCooldown()) {
                this.entityLookupCooldown = 0;
            }*/
        }
    }

    /* // this is not needed - the only place this gets called from was transferItemsOut() below.
    @Inject(method = "getInventoryForHopperTransfer()" + IInventory, at = @At("HEAD"), cancellable = true)
    private void getInventoryForHopperTransfer(CallbackInfoReturnable<IInventory> info) {
        EnumFacing enumfacing = BlockHopper.getFacing(this.getBlockMetadata());
        info.setReturnValue(getInventory(this.getWorld(), this.getXPos() + (double) enumfacing.getXOffset(), this.getYPos() + (double) enumfacing.getYOffset(), this.getZPos() + (double) enumfacing.getZOffset(), false));
    }*/

    @Redirect(method = "updateHopper()Z",
            at = @At(value = "INVOKE", target = TEHopper + "pullItems(" + IHopper + ")Z"))
    private boolean redirectPullItems(IHopper hopper) {
        mayAcceptItems = true;
        return TileEntityHopper.pullItems(hopper);
    }

    /*
    @Inject(method = "updateHopper()Z", at = @At("HEAD"))
    private void updateHopper(CallbackInfoReturnable<Boolean> info) {
        if (this.world != null && !this.world.isRemote) {
            if (!this.isOnTransferCooldown() && BlockHopper.isEnabled(this.getBlockMetadata())) {
                if (!this.isOnEntityLookupCooldown()) {
                    this.entityLookupCooldown = 8;  // entity lookup cooldown gets reset even if nothing happened
                    //HoppersFix.logger.info("entity lookup cooldown reset");
                }
            }
        }
    }*/

    @Inject(method = "transferItemsOut()Z", at = @At(value = "INVOKE", target = TEHopper + "getSizeInventory()I"), cancellable = true)
    private void transferItemsOut(CallbackInfoReturnable<Boolean> cir) {
        EnumFacing enumfacing = BlockHopper.getFacing(this.getBlockMetadata());
        cir.setReturnValue(IPaperHopper.hopperPush(
                this,
                IPaperHopper.getInventory(this.getWorld(), // exactly getInventoryForHopperTransfer()
                        this.getXPos() + (double) enumfacing.getXOffset(),
                        this.getYPos() + (double) enumfacing.getYOffset(),
                        this.getZPos() + (double) enumfacing.getZOffset(),
                        true), // dilemma. Turn this off - hoppers ignore the chest minecarts in front. Leave this on..
                enumfacing));
    }
}