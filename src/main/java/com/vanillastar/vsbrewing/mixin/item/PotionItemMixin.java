package com.vanillastar.vsbrewing.mixin.item;

import static com.vanillastar.vsbrewing.item.BlockItemHelperKt.PLACEABLE_BOTTLE_BLOCK_ITEM_HELPER;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;
import net.minecraft.util.ActionResult;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PotionItem.class)
public abstract class PotionItemMixin {
  @Inject(
      method = "useOnBlock(Lnet/minecraft/item/ItemUsageContext;)Lnet/minecraft/util/ActionResult;",
      at = @At("HEAD"),
      cancellable = true)
  private void useOnBlockToPlace(
      @NotNull ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
    ItemStack stack = context.getStack();
    if (!stack.isOf(Items.POTION)) {
      // Place only regular potions, not splash or lingering potions.
      return;
    }

    PlayerEntity player = context.getPlayer();
    if (player == null || !player.isSneaking()) {
      // Place only if sneaking, so as not to interfere with drinking the potion.
      return;
    }

    // Attempt to place the potion as a block.
    cir.setReturnValue(PLACEABLE_BOTTLE_BLOCK_ITEM_HELPER.place(context));
  }
}
