package ru.arixcompany.features.module.modules.combat.aura.rotation.impl;

import net.minecraft.util.Mth;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.player.EventLook;
import ru.arixcompany.features.event.player.EventRotation;
import ru.arixcompany.features.module.modules.combat.aura.rotation.Component;

public class FreeLookUtil extends Component {
   public static boolean active;
   public static float freeYaw;
   public static float freePitch;

   @EventHandler
   public void onEvent(EventLook event) {
      if (active) {
         this.rotateTowards(event.getYaw(), event.getPitch());
         event.cancel();
      }
   }

   @EventHandler
   public void onEvent(EventRotation event) {
      if (active) {
         event.setYaw(freeYaw);
         event.setPitch(freePitch);
      } else {
         freeYaw = event.getYaw();
         freePitch = event.getPitch();
      }
   }

   private void rotateTowards(double targetYaw, double targetPitch) {
      freePitch = Mth.clamp((float)(freePitch + targetPitch * 0.15), -90.0F, 90.0F);
      freeYaw = (float)(freeYaw + targetYaw * 0.15);
   }
}
