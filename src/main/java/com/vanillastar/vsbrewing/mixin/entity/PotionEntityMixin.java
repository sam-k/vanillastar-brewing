package com.vanillastar.vsbrewing.mixin.entity;

import static com.vanillastar.vsbrewing.block.entity.PotionEntityHelperKt.FLASK_SPLASH_MULTIPLIER;
import static com.vanillastar.vsbrewing.item.ModItemsKt.MOD_ITEMS;
import static com.vanillastar.vsbrewing.networking.NetworkingHelperKt.sendWorldEvent;

import com.vanillastar.vsbrewing.networking.ThrownPotionPayload;
import net.minecraft.SharedConstants;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.passive.AxolotlEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potions;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PotionEntity.class)
public abstract class PotionEntityMixin extends ThrownItemEntity {
  @Unique
  private static double FLASK_SPLASH_SIZE = 4.0 * FLASK_SPLASH_MULTIPLIER;

  @Unique
  private static ThrownPotionPayload.@Nullable Type getPotionStackType(@NotNull ItemStack stack) {
    if (stack.isOf(Items.SPLASH_POTION)) {
      return ThrownPotionPayload.Type.SPLASH_POTION;
    }
    if (stack.isOf(Items.LINGERING_POTION)) {
      return ThrownPotionPayload.Type.LINGERING_POTION;
    }
    if (stack.isOf(MOD_ITEMS.splashPotionFlaskItem)) {
      return ThrownPotionPayload.Type.SPLASH_FLASK;
    }
    if (stack.isOf(MOD_ITEMS.lingeringPotionFlaskItem)) {
      return ThrownPotionPayload.Type.LINGERING_FLASK;
    }
    return null;
  }

  private PotionEntityMixin(EntityType<? extends ThrownItemEntity> entityType, World world) {
    super(entityType, world);
  }

  /**
   * This is mostly copied from `PotionEntity#applyWater`, except the splash's radius is increased.
   */
  @Unique
  private void applyWaterFlask() {
    Box box = this.getBoundingBox()
        .expand(/* x= */ FLASK_SPLASH_SIZE, /* y= */ 2.0, /* z= */ FLASK_SPLASH_SIZE);

    for (LivingEntity targetEntity : this.getWorld()
        .getEntitiesByClass(LivingEntity.class, box, PotionEntity.AFFECTED_BY_WATER)) {
      double squaredDist = this.squaredDistanceTo(targetEntity);
      if (squaredDist >= Math.pow(FLASK_SPLASH_SIZE, 2)) {
        continue;
      }

      if (targetEntity.hurtByWater()) {
        targetEntity.damage(
            this.getDamageSources().indirectMagic(/* source= */ this, this.getOwner()),
            /* amount= */ 1.0F);
      }
      if (targetEntity.isOnFire() && targetEntity.isAlive()) {
        targetEntity.extinguishWithSound();
      }
    }

    for (AxolotlEntity axolotlEntity :
        this.getWorld().getNonSpectatingEntities(AxolotlEntity.class, box)) {
      axolotlEntity.hydrateFromPotion();
    }
  }

