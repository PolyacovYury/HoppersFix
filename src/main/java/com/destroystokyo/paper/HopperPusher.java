package com.destroystokyo.paper;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.polyacovyury.hoppersfix.interfaces.IPaperHopper;

public interface HopperPusher {

    default IPaperHopper findHopper() {
        BlockPos pos = new BlockPos(getX(), getY(), getZ());
        int startX = pos.getX() - 1;
        int endX = pos.getX() + 1;
        int startY = Math.max(0, pos.getY() - 1);
        int endY = Math.min(255, pos.getY() + 1);
        int startZ = pos.getZ() - 1;
        int endZ = pos.getZ() + 1;
        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                for (int z = startZ; z <= endZ; z++) {
                    Chunk chunk = getWorld().getChunk(new BlockPos(x, y, z));
                    TileEntity hopper = chunk.getTileEntity(new BlockPos(x, y, z), Chunk.EnumCreateEntityType.CHECK);
                    if (!(hopper instanceof IPaperHopper)) continue; // Avoid playing with the bounding boxes, if at all possible
                    AxisAlignedBB hopperBoundingBox = ((IPaperHopper)hopper).getHopperLookupBoundingBox();
                    /*
                     * Check if the entity's bounding box intersects with the hopper's lookup box.
                     * This operation doesn't work both ways!
                     * Make sure you check if the entity's box intersects the hopper's box, not vice versa!
                     */
                    AxisAlignedBB boundingBox = this.getEntityBoundingBox().shrink(0.1); // Imitate vanilla behavior
                    if (boundingBox.intersects(hopperBoundingBox)) {
                        return (IPaperHopper) hopper;
                    }
                }
            }
        }
        return null;
    }

    boolean acceptItem(IPaperHopper hopper);

    default boolean tryPutInHopper() {
        IPaperHopper hopper = findHopper();
        return hopper != null && hopper.canAcceptItems() && acceptItem(hopper);
    }

    AxisAlignedBB getEntityBoundingBox();

    World getWorld();

    double getX();

    double getY();

    double getZ();
}