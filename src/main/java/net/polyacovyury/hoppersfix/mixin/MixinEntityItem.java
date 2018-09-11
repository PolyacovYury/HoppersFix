package net.polyacovyury.hoppersfix.mixin;

import com.destroystokyo.paper.HopperPusher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.world.World;
import net.polyacovyury.hoppersfix.interfaces.IPaperHopper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityItem.class)
public abstract class MixinEntityItem extends Entity implements HopperPusher {
    public MixinEntityItem(World worldIn) {
        super(worldIn);
    }

    @Override
    public boolean acceptItem(IPaperHopper hopper) {
        return TileEntityHopper.putDropInInventoryAllSlots(null, hopper, (EntityItem)(Object)this);
    }

    @Override
    public World getWorld() {
        return this.world;
    }

    @Redirect(method="onUpdate()V", at=@At(value="RETURN", target="Lnet/minecraft/entity/Entity;onUpdate()V"))
    private void redirectOnUpdate(CallbackInfo info) {
        if (tryPutInHopper()) info.cancel();
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