  /**
   * This is mostly copied from `PotionEntity#applySplashPotion`, except the splash's radius is
   * increased.
   */
  @Unique
  private void applySplashFlask(
      PotionContentsComponent potionContents, @Nullable Entity hitEntity) {
    Box box = this.getBoundingBox()
        .expand(/* x= */ FLASK_SPLASH_SIZE, /* y= */ 2.0, /* z= */ FLASK_SPLASH_SIZE);
    Entity sourceEntity = this.getEffectCause();

    for (LivingEntity targetEntity :
        this.getWorld().getNonSpectatingEntities(LivingEntity.class, box)) {
      if (!targetEntity.isAffectedBySplashPotions()) {
        continue;
      }

      double squaredDist = this.squaredDistanceTo(targetEntity);
      if (squaredDist >= Math.pow(FLASK_SPLASH_SIZE, 2)) {
        continue;
      }
      double proximity =
          1.0 - (hitEntity != targetEntity ? Math.sqrt(squaredDist) / FLASK_SPLASH_SIZE : 0.0);

      for (StatusEffectInstance effect : potionContents.getEffects()) {
        RegistryEntry<StatusEffect> effectType = effect.getEffectType();
        if (effectType.value().isInstant()) {
          effectType
              .value()
              .applyInstantEffect(
                  /* source= */ this,
                  this.getOwner(),
                  targetEntity,
                  effect.getAmplifier(),
                  proximity);
        } else {
          StatusEffectInstance normalizedEffect = new StatusEffectInstance(
              effectType,
              effect.mapDuration(duration -> (int) (proximity * (double) duration + 0.5)),
              effect.getAmplifier(),
              effect.isAmbient(),
              effect.shouldShowParticles());
          if (!normalizedEffect.isDurationBelow(SharedConstants.TICKS_PER_SECOND)) {
            targetEntity.addStatusEffect(normalizedEffect, sourceEntity);
          }
        }
      }
    }
  }

  /**
   * This is mostly copied from `PotionEntity#applyLingeringPotion`, except the effect cloud's
   * radius is increased and its radius decay is decreased.
   */
  @Unique
  private void applyLingeringFlask(PotionContentsComponent potionContents) {
    AreaEffectCloudEntity cloudEntity =
        new AreaEffectCloudEntity(this.getWorld(), this.getX(), this.getY(), this.getZ());
    if (this.getOwner() instanceof LivingEntity sourceEntity) {
      cloudEntity.setOwner(sourceEntity);
    }
    cloudEntity.setDuration(cloudEntity.getDuration() * FLASK_SPLASH_MULTIPLIER);
    cloudEntity.setRadius(3.0F * (float) FLASK_SPLASH_MULTIPLIER);
    cloudEntity.setRadiusOnUse(-0.5F / (float) FLASK_SPLASH_MULTIPLIER);
    cloudEntity.setWaitTime(10);
    cloudEntity.setRadiusGrowth(-cloudEntity.getRadius() / (float) cloudEntity.getDuration());
    cloudEntity.setPotionContents(potionContents);

    this.getWorld().spawnEntity(cloudEntity);
  }

  /** This is mostly copied from `PotionEntity#onCollision`. */
  @Inject(
      method = "onCollision(Lnet/minecraft/util/hit/HitResult;)V",
      at = @At("HEAD"),
      cancellable = true)
  private void onFlaskCollision(HitResult hitResult, CallbackInfo ci) {
    ItemStack stack = this.getStack();
    ThrownPotionPayload.Type stackType = getPotionStackType(stack);
    if (stackType != ThrownPotionPayload.Type.SPLASH_FLASK
        && stackType != ThrownPotionPayload.Type.LINGERING_FLASK) {
      return;
    }

    super.onCollision(hitResult);
    if (this.getWorld().isClient) {
      ci.cancel();
      return;
    }

    PotionContentsComponent potionContents =
        stack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);
    if (potionContents.matches(Potions.WATER)) {
      this.applyWaterFlask();
    } else if (potionContents.hasEffects()) {
      if (stackType == ThrownPotionPayload.Type.SPLASH_FLASK) {
        this.applySplashFlask(
            potionContents,
            hitResult.getType() == HitResult.Type.ENTITY
                ? ((EntityHitResult) hitResult).getEntity()
                : null);
      } else {
        this.applyLingeringFlask(potionContents);
      }
    }

    sendWorldEvent(
        this.getWorld(),
        this.getBlockPos(),
        new ThrownPotionPayload(
            this.getBlockPos().asLong(),
            stackType.toString(),
            potionContents.potion().isPresent()
                && potionContents.potion().get().value().hasInstantEffect(),
            potionContents.getColor()));
    this.discard();

    ci.cancel();
  }
}
