package penner.easing;

import net.minecraft.util.Mth;

public class Sine {
	
	public static float  easeIn(float t,float b , float c, float d) {
		return -c * Mth.cos(t/d * (Mth.PI/2)) + c + b;
	}
	
	public static float  easeOut(float t,float b , float c, float d) {
		return c * Mth.sin(t/d * (Mth.PI/2)) + b;
	}
	
	public static float  easeInOut(float t,float b , float c, float d) {
		return -c/2 * (Mth.cos(Mth.PI*t/d) - 1) + b;
	}
	
}
