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
            if (pushStack(inventoryIn, hopper, origItemStack, index, null, true)) {
                return true;  // returning true results in a setCooldown call in native code
            }
            cooldownHopper(hopper);
        }
        return false;
    }

    static boolean hopperPush(IHopper hopper, IInventory iinventory, EnumFacing enumfacing) {
        boolean foundItem = false;
        for (int i = 0; i < hopper.getSizeInventory(); ++i) {
            if (!hopper.getStackInSlot(i).isEmpty()) {
                foundItem = true;
                if (pushStack(hopper, iinventory, hopper.getStackInSlot(i), i, enumfacing, false)) {
                    return true;  // returning true results in a setCooldown call in native code
                }
            }
        }
        if (foundItem) { // Inventory was full - cooldown
            cooldownHopper(hopper);
        }
        return false;
    }

    // this function is courtesy of Orhideous
    static boolean pushStack(IInventory source, IInventory destination, ItemStack origItemStack, int index, EnumFacing direction, boolean pulling) {
        final int pulledPerTick = 1;
        final int origCount = origItemStack.getCount();

        // Operate only with copied stack next
        ItemStack itemStack = origItemStack.copy();
        itemStack.setCount(pulledPerTick);

        // Phase 1: try to insert single item from itemStack to hopper slot(s)
        // This will mutate copied itemStack!
        final ItemStack remainingItemStack = TileEntityHopper
                .putStackInInventoryAllSlots(source, destination, itemStack, direction);

        // this is false when there are no place for new item in hopper slot.
        // Since original item stack isn't mutated, let it be "as is" in that case
        if (remainingItemStack.getCount() == 0) {
            // Grab one item original itemStack
            origItemStack.setCount(origCount - pulledPerTick);
            // Temporary ignore related updates
            if (pulling) HoppersFix.IGNORE_TILE_UPDATES = true;
            // Replace stack in inventory slot with mutated one
            source.setInventorySlotContents(index, origItemStack);
            // Turn on tile updates back
            if (pulling) HoppersFix.IGNORE_TILE_UPDATES = false;
            // Let minecraft clean up things
            (pulling ? source : destination).markDirty();
            return true;
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
}
