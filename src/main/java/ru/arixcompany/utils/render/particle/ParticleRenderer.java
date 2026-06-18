package ru.arixcompany.utils.render.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

final class ParticleRenderer {

	static void drawCube(VertexConsumer buffer, Matrix4f matrix, int color, float size) {
		float half = size / 2.0F;
		int r = color >> 16 & 0xFF;
		int g = color >> 8 & 0xFF;
		int b = color & 0xFF;
		int a = color >> 24 & 0xFF;

		buffer.addVertex(matrix, -half, half, -half).setColor(r, g, b, a);
		buffer.addVertex(matrix, -half, half, half).setColor(r, g, b, a);
		buffer.addVertex(matrix, half, half, half).setColor(r, g, b, a);
		buffer.addVertex(matrix, half, half, -half).setColor(r, g, b, a);

		buffer.addVertex(matrix, -half, -half, -half).setColor(r, g, b, a);
		buffer.addVertex(matrix, half, -half, -half).setColor(r, g, b, a);
		buffer.addVertex(matrix, half, -half, half).setColor(r, g, b, a);
		buffer.addVertex(matrix, -half, -half, half).setColor(r, g, b, a);

		buffer.addVertex(matrix, -half, half, half).setColor(r, g, b, a);
		buffer.addVertex(matrix, -half, -half, half).setColor(r, g, b, a);
		buffer.addVertex(matrix, half, -half, half).setColor(r, g, b, a);
		buffer.addVertex(matrix, half, half, half).setColor(r, g, b, a);

		buffer.addVertex(matrix, -half, half, -half).setColor(r, g, b, a);
		buffer.addVertex(matrix, half, half, -half).setColor(r, g, b, a);
		buffer.addVertex(matrix, half, -half, -half).setColor(r, g, b, a);
		buffer.addVertex(matrix, -half, -half, -half).setColor(r, g, b, a);

		buffer.addVertex(matrix, -half, half, -half).setColor(r, g, b, a);
		buffer.addVertex(matrix, -half, -half, -half).setColor(r, g, b, a);
		buffer.addVertex(matrix, -half, -half, half).setColor(r, g, b, a);
		buffer.addVertex(matrix, -half, half, half).setColor(r, g, b, a);

		buffer.addVertex(matrix, half, half, -half).setColor(r, g, b, a);
		buffer.addVertex(matrix, half, half, half).setColor(r, g, b, a);
		buffer.addVertex(matrix, half, -half, half).setColor(r, g, b, a);
		buffer.addVertex(matrix, half, -half, -half).setColor(r, g, b, a);
	}

	static void drawLines(VertexConsumer buffer, Matrix4f matrix, int color, float size) {
		float half = size / 2.0F;
		int r = color >> 16 & 0xFF;
		int g = color >> 8 & 0xFF;
		int b = color & 0xFF;
		int a = color >> 24 & 0xFF;

		line(buffer, matrix, -half, -half, -half, half, -half, -half, r, g, b, a);
		line(buffer, matrix, half, -half, -half, half, -half, half, r, g, b, a);
		line(buffer, matrix, half, -half, half, -half, -half, half, r, g, b, a);
		line(buffer, matrix, -half, -half, half, -half, -half, -half, r, g, b, a);
		line(buffer, matrix, -half, half, -half, half, half, -half, r, g, b, a);
		line(buffer, matrix, half, half, -half, half, half, half, r, g, b, a);
		line(buffer, matrix, half, half, half, -half, half, half, r, g, b, a);
		line(buffer, matrix, -half, half, half, -half, half, -half, r, g, b, a);
		line(buffer, matrix, -half, -half, -half, -half, half, -half, r, g, b, a);
		line(buffer, matrix, half, -half, -half, half, half, -half, r, g, b, a);
		line(buffer, matrix, half, -half, half, half, half, half, r, g, b, a);
		line(buffer, matrix, -half, -half, half, -half, half, half, r, g, b, a);
	}

	private static void line(VertexConsumer buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, int r, int g, int b, int a) {
		buffer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a);
		buffer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a);
	}

	static void drawGlow(VertexConsumer buffer, Matrix4f matrix, int color, int alpha, float size) {
		int r = color >> 16 & 0xFF;
		int g = color >> 8 & 0xFF;
		int b = color & 0xFF;
		float half = size / 2.0F;

		buffer.addVertex(matrix, -half, -half, 0).setUv(0, 0).setColor(r, g, b, alpha);
		buffer.addVertex(matrix, -half, half, 0).setUv(0, 1).setColor(r, g, b, alpha);
		buffer.addVertex(matrix, half, half, 0).setUv(1, 1).setColor(r, g, b, alpha);
		buffer.addVertex(matrix, half, -half, 0).setUv(1, 0).setColor(r, g, b, alpha);
	}
}
