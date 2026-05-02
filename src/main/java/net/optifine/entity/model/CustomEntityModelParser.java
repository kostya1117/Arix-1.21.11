package net.optifine.entity.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.optifine.Config;
import net.optifine.config.ConnectedParser;
import net.optifine.entity.model.anim.ModelUpdater;
import net.optifine.entity.model.anim.ModelVariableUpdater;
import net.optifine.player.PlayerItemParser;
import net.optifine.util.Json;

public class CustomEntityModelParser {
    public static final String ENTITY = "entity";
    public static final String TEXTURE = "texture";
    public static final String SHADOW_SIZE = "shadowSize";
    public static final String ITEM_TYPE = "type";
    public static final String ITEM_TEXTURE_SIZE = "textureSize";
    public static final String ITEM_USE_PLAYER_TEXTURE = "usePlayerTexture";
    public static final String ITEM_MODELS = "models";
    public static final String ITEM_ANIMATIONS = "animations";
    public static final String MODEL_ID = "id";
    public static final String MODEL_BASE_ID = "baseId";
    public static final String MODEL_MODEL = "model";
    public static final String MODEL_TYPE = "type";
    public static final String MODEL_PART = "part";
    public static final String MODEL_ATTACH = "attach";
    public static final String ENTITY_MODEL = "EntityModel";
    public static final String ENTITY_MODEL_PART = "EntityModelPart";

    public static CustomEntityRenderer parseEntityRender(JsonObject obj, String path) {
        ConnectedParser cp = new ConnectedParser("CustomEntityModels");
        String name = cp.parseName(path);
        String basePath = cp.parseBasePath(path);
        String texture = Json.getString(obj, "texture");
        int[] textureSize = Json.parseIntArray(obj.get("textureSize"), 2);
        float shadowSize = Json.getFloat(obj, "shadowSize", -1.0F);
        JsonArray models = (JsonArray)obj.get("models");
        checkNull(models, "Missing models");
        Map mapModelJsons = new HashMap();
        List listModels = new ArrayList();

        for(int i = 0; i < models.size(); ++i) {
            JsonObject elem = (JsonObject)models.get(i);
            processBaseId(elem, mapModelJsons);
            processExternalModel(elem, mapModelJsons, basePath);
            processId(elem, mapModelJsons);
            CustomModelRenderer mr = parseCustomModelRenderer(elem, textureSize, basePath);
            if (mr != null) {
                listModels.add(mr);
            }
        }

        CustomModelRenderer[] modelRenderers = (CustomModelRenderer[])listModels.toArray(new CustomModelRenderer[listModels.size()]);
        Identifier textureLocation = null;
        if (texture != null) {
            textureLocation = getResourceLocation(basePath, texture, ".png");
        }

        CustomEntityRenderer cer = new CustomEntityRenderer(name, basePath, textureLocation, modelRenderers, shadowSize);
        return cer;
    }


    private static void processBaseId(JsonObject elem, Map mapModelJsons) {
        String s = Json.getString(elem, "baseId");
        if (s != null) {
            JsonObject jsonobject = (JsonObject)mapModelJsons.get(s);
            if (jsonobject == null) {
                Config.warn("BaseID not found: " + s);
            } else {
                copyJsonElements(jsonobject, elem);
            }
        }
    }

    private static void processExternalModel(JsonObject elem, Map mapModelJsons, String basePath) {
        String s = Json.getString(elem, "model");
        if (s != null) {
            Identifier identifier = getResourceLocation(basePath, s, ".jpm");

            try {
                JsonObject jsonobject = loadJson(identifier);
                if (jsonobject == null) {
                    Config.warn("Model not found: " + identifier);
                    return;
                }

                copyJsonElements(jsonobject, elem);
            } catch (IOException ioexception) {
                Config.error(ioexception.getClass().getName() + ": " + ioexception.getMessage());
            } catch (JsonParseException jsonparseexception) {
                Config.error(jsonparseexception.getClass().getName() + ": " + jsonparseexception.getMessage());
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    private static void copyJsonElements(JsonObject objFrom, JsonObject objTo) {
        for (Entry<String, JsonElement> entry : objFrom.entrySet()) {
            if (!entry.getKey().equals("id") && !objTo.has(entry.getKey())) {
                objTo.add(entry.getKey(), entry.getValue());
            }
        }
    }

    public static Identifier getResourceLocation(String basePath, String path, String extension) {
        if (!path.endsWith(extension)) {
            path = path + extension;
        }

        if (!path.contains("/")) {
            path = basePath + "/" + path;
        } else if (path.startsWith("./")) {
            path = basePath + "/" + path.substring(2);
        } else if (path.startsWith("~/")) {
            path = "optifine/" + path.substring(2);
        }

        return new Identifier(path);
    }

    private static void processId(JsonObject elem, Map mapModelJsons) {
        String s = Json.getString(elem, "id");
        if (s != null) {
            if (s.length() < 1) {
                Config.warn("Empty model ID: " + s);
            } else if (mapModelJsons.containsKey(s)) {
                Config.warn("Duplicate model ID: " + s);
            } else {
                mapModelJsons.put(s, elem);
            }
        }
    }

    public static CustomModelRenderer parseCustomModelRenderer(JsonObject elem, int[] textureSize, String basePath) {
        String s = Json.getString(elem, "part");
        checkNull(s, "Model part not specified, missing 'part'.");
        boolean flag = Json.getBoolean(elem, "attach", false);
        Model model = new CustomEntityModel(ModelPart.makeRoot(), RenderTypes::entityCutoutNoCull);
        if (textureSize != null) {
            model.textureWidth = textureSize[0];
            model.textureHeight = textureSize[1];
        }

        ModelUpdater modelupdater = null;
        JsonArray jsonarray = (JsonArray)elem.get("animations");
        if (jsonarray != null) {
            List<ModelVariableUpdater> list = new ArrayList<>();

            for (int i = 0; i < jsonarray.size(); i++) {
                JsonObject jsonobject = (JsonObject)jsonarray.get(i);

                for (Entry<String, JsonElement> entry : jsonobject.entrySet()) {
                    String s1 = entry.getKey();
                    String s2 = entry.getValue().getAsString();
                    ModelVariableUpdater modelvariableupdater = new ModelVariableUpdater(s1, s2);
                    list.add(modelvariableupdater);
                }
            }

            if (list.size() > 0) {
                ModelVariableUpdater[] amodelvariableupdater = list.toArray(new ModelVariableUpdater[list.size()]);
                modelupdater = new ModelUpdater(amodelvariableupdater);
            }
        }

        ModelPart modelpart = PlayerItemParser.parseModelRenderer(elem, model, textureSize, basePath);
        return new CustomModelRenderer(s, flag, modelpart, modelupdater);
    }

    private static void checkNull(Object obj, String msg) {
        if (obj == null) {
            throw new JsonParseException(msg);
        }
    }

    public static JsonObject loadJson(Identifier location) throws IOException, JsonParseException {
        InputStream inputstream = Config.getResourceStream(location);
        if (inputstream == null) {
            return null;
        }

        String s = Config.readInputStream(inputstream, "ASCII");
        inputstream.close();
        JsonParser jsonparser = new JsonParser();
        return (JsonObject)jsonparser.parse(s);
    }
}
