package ru.arixcompany.features.module.modules.combat.aura.utils;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import ru.arixcompany.features.module.modules.combat.aura.rotation.impl.RotationRepo;
import ru.arixcompany.utils.IMinecraft;

public class Rotation implements IMinecraft {

   public float yaw;
   public float pitch;
   public boolean isNormalized;

   public Rotation(float yaw, float pitch) {
      this(yaw, pitch, false);
   }

   public Rotation(float yaw, float pitch, boolean isNormalized) {
      this.yaw = yaw;
      this.pitch = pitch;
      this.isNormalized = isNormalized;
   }

   public Rotation(Entity entity) {
      this(entity.getYRot(), entity.getXRot(), false);
   }

   /**
    * Нормализует ротацию в соответствии с GCD сервера
    * Точно как в LiquidBounce
    */
   public Rotation normalize() {
      if (isNormalized) {
         return this;
      }

      float gcd = SensitivityUtil.getGCDValue();
      Rotation currentRotation = RotationRepo.getServerAngle();

      RotationDelta diff = currentRotation.rotationDeltaTo(this);
      
      float g1 = Math.round(diff.deltaYaw / gcd) * gcd;
      float g2 = Math.round(diff.deltaPitch / gcd) * gcd;

      float yaw = currentRotation.yaw + g1;
      float pitch = Mth.clamp(currentRotation.pitch + g2, -90f, 90f);

      return new Rotation(yaw, pitch, true);
   }

   /**
    * Вычисляет дельту ротации до целевой ротации
    */
   public RotationDelta rotationDeltaTo(Rotation other) {
      float deltaYaw = angleDifference(other.yaw, this.yaw);
      float deltaPitch = angleDifference(other.pitch, this.pitch);
      return new RotationDelta(deltaYaw, deltaPitch);
   }

   /**
    * Вычисляет угол между двумя ротациями
    */
   public float angleTo(Rotation other) {
      RotationDelta delta = rotationDeltaTo(other);
      return delta.length();
   }

   /**
    * Вычисляет разницу между двумя углами с учетом оборачивания
    */
   private static float angleDifference(float a, float b) {
      return Mth.wrapDegrees(a - b);
   }

   /**
    * Интерполирует между двумя ротациями (как в LiquidBounce)
    */
   public Rotation interpolateTo(Rotation other, float factor) {
      float lerpYaw = fma(factor, other.yaw - yaw, yaw);
      float lerpPitch = fma(factor, other.pitch - pitch, pitch);
      return new Rotation(lerpYaw, lerpPitch);
   }

   /**
    * Линейное движение к целевой ротации с ограничением скорости
    * Как в LiquidBounce
    */
   public Rotation towardsLinear(Rotation other, float horizontalFactor, float verticalFactor) {
      RotationDelta diff = rotationDeltaTo(other);
      float rotationDifference = diff.length();
      
      float straightLineYaw = Math.abs(diff.deltaYaw / rotationDifference) * horizontalFactor;
      float straightLinePitch = Math.abs(diff.deltaPitch / rotationDifference) * verticalFactor;

      return new Rotation(
         this.yaw + Mth.clamp(diff.deltaYaw, -straightLineYaw, straightLineYaw),
         Mth.clamp(this.pitch + Mth.clamp(diff.deltaPitch, -straightLinePitch, straightLinePitch), -90, 90)
      );
   }

   /**
    * Проверяет, приблизительно ли равны две ротации
    */
   public boolean approximatelyEquals(Rotation other, float tolerance) {
      return angleTo(other) <= tolerance;
   }

   /**
    * FMA (Fused Multiply-Add) для более точных вычислений
    */
   private static float fma(float a, float b, float c) {
      return a * b + c;
   }

   @Override
   public String toString() {
      return String.format("Rotation(yaw=%.2f, pitch=%.2f, normalized=%s)", yaw, pitch, isNormalized);
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Rotation)) return false;
      Rotation rotation = (Rotation) o;
      return Float.compare(rotation.yaw, yaw) == 0 &&
             Float.compare(rotation.pitch, pitch) == 0;
   }

   @Override
   public int hashCode() {
      return 31 * Float.hashCode(yaw) + Float.hashCode(pitch);
   }

   /**
    * Вспомогательный класс для хранения дельты ротации
    */
   public static class RotationDelta {
      public float deltaYaw;
      public float deltaPitch;

      public RotationDelta(float deltaYaw, float deltaPitch) {
         this.deltaYaw = deltaYaw;
         this.deltaPitch = deltaPitch;
      }

      public float length() {
         return (float) Math.hypot(Math.abs(deltaYaw), Math.abs(deltaPitch));
      }
   }
}
