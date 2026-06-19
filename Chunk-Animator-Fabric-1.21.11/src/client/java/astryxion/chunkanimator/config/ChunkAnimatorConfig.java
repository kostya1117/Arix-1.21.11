package astryxion.chunkanimator.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Fabric config replacement for NeoForge's {@code ModConfigSpec}.
 *
 * The rest of the mod code expects NeoForge-style {@code *.get()} accessors, so we keep
 * the same public field names and access pattern to preserve behavior 1:1.
 */
public final class ChunkAnimatorConfig {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String FILE_NAME = "chunkanimator.json";

	public static final ConfigValue<AnimationMode> MODE = new ConfigValue<>(AnimationMode.BELOW);
	public static final ConfigValue<EasingFunction> EASING_FUNCTION = new ConfigValue<>(EasingFunction.SINE);
	public static final ConfigValue<Integer> ANIMATION_DURATION = new ConfigValue<>(1000);
	public static final ConfigValue<Boolean> DISABLE_AROUND_PLAYER = new ConfigValue<>(false);

	private ChunkAnimatorConfig() {}

	public static void load() {
		final Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);

		final Model model;
		if (Files.exists(path)) {
			model = read(path);
		} else {
			model = new Model();
			write(path, model);
		}

		MODE.set(model.mode);
		EASING_FUNCTION.set(model.easingFunction);
		ANIMATION_DURATION.set(model.animationDuration);
		DISABLE_AROUND_PLAYER.set(model.disableAroundPlayer);
	}

	private static Model read(Path path) {
		try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			final Model model = GSON.fromJson(reader, Model.class);
			return model == null ? new Model() : model.sanitize();
		} catch (Exception e) {
			// If config is malformed, fall back to defaults (and overwrite on next save).
			return new Model();
		}
	}

	@SuppressWarnings("SameParameterValue")
	private static void write(Path path, Model model) {
		try {
			Files.createDirectories(path.getParent());
		} catch (IOException ignored) {
		}

		try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
			GSON.toJson(model.sanitize(), writer);
		} catch (IOException ignored) {
		}
	}

	public static final class ConfigValue<T> {
		private volatile T value;

		private ConfigValue(T defaultValue) {
			this.value = defaultValue;
		}

		public T get() {
			return value;
		}

		private void set(T value) {
			this.value = value;
		}
	}

	private static final class Model {
		AnimationMode mode = AnimationMode.BELOW;
		EasingFunction easingFunction = EasingFunction.SINE;
		int animationDuration = 1000;
		boolean disableAroundPlayer = false;

		Model sanitize() {
			if (mode == null) mode = AnimationMode.BELOW;
			if (easingFunction == null) easingFunction = EasingFunction.SINE;
			if (animationDuration < 0) animationDuration = 0;
			return this;
		}
	}
}

