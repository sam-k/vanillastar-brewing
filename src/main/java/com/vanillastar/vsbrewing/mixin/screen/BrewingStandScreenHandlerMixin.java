package com.vanillastar.vsbrewing.mixin.screen;

import static com.vanillastar.vsbrewing.utils.IdentifierHelperKt.getModIdentifier;

import com.mojang.datafixers.util.Pair;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.BrewingStandScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

public class BrewingStandScreenHandlerMixin {
  @Mixin(BrewingStandScreenHandler.FuelSlot.class)
  public static class FuelSlotMixin extends Slot {
    @Unique
    private static final Identifier EMPTY_BLAZE_POWDER_SLOT_TEXTURE =
        getModIdentifier("item/empty_slot_blaze_powder");

    private FuelSlotMixin(Inventory inventory, int index, int x, int y) {
      super(inventory, index, x, y);
    }

    @Override
    public Pair<Identifier, Identifier> getBackgroundSprite() {
      return Pair.of(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, EMPTY_BLAZE_POWDER_SLOT_TEXTURE);
    }
  }

  @Mixin(BrewingStandScreenHandler.PotionSlot.class)
  public static class PotionSlotMixin extends Slot {
    @Unique
    private static final Identifier EMPTY_POTION_SLOT_TEXTURE =
        getModIdentifier("item/empty_slot_potion");

    private PotionSlotMixin(Inventory inventory, int index, int x, int y) {
      super(inventory, index, x, y);
    }

    @Override
    public Pair<Identifier, Identifier> getBackgroundSprite() {
      return Pair.of(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, EMPTY_POTION_SLOT_TEXTURE);
    }
  }
}
