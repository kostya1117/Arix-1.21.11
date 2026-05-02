package net.minecraft.client.resources.model;

import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;


public interface ResolvableModel {
    void resolveDependencies(ResolvableModel.Resolver p_376736_);

    
    interface Resolver {
        void markDependency(Identifier p_458968_);
    }
}
