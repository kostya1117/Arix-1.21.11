package net.optifine.entity.model;

import net.minecraft.client.model.AdultAndBabyModelPair;
import net.minecraft.client.model.Model;

public class ModelPairUtils {
    public static <T extends Model> AdultAndBabyModelPair<T> updateModel(AdultAndBabyModelPair<T> pairIn, boolean babyIn, T modelIn) {
        AdultAndBabyModelPair<T> adultandbabymodelpair;
        if (babyIn) {
            adultandbabymodelpair = new AdultAndBabyModelPair<>(pairIn.adultModel(), modelIn);
        } else {
            adultandbabymodelpair = new AdultAndBabyModelPair<>(modelIn, pairIn.babyModel());
        }

        return adultandbabymodelpair;
    }
}
