package net.minecraft.client.renderer.entity.state;

import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;


public class GuardianRenderState extends LivingEntityRenderState {
    public float spikesAnimation;
    public float tailAnimation;
    public Vec3 eyePosition = Vec3.ZERO;
    public  Vec3 lookDirection;
    public  Vec3 lookAtPosition;
    public  Vec3 attackTargetPosition;
    public float attackTime;
    public float attackScale;
}
