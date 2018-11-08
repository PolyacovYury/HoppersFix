package net.polyacovyury.hoppersfix.interfaces;

import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

public interface HopperPusher {
    boolean acceptItem(IPaperHopper hopper);

    default boolean tryPutInHopper() {
        boolean accepted = false;
        int posX = (int) Math.floor(((Entity) this).posX);  // Why'd I poke GC with BlockPos,
        int posY = (int) Math.floor(((Entity) this).posY);  // If I just can be more verbose?
        int posZ = (int) Math.floor(((Entity) this).posZ);  // and yes, this is what it does.
        int startX = posX - 1;
        int endX = posX + 1;
        int startY = Math.max(0, posY - 1);
        int endY = Math.min(255, posY); // original code checked for a block above the entity. WTF?
        int startZ = posZ - 1;
        int endZ = posZ + 1;
        BlockPos.PooledMutableBlockPos adjacentPos = BlockPos.PooledMutableBlockPos.retain();
        for (int y = startY; y <= endY; y++) {  // since it's more probable, that the hopper is below - less lag
            for (int x = startX; x <= endX; x++) {
                for (int z = startZ; z <= endZ; z++) {
                    adjacentPos.setPos(x, y, z);
                    Chunk chunk = ((Entity) this).world.getChunk(adjacentPos);
                    TileEntity hopper = chunk.getTileEntity(adjacentPos, Chunk.EnumCreateEntityType.CHECK);
                    if (!(hopper instanceof IPaperHopper))
                        continue; // Avoid playing with the bounding boxes, if at all possible
                    IPaperHopper pHopper = (IPaperHopper) hopper;
                    AxisAlignedBB hopperBoundingBox = pHopper.getHopperLookupBoundingBox();
                    AxisAlignedBB boundingBox = ((Entity) this).getEntityBoundingBox();
                    if (boundingBox.intersects(hopperBoundingBox)) {
                        accepted |= pHopper.canAcceptItems() && acceptItem(pHopper);
                    }
                }
            }
        }
        adjacentPos.release();
        return accepted;
    }
}