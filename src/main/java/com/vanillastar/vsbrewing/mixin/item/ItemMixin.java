package com.vanillastar.vsbrewing.mixin.item;

import static com.vanillastar.vsbrewing.item.BlockItemHelperKt.PLACEABLE_BOTTLE_BLOCK_ITEM_HELPER;
import static com.vanillastar.vsbrewing.tag.ModTagsKt.MOD_TAGS;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class ItemMixin {
  @Inject(
      method = "useOnBlock(Lnet/minecraft/item/ItemUsageContext;)Lnet/minecraft/util/ActionResult;",
      at = @At("HEAD"),
      cancellable = true)
  private void useOnBlockToPlace(
      @NotNull ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
    ItemStack stack = context.getStack();
    if (!stack.isIn(MOD_TAGS.placeableBottles)) {
      return;
    }

    PlayerEntity player = context.getPlayer();
    if (stack.isIn(MOD_TAGS.placeableBottlesWithSneaking)
        && (player == null || !player.isSneaking())) {
      // Place only if sneaking.
      return;
    }

    // Attempt to place bottle as a block.
    cir.setReturnValue(PLACEABLE_BOTTLE_BLOCK_ITEM_HELPER.place(context));
  }
}
