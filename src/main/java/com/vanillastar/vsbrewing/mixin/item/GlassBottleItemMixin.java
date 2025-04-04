package com.vanillastar.vsbrewing.mixin.item;

import static com.vanillastar.vsbrewing.item.FlaskItemHelperKt.fillFromFlaskBlock;

import net.minecraft.item.GlassBottleItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(GlassBottleItem.class)
public abstract class GlassBottleItemMixin extends Item {
  private GlassBottleItemMixin(Settings settings) {
    super(settings);
  }

  @Override
  public ActionResult useOnBlock(ItemUsageContext context) {
    return fillFromFlaskBlock(context, /* isFlaskItem= */ false, () -> super.useOnBlock(context));
  }
}
