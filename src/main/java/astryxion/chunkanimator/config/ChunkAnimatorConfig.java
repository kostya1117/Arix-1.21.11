package astryxion.chunkanimator.config;

/**
 * Fabric config replacement for NeoForge's {@code ModConfigSpec}.
 *
 * The rest of the mod code expects NeoForge-style {@code *.get()} accessors, so we keep
 * the same public field names and access pattern to preserve behavior 1:1.
 */
public final class ChunkAnimatorConfig {

	public static final ConfigValue<AnimationMode> MODE = new ConfigValue<>(AnimationMode.BELOW);
	public static final ConfigValue<EasingFunction> EASING_FUNCTION = new ConfigValue<>(EasingFunction.SINE);
	public static final ConfigValue<Integer> ANIMATION_DURATION = new ConfigValue<>(1000);
	public static final ConfigValue<Boolean> DISABLE_AROUND_PLAYER = new ConfigValue<>(false);

	private ChunkAnimatorConfig() {}

	public static void load() {
		final Model model = new Model();
		MODE.set(model.mode);
		EASING_FUNCTION.set(model.easingFunction);
		ANIMATION_DURATION.set(model.animationDuration);
		DISABLE_AROUND_PLAYER.set(model.disableAroundPlayer);
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

