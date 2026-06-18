package ru.arixcompany.utils.render.particle;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.util.function.Function;

final class ParticleRenderLayers {
	private static final RenderPipeline COLOR_PIPELINE = RenderPipelines.register(
			RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
					.withLocation(Identifier.fromNamespaceAndPath("arix", "world_particles_color"))
					.withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
					.withCull(false)
					.withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
					.withDepthWrite(false)
					.withBlend(BlendFunction.LIGHTNING)
					.build()
	);

	private static final RenderPipeline LINES_PIPELINE = RenderPipelines.register(
			RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
					.withLocation(Identifier.fromNamespaceAndPath("arix", "world_particles_lines"))
					.withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.DEBUG_LINES)
					.withCull(false)
					.withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
					.withDepthWrite(false)
					.withBlend(BlendFunction.LIGHTNING)
					.build()
	);

	private static final RenderPipeline GLOW_PIPELINE = RenderPipelines.register(
			RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
					.withLocation(Identifier.fromNamespaceAndPath("arix", "world_particles_glow"))
					.withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
					.withCull(false)
					.withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
					.withDepthWrite(false)
					.withBlend(BlendFunction.LIGHTNING)
					.withSampler("Sampler0")
					.build()
	);

	static final RenderType QUADS = RenderType.create(
			"arix" + "_world_particles_cube",
			RenderSetup.builder(COLOR_PIPELINE)
					.sortOnUpload()
					.bufferSize(2048)
					.createRenderSetup()
	);

	static final RenderType LINES = RenderType.create(
			"arix" + "_world_particles_lines",
			RenderSetup.builder(LINES_PIPELINE)
					.sortOnUpload()
					.bufferSize(2048)
					.createRenderSetup()
	);

	static final Function<Identifier, RenderType> GLOW = Util.memoize(texture -> {
		RenderSetup setup = RenderSetup.builder(GLOW_PIPELINE)
				.withTexture("Sampler0", texture)
				.sortOnUpload()
				.bufferSize(2048)
				.createRenderSetup();
		return RenderType.create("arix" + "_world_particles_glow", setup);
	});

	private ParticleRenderLayers() {
	}
}
