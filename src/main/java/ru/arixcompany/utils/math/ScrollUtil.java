package ru.arixcompany.utils.math;

import com.mojang.blaze3d.platform.Window;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.Mth;
import org.lwjgl.opengl.GL11;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.render.ColorUtil;
import ru.arixcompany.utils.render.RenderUtils;

public class ScrollUtil implements IMinecraft {
   private static Window mw;
   @Setter
   @Getter
   private float target;
   @Setter
   @Getter
   private float scroll;
   @Getter
   private float max;
   @Setter
   @Getter
   private float speed = 8.0F;
   @Setter
   @Getter
   private boolean enabled;
   float barHeight;

   public static Window getWindow() {
      if (mw == null) {
         if (mc != null) {
            mw = mc.getWindow();
         }
      }

      return mw;
   }

   public ScrollUtil() {
      this.setEnabled(true);
   }

   public void update() {
      this.scroll = this.lerp(this.scroll, this.target, this.speed / 100.0F);
   }

   public void handleScroll(double scrollY) {
      if (this.enabled) {
         float wheel = (float) scrollY * (this.speed * 10.0F);
         float stretch = 0.0F;
         this.target = Math.min(Math.max(this.target + wheel / 2.0F, this.max - stretch), stretch);
      }
   }

   @SuppressWarnings("unchecked")
   public <T extends Number> T lerp(T input, T target, double step) {
      double start = input.doubleValue();
      double end = target.doubleValue();
      double result = start + step * (end - start);
      if (input instanceof Integer) {
         return (T) Integer.valueOf((int) Math.round(result));
      } else if (input instanceof Double) {
         return (T) Double.valueOf(result);
      } else if (input instanceof Float) {
         return (T) Float.valueOf((float) result);
      } else if (input instanceof Long) {
         return (T) Long.valueOf(Math.round(result));
      } else if (input instanceof Short) {
         return (T) Short.valueOf((short) Math.round(result));
      } else if (input instanceof Byte) {
         return (T) Byte.valueOf((byte) Math.round(result));
      } else {
         throw new IllegalArgumentException("Unsupported type: " + input.getClass().getSimpleName());
      }
   }

   public static void enable() {
      GL11.glEnable(3089);
   }

   public static void disable() {
      GL11.glDisable(3089);
   }

   public static void scissor(Window window, double x, double y, double width, double height) {
      if (x + width != x && y + height != y && !(x < 0.0) && !(y + height < 0.0)) {
         double scaleFactor = window.getGuiScale();
         GL11.glScissor(
               (int) Math.round(x * scaleFactor),
               (int) Math.round((window.getScreenHeight() - (y + height)) * scaleFactor),
               (int) Math.round(width * scaleFactor),
               (int) Math.round(height * scaleFactor));
      }
   }

   public void reset() {
      this.scroll = 0.0F;
      this.target = 0.0F;
   }

   public void setMax(float max, float height) {
      this.max = -max + height;
   }

   public void render(float x, float y, float width, float height, float alpha) {
      if (!(this.getMax() >= 0.0F)) {
         float percentage = this.getMax() != 0.0F ? this.getScroll() / this.getMax() : 0.0F;
         float targetBarHeight = height - this.getMax() / (this.getMax() - height) * height;
         this.barHeight = Mth.interpolate(targetBarHeight, this.barHeight, 0.9F);
         boolean allowed = this.barHeight < height && this.barHeight > 0.0F;
         if (allowed) {
            float scrollY = y + height * percentage - this.barHeight * percentage;
            int mainColor = ColorUtil.replAlpha(ColorUtil.getMainColor(1, 1),
                  (int) Mth.clamp(255.0F * alpha, 0.0F, 255.0F));
            int mainColor20 = ColorUtil.replAlpha(ColorUtil.getMainColor(1, 1),
                  (int) Mth.clamp(20.0F * alpha, 0.0F, 20.0F));
             RenderUtils.fillRoundRect(x, y, width, height,1, mainColor20);
             RenderUtils.fillRoundRect(x, scrollY, width, this.barHeight, 1.0F, mainColor);
         }
      }
   }

    public void setMax(float max) {
      this.max = max;
   }

}
