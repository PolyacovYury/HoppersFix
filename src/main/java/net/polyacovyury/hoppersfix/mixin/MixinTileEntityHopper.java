package net.polyacovyury.hoppersfix.mixin;

import com.google.common.collect.Lists;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockHopper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityMinecartHopper;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.*;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.polyacovyury.hoppersfix.HoppersFix;
import net.polyacovyury.hoppersfix.interfaces.IPaperHopper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(TileEntityHopper.class)
public abstract class MixinTileEntityHopper extends TileEntityLockableLoot implements IHopper, ITickable, IPaperHopper {

    @Shadow public abstract void setTransferCooldown(int ticks);

    private static final String World = "Lnet/minecraft/world/World;";
    private static final String List = "Ljava/util/List;";
    private static final String IInventory = "Lnet/minecraft/inventory/IInventory;";
    private static final String IHopper = "Lnet/minecraft/tileentity/IHopper;";
    private static final String TEHopper = "Lnet/minecraft/tileentity/TileEntityHopper;";
    private static final String EnumFacing = "Lnet/minecraft/util/EnumFacing;";
    private int entityLookupCooldown = -1;
    private boolean mayAcceptItems = false;

    @Inject(method = "getSourceInventory(" + IHopper + ")" + IInventory, at = @At(value = "HEAD"), cancellable = true)
    private static void getSourceInventory(IHopper hopper, CallbackInfoReturnable<IInventory> cir) {
        cir.setReturnValue(getInventory(hopper, true));
    }

    private static IInventory getInventory(IHopper ihopper, boolean searchForEntities) {
        return getInventory(ihopper.getWorld(), ihopper.getXPos(), ihopper.getYPos() + 1.0D, ihopper.getZPos(), searchForEntities);
    }

    @Inject(method = "getInventoryAtPosition(" + World + "DDD)" + IInventory, at = @At(value = "HEAD"), cancellable = true)
    private static void getInventoryAtPosition(World world, double d0, double d1, double d2, CallbackInfoReturnable<IInventory> info) {
        info.setReturnValue(getInventory(world, d0, d1, d2, true));
    }

    private static IInventory getInventory(World worldIn, double x, double y, double z, boolean searchForEntities) {
        IInventory iinventory = null;
        int i = MathHelper.floor(x);
        int j = MathHelper.floor(y);
        int k = MathHelper.floor(z);
        BlockPos blockpos = new BlockPos(i, j, k);
        net.minecraft.block.state.IBlockState state = worldIn.getBlockState(blockpos);
        Block block = state.getBlock();
        if (block.hasTileEntity(state)) {
            TileEntity tileentity = worldIn.getTileEntity(blockpos);
            if (tileentity instanceof IInventory) {
                iinventory = (IInventory) tileentity;
                if (iinventory instanceof TileEntityChest && block instanceof BlockChest) {
                    iinventory = ((BlockChest) block).getContainer(worldIn, blockpos, true);
                }
            }
        }
        if (iinventory == null && searchForEntities) {
            List<Entity> list = worldIn.getEntitiesInAABBexcluding(null, new AxisAlignedBB(x - 0.5D, y - 0.5D, z - 0.5D, x + 0.5D, y + 0.5D, z + 0.5D), EntitySelectors.HAS_INVENTORY);
            if (!list.isEmpty()) {
                iinventory = (IInventory) list.get(worldIn.rand.nextInt(list.size()));
            }
        }
        return iinventory;
    }

    // overwriting this, so that the same list gets iterated over just 2 times instead of 50
    @Inject(method = "getCaptureItems(" + World + "DDD)" + List, at = @At("HEAD"), cancellable = true)
    private static void getCaptureItems(
            World worldIn, double x, double y, double z, CallbackInfoReturnable<List<EntityItem>> info) {
        List<EntityItem> list = Lists.newArrayList();
        /*Chunk chunk = worldIn.getChunk(new BlockPos(x, y, z));
        TileEntity hopper = chunk.getTileEntity(new BlockPos(x, y, z), Chunk.EnumCreateEntityType.CHECK);
        // whether this isn't a Hopper block (e.g. EntityMinecartHopper also calls this) or it isn't on cooldown
        if (!(hopper instanceof MixinTileEntityHopper) || !((MixinTileEntityHopper)hopper).isOnEntityLookupCooldown()) {
            chunk.getEntitiesOfTypeWithinAABB(
                    EntityItem.class, new AxisAlignedBB(x - 0.5D, y, z - 0.5D, x + 0.5D, y + 1.5D, z + 0.5D), list,
                    EntitySelectors.IS_ALIVE);
            //HoppersFix.logger.info("entity lookup processed");
        }
        //HoppersFix.logger.info("entity lookup rewritten");*/
        info.setReturnValue(list);
    }

