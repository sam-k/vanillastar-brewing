package com.vanillastar.vsbrewing.mixin.block;

import static com.vanillastar.vsbrewing.VSBrewingKt.MOD_ID;
import static com.vanillastar.vsbrewing.block.ModBlocksKt.MOD_BLOCKS;
import static com.vanillastar.vsbrewing.block.entity.BlockEntityHelperKt.BREWING_CAULDRON_BREW_TIME_TICKS;
import static com.vanillastar.vsbrewing.block.entity.BlockEntityHelperKt.BREWING_STAND_INVENTORY_FUEL_SLOT_INDEX;
import static com.vanillastar.vsbrewing.block.entity.BlockEntityHelperKt.BREWING_STAND_INVENTORY_INGREDIENT_SLOT_INDEX;
import static com.vanillastar.vsbrewing.block.entity.ModBlockEntitiesKt.MOD_BLOCK_ENTITIES;
import static com.vanillastar.vsbrewing.component.ModComponentsKt.MOD_COMPONENTS;
import static com.vanillastar.vsbrewing.item.ModItemsKt.MOD_ITEMS;
import static com.vanillastar.vsbrewing.tag.ModTagsKt.MOD_TAGS;

import com.vanillastar.vsbrewing.block.entity.PotionCauldronBlockEntity;
import com.vanillastar.vsbrewing.networking.BrewingCauldronPayload;
import com.vanillastar.vsbrewing.screen.BrewingCauldronScreenHandler;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.potion.Potions;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandBlockEntityMixin extends LockableContainerBlockEntity
    implements SidedInventory {
  @Unique
  private static boolean checkIsOnCauldron(@NotNull World world, @NotNull BlockPos pos) {
    return world.getBlockState(pos.down()).isIn(MOD_TAGS.brewableCauldrons);
  }

  @Unique
  private static ItemStack getCauldronPotionStack(@NotNull World world, @NotNull BlockPos pos) {
    BlockPos cauldronPos = pos.down();

    BlockState cauldronState = world.getBlockState(cauldronPos);
    if (cauldronState.isOf(Blocks.WATER_CAULDRON)) {
      ItemStack stack = new ItemStack(MOD_ITEMS.potionFlaskItem);
      stack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Potions.WATER));
      stack.set(
          MOD_COMPONENTS.potionFlaskRemainingUsesComponent,
          cauldronState.get(LeveledCauldronBlock.LEVEL));
      return stack;
    }

    PotionCauldronBlockEntity blockEntity = world
        .getBlockEntity(cauldronPos, MOD_BLOCK_ENTITIES.potionCauldronBlockEntityType)
        .orElse(null);
    if (blockEntity == null) {
      return ItemStack.EMPTY;
    }
    return blockEntity.getPotionStack();
  }

  @Unique
  private static boolean canCraftInBrewingCauldron(
      @NotNull World world, BlockPos pos, @NotNull DefaultedList<ItemStack> slots) {
    BrewingRecipeRegistry brewingRecipeRegistry = world.getBrewingRecipeRegistry();

    ItemStack ingredient = slots.get(BREWING_STAND_INVENTORY_INGREDIENT_SLOT_INDEX);
    if (ingredient.isEmpty() || !brewingRecipeRegistry.isValidIngredient(ingredient)) {
      return false;
    }

    return brewingRecipeRegistry.hasRecipe(getCauldronPotionStack(world, pos), ingredient);
  }

  @Unique
  private static void craftInBrewingCauldron(
      @NotNull World world, BlockPos pos, @NotNull DefaultedList<ItemStack> slots) {
    BrewingRecipeRegistry brewingRecipeRegistry = world.getBrewingRecipeRegistry();

    ItemStack ingredientStack = slots.get(BREWING_STAND_INVENTORY_INGREDIENT_SLOT_INDEX);
    ItemStack cauldronPotionStack = getCauldronPotionStack(world, pos);
    ItemStack resultingPotionStack =
        brewingRecipeRegistry.craft(ingredientStack, cauldronPotionStack);

    // Update potion cauldron, creating one in place of a water cauldron if necessary.
    BlockPos cauldronPos = pos.down();
    BlockState cauldronState = world.getBlockState(cauldronPos);
    BlockState newCauldronState = cauldronState;
    PotionCauldronBlockEntity potionCauldronBlockEntity = world
        .getBlockEntity(cauldronPos, MOD_BLOCK_ENTITIES.potionCauldronBlockEntityType)
        .orElse(null);
    if (cauldronState.isOf(Blocks.WATER_CAULDRON) && potionCauldronBlockEntity == null) {
      newCauldronState = MOD_BLOCKS
          .potionCauldronBlock
          .getDefaultState()
          .with(LeveledCauldronBlock.LEVEL, cauldronState.get(LeveledCauldronBlock.LEVEL));
      world.setBlockState(cauldronPos, newCauldronState, Block.NOTIFY_LISTENERS);
      potionCauldronBlockEntity = new PotionCauldronBlockEntity(cauldronPos, newCauldronState);
      world.addBlockEntity(potionCauldronBlockEntity);
    }
    if (potionCauldronBlockEntity == null) {
      return;
    }
    potionCauldronBlockEntity.setPotion(resultingPotionStack);
    markDirty(world, cauldronPos, newCauldronState);
    world.updateListeners(cauldronPos, cauldronState, newCauldronState, /* flags= */ 0);
    BrewingStandBlockEntity brewingStandBlockEntity =
        world.getBlockEntity(pos, BlockEntityType.BREWING_STAND).orElse(null);
    if (brewingStandBlockEntity != null) {
      for (ServerPlayerEntity player : PlayerLookup.tracking(brewingStandBlockEntity)) {
        ServerPlayNetworking.send(
            player,
            getBrewingCauldronPayload(
                cauldronPos, newCauldronState, potionCauldronBlockEntity, player));
      }
    }

    ingredientStack.decrement(1);
    Item ingredientRemainder = ingredientStack.getItem().getRecipeRemainder();
    if (ingredientRemainder != null) {
      ItemStack ingredientRemainderStack = new ItemStack(ingredientRemainder);
      if (ingredientRemainderStack.isEmpty()) {
        ingredientStack = ingredientRemainderStack;
      } else {
        ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), ingredientRemainderStack);
      }
    }

    slots.set(BREWING_STAND_INVENTORY_INGREDIENT_SLOT_INDEX, ingredientStack);
    world.syncWorldEvent(WorldEvents.BREWING_STAND_BREWS, pos, /* data= */ 0);
  }

  @Unique
  private static @NotNull BrewingCauldronPayload getBrewingCauldronPayload(
      @NotNull BlockPos pos,
      BlockState state,
      PotionCauldronBlockEntity blockEntity,
      ServerPlayerEntity player) {
    BlockPos cauldronPos = pos.down();
    if (blockEntity == null && state != null && state.isOf(Blocks.WATER_CAULDRON)) {
      blockEntity = new PotionCauldronBlockEntity(
          cauldronPos, state, new PotionContentsComponent(Potions.WATER));
    }
    NbtCompound potionNbt = player == null || blockEntity == null
        ? new NbtCompound()
        : blockEntity.createNbt(player.getWorld().getRegistryManager());
    return new BrewingCauldronPayload(
        pos.asLong(),
        state == null ? 0 : state.getOrEmpty(LeveledCauldronBlock.LEVEL).orElse(0),
        potionNbt);
  }

  @Inject(
      method =
          "tick(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/block/entity/BrewingStandBlockEntity;)V",
      at = @At("HEAD"),
      cancellable = true)
  private static void tickBrewingCauldron(
      @NotNull World world,
      @NotNull BlockPos pos,
      BlockState state,
      BrewingStandBlockEntity blockEntity,
      CallbackInfo ci) {
    if (!checkIsOnCauldron(world, pos)) {
      return;
    }

    // Refill fuel if empty.
    ItemStack fuel = blockEntity.inventory.get(BREWING_STAND_INVENTORY_FUEL_SLOT_INDEX);
    if (blockEntity.fuel <= 0 && fuel.isOf(Items.BLAZE_POWDER)) {
      blockEntity.fuel = BrewingStandBlockEntity.MAX_FUEL_USES;
      fuel.decrement(1);
      markDirty(world, pos, state);
    }

    boolean isCraftable = canCraftInBrewingCauldron(world, pos, blockEntity.inventory);
    ItemStack ingredient = blockEntity.inventory.get(BREWING_STAND_INVENTORY_INGREDIENT_SLOT_INDEX);
    if (blockEntity.brewTime > 0) {
      // Continue brewing, or halt if no longer valid.
      blockEntity.brewTime--;
      if (blockEntity.brewTime == 0 && isCraftable) {
        craftInBrewingCauldron(world, pos, blockEntity.inventory);
      } else if (!isCraftable || !ingredient.isOf(blockEntity.itemBrewing)) {
        blockEntity.brewTime = 0;
      }
      markDirty(world, pos, state);
    } else if (isCraftable && blockEntity.fuel > 0) {
      // Start crafting.
      blockEntity.fuel--;
      blockEntity.brewTime = BREWING_CAULDRON_BREW_TIME_TICKS;
      blockEntity.itemBrewing = ingredient.getItem();
      markDirty(world, pos, state);
    }

    ci.cancel();
  }

  private BrewingStandBlockEntityMixin(
      BlockEntityType<BrewingStandBlockEntity> blockEntityType,
      BlockPos blockPos,
      BlockState blockState) {
    super(blockEntityType, blockPos, blockState);
  }

  @Unique
  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean checkIsOnCauldron() {
    return this.world != null
        && this.world.getBlockState(this.pos.down()).isIn(MOD_TAGS.brewableCauldrons);
  }

  @Unique
  private @NotNull BrewingCauldronPayload getScreenOpeningData(
      @Nullable ServerPlayerEntity player) {
    BlockPos cauldronPos = this.pos.down();
    if (player == null) {
      return getBrewingCauldronPayload(cauldronPos, null, null, null);
    }
    return getBrewingCauldronPayload(
        cauldronPos,
        player.getWorld().getBlockState(cauldronPos),
        player
            .getWorld()
            .getBlockEntity(cauldronPos, MOD_BLOCK_ENTITIES.potionCauldronBlockEntityType)
            .orElse(null),
        player);
  }

  @Inject(
      method = "getContainerName()Lnet/minecraft/text/Text;",
      at = @At("HEAD"),
      cancellable = true)
  private void getBrewingCauldronContainerName(CallbackInfoReturnable<Text> cir) {
    if (!this.checkIsOnCauldron()) {
      return;
    }
    cir.setReturnValue(Text.translatable(String.format("container.%s.brewing_cauldron", MOD_ID)));
    cir.cancel();
  }

  @Inject(
      method =
          "createScreenHandler(ILnet/minecraft/entity/player/PlayerInventory;)Lnet/minecraft/screen/ScreenHandler;",
      at = @At("HEAD"),
      cancellable = true)
  private void createBrewingCauldronScreenHandler(
      int syncId, PlayerInventory playerInventory, CallbackInfoReturnable<ScreenHandler> cir) {
    if (!this.checkIsOnCauldron()) {
      return;
    }

    BrewingCauldronPayload payload;
    if (this.world != null && !this.world.isClient) {
      // Manually send screen opening data to the client.
      // This would be better suited with Fabric's `ExtendedScreenHandlerType`, but this block
      // entity chooses between two screen handlers. Fabric's API cannot discriminate such cases and
      // will throw incorrectly.
      ServerPlayerEntity player = (ServerPlayerEntity) playerInventory.player;
      payload = this.getScreenOpeningData(player);
      ServerPlayNetworking.send(player, payload);
    } else {
      payload = this.getScreenOpeningData(null);
    }

    cir.setReturnValue(new BrewingCauldronScreenHandler(
        syncId,
        playerInventory,
        payload,
        this,
        ((BrewingStandBlockEntity) (Object) this).propertyDelegate));
    cir.cancel();
  }
}
