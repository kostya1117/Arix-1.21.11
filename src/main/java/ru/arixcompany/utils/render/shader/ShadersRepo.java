package ru.arixcompany.utils.render.shader;

import lombok.Getter;
import ru.arixcompany.utils.render.shader.shaders.FractalFlameShader;
import ru.arixcompany.utils.render.shader.shaders.RoundRectShader;

import java.util.ArrayList;
import java.util.List;

public class ShadersRepo {
    @Getter
    private static final List<IShader> shaders = new ArrayList<>();

    public void register() {
        shaders.add(new RoundRectShader());
        shaders.add(new FractalFlameShader());
    }

    public void init() {
        register();

        for (IShader shader : shaders) {
            shader.init();
        }
    }
}
