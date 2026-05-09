package ru.arixcompany.features.module.modules.combat.aura.rotation.impl;


import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec2;
import ru.arixcompany.utils.IMinecraft;

public class Rotation implements IMinecraft {
   public float yaw;
   public float pitch;

   public Rotation(Entity entity) {
      this.yaw = entity.getYRot();
      this.pitch = entity.getXRot();
   }

   public Rotation(float yawN, float pitchN) {
      this.yaw = yawN;
      this.pitch = pitchN;
   }
   public float getDelta(Rotation target) {
      float yawDelta = Mth.wrapDegrees(target.yaw - this.yaw);
      float pitchDelta = target.pitch - this.pitch;
      return (float) Math.hypot((double) Math.abs(yawDelta), (double) Math.abs(pitchDelta));
   }
   public Rotation adjustSensitivity() {
      double gcd = RotationRepo.getGCD();

      Rotation previousAngle = RotationRepo.getServerAngle();

      float adjustedYaw = adjustAxis(yaw, previousAngle.yaw, gcd);
      float adjustedPitch = adjustAxis(pitch, previousAngle.pitch, gcd);

      return new Rotation(adjustedYaw, Mth.clamp(adjustedPitch, -90f, 90f));
   }

   private float adjustAxis(float axisValue, float previousValue, double gcd) {
      float delta = Mth.wrapDegrees(axisValue - previousValue);
      return previousValue + Math.round(delta / gcd / 0.15F) * (float) gcd * 0.15F;
   }

   public static Vec2 camera() {
      return new Vec2(cameraYaw(), cameraPitch());
   }

   public static float cameraYaw() {
      return Mth.wrapDegrees(mc.gameRenderer.getMainCamera().yRot() + (mc.gameRenderer.getMainCamera().isDetached() ? 180 : 0));
   }

   public static float cameraPitch() {
      return (mc.gameRenderer.getMainCamera().isDetached() ? -1 : 1) * mc.gameRenderer.getMainCamera().xRot();
   }
}
