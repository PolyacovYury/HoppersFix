package net.polyacovyury.hoppersfix.interfaces;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityMinecartHopper;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.IHopper;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.polyacovyury.hoppersfix.HoppersFix;

public interface IPaperHopper extends IHopper {
    static boolean acceptItem(IHopper hopper, IInventory iinventory) {
        if (iinventory != null) {
            EnumFacing enumfacing = EnumFacing.DOWN;

            if (isInventoryEmpty(iinventory, enumfacing)) {
                return false;
            }

            if (iinventory instanceof ISidedInventory) {
                ISidedInventory isidedinventory = (ISidedInventory) iinventory;
                int[] arr = isidedinventory.getSlotsForFace(enumfacing);

                for (int i : arr) {
                    if (pullItemFromSlot(hopper, iinventory, i, enumfacing)) {
                        return true;
                    }
                }
            } else {
                int j = iinventory.getSizeInventory();

                for (int k = 0; k < j; ++k) {
                    if (pullItemFromSlot(hopper, iinventory, k, enumfacing)) {
                        return true;
                    }
                }
            }
        } else if (!(hopper instanceof TileEntityHopper)) {  // hoppers will not look for items, but minecarts will
            for (EntityItem entityitem : TileEntityHopper.getCaptureItems(hopper.getWorld(), hopper.getXPos(), hopper.getYPos(), hopper.getZPos())) {
                if (TileEntityHopper.putDropInInventoryAllSlots(null, hopper, entityitem)) {
                    return true;
                }
            }
        }

        return false;

    }

    static boolean isInventoryEmpty(IInventory inventoryIn, EnumFacing side) {
        if (inventoryIn instanceof ISidedInventory) {
            ISidedInventory isidedinventory = (ISidedInventory) inventoryIn;
            int[] arr = isidedinventory.getSlotsForFace(side);

            for (int i : arr) {
                if (!isidedinventory.getStackInSlot(i).isEmpty()) {
                    return false;
                }
            }
        } else {
            int j = inventoryIn.getSizeInventory();

            for (int k = 0; k < j; ++k) {
                if (!inventoryIn.getStackInSlot(k).isEmpty()) {
                    return false;
                }
            }
        }

        return true;
    }

    static boolean pullItemFromSlot(IHopper hopper, IInventory inventoryIn, int index, EnumFacing direction) {
        ItemStack origItemStack = inventoryIn.getStackInSlot(index);

        if (!origItemStack.isEmpty() && canExtractItemFromSlot(inventoryIn, origItemStack, index, direction)) {
            ItemStack itemstack = origItemStack.copy();
            final int origCount = origItemStack.getCount();
            final int moved = Math.min(1, origCount);
            itemstack.setCount(moved);

            final ItemStack itemstack2 = TileEntityHopper.putStackInInventoryAllSlots(inventoryIn, hopper, itemstack, null);
            final int remaining = itemstack2.getCount();
            if (remaining != moved) {
                origItemStack = origItemStack.copy();
                origItemStack.setCount(origCount - moved + remaining);
                HoppersFix.IGNORE_TILE_UPDATES = true;
                inventoryIn.setInventorySlotContents(index, origItemStack);
                HoppersFix.IGNORE_TILE_UPDATES = false;
                inventoryIn.markDirty();
                return true;
            }
            origItemStack.setCount(origCount);
            cooldownHopper(hopper);
        }
        return false;
    }

    @SuppressWarnings("SameReturnValue")
    static boolean cooldownHopper(IHopper hopper) {
        if (hopper instanceof TileEntityHopper) {
            ((TileEntityHopper) hopper).setTransferCooldown(8);
        } else if (hopper instanceof EntityMinecartHopper) {
            ((EntityMinecartHopper) hopper).setTransferTicker(4);
        }
        return true;
    }


    static boolean canExtractItemFromSlot(IInventory inventoryIn, ItemStack stack, int index, EnumFacing side) {
        return !(inventoryIn instanceof ISidedInventory) || ((ISidedInventory) inventoryIn).canExtractItem(index, stack, side);
    }

    default AxisAlignedBB getHopperLookupBoundingBox() {
        return getHopperLookupBoundingBox(this.getXPos(), this.getYPos(), this.getZPos());
    }

    default AxisAlignedBB getHopperLookupBoundingBox(double d0, double d1, double d2) {
        return new AxisAlignedBB(d0 - 0.5D, d1, d2 - 0.5D, d0 + 0.5D, d1 + 1.5D, d2 + 0.5D);
    }

    boolean canAcceptItems();

    boolean isOnCooldown();
}
