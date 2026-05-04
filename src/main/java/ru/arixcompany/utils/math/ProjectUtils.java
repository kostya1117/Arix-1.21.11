package ru.arixcompany.utils.math;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Camera;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import ru.arixcompany.utils.IMinecraft;

@UtilityClass
public class ProjectUtils implements IMinecraft {
    public static final Matrix4f lastProjMat = new Matrix4f();
    public static final Matrix4f lastModMat = new Matrix4f();
    public static final Matrix4f lastWorldSpaceMatrix = new Matrix4f();

    public static Vector3d worldSpaceToScreenSpace(Vector3d pos) {
        Camera camera = mc.getEntityRenderDispatcher().camera;
        int displayHeight = mc.getWindow().getHeight();
        int[] viewport = new int[4];
        GL11.glGetIntegerv(2978, viewport);
        Vector3f target = new Vector3f();
        double deltaX = pos.x - camera.position().x;
        double deltaY = pos.y - camera.position().y;
        double deltaZ = pos.z - camera.position().z;
        Vector4f transformedCoordinates = new Vector4f((float)deltaX, (float)deltaY, (float)deltaZ, 1.0F).mul(lastWorldSpaceMatrix);
        Matrix4f matrixProj = new Matrix4f(lastProjMat);
        Matrix4f matrixModel = new Matrix4f(lastModMat);
        matrixProj.mul(matrixModel).project(transformedCoordinates.x(), transformedCoordinates.y(), transformedCoordinates.z(), viewport, target);
        return new Vector3d(target.x / getScaleFactor(), (displayHeight - target.y) / getScaleFactor(), target.z);
    }

    public static double getScaleFactor() {
        return mc.getWindow().getGuiScale();
    }
}
