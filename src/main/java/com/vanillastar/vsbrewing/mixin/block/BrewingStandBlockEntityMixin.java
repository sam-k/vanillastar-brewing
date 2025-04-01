package com.vanillastar.vsbrewing.mixin.block;

import static com.vanillastar.vsbrewing.VSBrewingKt.MOD_ID;
import static com.vanillastar.vsbrewing.block.ModBlocksKt.MOD_BLOCKS;
import static com.vanillastar.vsbrewing.block.entity.BlockEntityHelperKt.BREWING_CAULDRON_BREW_TIME_TICKS;
import static com.vanillastar.vsbrewing.block.entity.ModBlockEntitiesKt.MOD_BLOCK_ENTITIES;
import static com.vanillastar.vsbrewing.component.ModComponentsKt.MOD_COMPONENTS;
import static com.vanillastar.vsbrewing.item.ModItemsKt.MOD_ITEMS;
import static com.vanillastar.vsbrewing.tag.ModTagsKt.MOD_TAGS;

import com.google.common.primitives.ImmutableIntArray;
import com.vanillastar.vsbrewing.block.entity.BrewingStandBlockEntityRenderData;
import com.vanillastar.vsbrewing.block.entity.PotionCauldronBlockEntity;
import com.vanillastar.vsbrewing.block.entity.PotionCauldronVariant;
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
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.potion.Potions;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.registry.RegistryWrapper;
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
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandBlockEntityMixin extends LockableContainerBlockEntity
    implements SidedInventory {
  @Shadow
  public DefaultedList<ItemStack> inventory;

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
          MOD_COMPONENTS.flaskRemainingUsesComponent,
          cauldronState.get(LeveledCauldronBlock.LEVEL));
      return stack;
    }

    PotionCauldronBlockEntity blockEntity = world
        .getBlockEntity(cauldronPos, MOD_BLOCK_ENTITIES.potionCauldronBlockEntityType)
        .orElse(null);
    if (blockEntity == null) {
      return ItemStack.EMPTY;
    }
    return blockEntity.getPotionStack(/* isFlask= */ true);
  }

  @Unique
  private static boolean canCraftInBrewingCauldron(
      @NotNull World world, BlockPos pos, @NotNull DefaultedList<ItemStack> slots) {
    BrewingRecipeRegistry brewingRecipeRegistry = world.getBrewingRecipeRegistry();

    ItemStack ingredient = slots.get(BrewingStandBlockEntity.INPUT_SLOT_INDEX);
    if (ingredient.isEmpty() || !brewingRecipeRegistry.isValidIngredient(ingredient)) {
      return false;
    }

    return brewingRecipeRegistry.hasRecipe(getCauldronPotionStack(world, pos), ingredient);
  }

  @Unique
  private static void craftInBrewingCauldron(
      @NotNull World world, BlockPos pos, @NotNull DefaultedList<ItemStack> slots) {
    BrewingRecipeRegistry brewingRecipeRegistry = world.getBrewingRecipeRegistry();

    ItemStack ingredientStack = slots.get(BrewingStandBlockEntity.INPUT_SLOT_INDEX);
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

    slots.set(BrewingStandBlockEntity.INPUT_SLOT_INDEX, ingredientStack);
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
          cauldronPos,
          state,
          new PotionContentsComponent(Potions.WATER),
          PotionCauldronVariant.NORMAL);
    }
    NbtCompound potionCauldronNbt = player == null || blockEntity == null
        ? new NbtCompound()
        : blockEntity.createNbt(player.getWorld().getRegistryManager());
    return new BrewingCauldronPayload(
        pos.asLong(),
        state != null ? state.getOrEmpty(LeveledCauldronBlock.LEVEL).orElse(0) : 0,
        potionCauldronNbt);
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
    ItemStack fuel = blockEntity.inventory.get(BrewingStandBlockEntity.FUEL_SLOT_INDEX);
    if (blockEntity.fuel <= 0 && fuel.isOf(Items.BLAZE_POWDER)) {
      blockEntity.fuel = BrewingStandBlockEntity.MAX_FUEL_USES;
      fuel.decrement(1);
      markDirty(world, pos, state);
    }

    boolean isCraftable = canCraftInBrewingCauldron(world, pos, blockEntity.inventory);
    ItemStack ingredient = blockEntity.inventory.get(BrewingStandBlockEntity.INPUT_SLOT_INDEX);
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
      return getBrewingCauldronPayload(
          cauldronPos, /* state= */ null, /* blockEntity= */ null, /* player= */ null);
    }
    World world = player.getWorld();
    return getBrewingCauldronPayload(
        cauldronPos,
        world.getBlockState(cauldronPos),
        world
            .getBlockEntity(cauldronPos, MOD_BLOCK_ENTITIES.potionCauldronBlockEntityType)
            .orElse(null),
        player);
  }

  @Override
  public Packet<ClientPlayPacketListener> toUpdatePacket() {
    return BlockEntityUpdateS2CPacket.create(this);
  }

  @Override
  public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
    return createNbt(registryLookup);
  }

  @Override
  public BrewingStandBlockEntityRenderData getRenderData() {
    if (this.checkIsOnCauldron()) {
      return new BrewingStandBlockEntityRenderData();
    }

    // The first three slots in `BrewingStandBlockEntity`'s `inventory` are hardcoded as the potion
    // slots.
    ImmutableIntArray.Builder colorsBuilder = ImmutableIntArray.builder(/* initialCapacity= */ 3);
    for (int i = 0; i < 3; i++) {
      ItemStack stack = this.inventory.get(i);
      colorsBuilder.add(
          stack.isEmpty()
              ? -1
              : stack
                  .getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT)
                  .getColor());
    }
    return new BrewingStandBlockEntityRenderData(colorsBuilder.build());
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
      payload = this.getScreenOpeningData(/* player= */ null);
    }

    cir.setReturnValue(new BrewingCauldronScreenHandler(
        syncId,
        playerInventory,
        payload,
        /* inventory= */ this,
        ((BrewingStandBlockEntity) (Object) this).propertyDelegate));
  }
}
