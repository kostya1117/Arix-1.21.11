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
