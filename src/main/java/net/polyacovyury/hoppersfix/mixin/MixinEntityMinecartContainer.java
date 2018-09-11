package net.polyacovyury.hoppersfix.mixin;

import com.destroystokyo.paper.HopperPusher;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.item.EntityMinecartContainer;
import net.minecraft.world.ILockableContainer;
import net.minecraft.world.World;
import net.minecraft.world.storage.loot.ILootContainer;
import net.polyacovyury.hoppersfix.interfaces.IPaperHopper;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(EntityMinecartContainer.class)
public abstract class MixinEntityMinecartContainer extends EntityMinecart implements ILockableContainer, ILootContainer, HopperPusher {

    public MixinEntityMinecartContainer(World worldIn) {
        super(worldIn);
    }

    @Override
    public boolean acceptItem(IPaperHopper hopper) {
        return IPaperHopper.acceptItem(hopper, this);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        tryPutInHopper();
    }

    @Override
    public World getWorld() {
        return this.world;
    }

    @Override
    public double getX() {
        return this.posX;
    }

    @Override
    public double getY() {
        return this.posY;
    }

    @Override
    public double getZ() {
        return this.posZ;
    }
}
