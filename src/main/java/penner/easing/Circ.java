package penner.easing;

import net.minecraft.util.Mth;

public class Circ {
	
	public static float  easeIn(float t,float b , float c, float d) {
		return -c * (Mth.sqrt(1 - (t/=d)*t) - 1) + b;
	}
	
	public static float  easeOut(float t,float b , float c, float d) {
		return c * Mth.sqrt(1 - (t=t/d-1)*t) + b;
	}
	
	public static float  easeInOut(float t,float b , float c, float d) {
		if ((t/=d/2) < 1) return -c/2 * (Mth.sqrt(1 - t*t) - 1) + b;
		return c/2 * (Mth.sqrt(1 - (t-=2)*t) + 1) + b;
	}

}