    @Inject(method = "pullItems", at = @At(value = "HEAD"))
    private static void pullItems(IHopper hopper, CallbackInfoReturnable<Boolean> cir) {
        IInventory iinventory = getInventory(hopper, !(hopper instanceof TileEntityHopper));
        cir.setReturnValue(IPaperHopper.acceptItem(hopper, iinventory));
    }

    private boolean isOnEntityLookupCooldown() {
        return this.entityLookupCooldown > 0;
    }

    public boolean canAcceptItems() {
        return mayAcceptItems;
    }

    @Inject(method = "update()V", at = @At("HEAD"), cancellable = true)
    private void update(CallbackInfo info) {
        if (this.world != null && !this.world.isRemote) {
            if (HoppersFix.IGNORE_TILE_UPDATES) info.cancel();
            --this.entityLookupCooldown;
            if (!this.isOnEntityLookupCooldown()) {
                this.entityLookupCooldown = 0;
            }
        }
    }

    @Inject(method = "getInventoryForHopperTransfer()" + IInventory, at = @At("HEAD"), cancellable = true)
    private void getInventoryForHopperTransfer(CallbackInfoReturnable<IInventory> info) {
        EnumFacing enumfacing = BlockHopper.getFacing(this.getBlockMetadata());
        info.setReturnValue(getInventory(this.getWorld(), this.getXPos() + (double) enumfacing.getXOffset(), this.getYPos() + (double) enumfacing.getYOffset(), this.getZPos() + (double) enumfacing.getZOffset(), true));
    }

    @Redirect(method = "updateHopper()Z",
            at = @At(value = "INVOKE", target = TEHopper + "pullItems(" + IHopper + ")Z"))
    private boolean redirectPullItems(IHopper hopper, CallbackInfoReturnable<Boolean> cir) {
        mayAcceptItems = true;
        return TileEntityHopper.pullItems(hopper);
    }

    /*@Shadow
    protected abstract boolean isOnTransferCooldown();*/

    @Inject(method = "updateHopper()Z", at = @At("HEAD"))
    private void updateHopper(CallbackInfoReturnable<Boolean> info) {
        mayAcceptItems = false;
    }/*
        if (this.world != null && !this.world.isRemote) {
            if (!this.isOnTransferCooldown() && BlockHopper.isEnabled(this.getBlockMetadata())) {
                if (!this.isOnEntityLookupCooldown()) {
                    this.entityLookupCooldown = 8;  // entity lookup cooldown gets reset even if nothing happened
                    //HoppersFix.logger.info("entity lookup cooldown reset");
                }
            }
        }
    }*/

    private boolean hopperPush(IInventory iinventory, EnumFacing enumfacing) {
        boolean foundItem = false;
        for (int i = 0; i < this.getSizeInventory(); ++i) {
            if (!this.getStackInSlot(i).isEmpty()) {
                foundItem = true;
                ItemStack origItemStack = this.getStackInSlot(i);
                final int origCount = origItemStack.getCount();
                final ItemStack itemstack2 = TileEntityHopper.putStackInInventoryAllSlots(this, iinventory, this.decrStackSize(i, 1), enumfacing);
                final int remaining = itemstack2.getCount();
                if (remaining != origCount) {
                    origItemStack = origItemStack.copy();
                    origItemStack.setCount(remaining);
                    this.setInventorySlotContents(i, origItemStack);
                    iinventory.markDirty();
                    return true;
                }
                origItemStack.setCount(origCount);
            }
        }
        if (foundItem) { // Inventory was full - cooldown
            this.setTransferCooldown(8);
        }
        return false;
    }

    @Inject(method = "transferItemsOut()Z", at=@At(value="INVOKE", target = TEHopper + "getSizeInventory()I"), cancellable = true)
    private void transferItemsOut(CallbackInfoReturnable<Boolean> cir) {
        EnumFacing enumfacing = BlockHopper.getFacing(this.getBlockMetadata());
        cir.setReturnValue(hopperPush(
            // exactly getInventoryForHopperTransfer()
            getInventory(this.getWorld(), this.getXPos() + (double) enumfacing.getXOffset(), this.getYPos() + (double) enumfacing.getYOffset(), this.getZPos() + (double) enumfacing.getZOffset(), true),
            enumfacing));
    }
}