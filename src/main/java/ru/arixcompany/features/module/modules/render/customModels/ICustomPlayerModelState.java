package ru.arixcompany.features.module.modules.render.customModels;

public interface ICustomPlayerModelState {
    boolean hasCustomModel();

    String getCustomModel();

    void setCustomModel(boolean enabled, String model);
}
