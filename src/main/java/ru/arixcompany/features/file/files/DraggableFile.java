package ru.arixcompany.features.file.files;

import com.google.gson.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import ru.arixcompany.Arix;
import ru.arixcompany.features.file.ClientFile;
import ru.arixcompany.features.file.exception.FileLoadException;
import ru.arixcompany.features.file.exception.FileSaveException;
import ru.arixcompany.features.module.setting.Setting;
import ru.arixcompany.features.module.setting.implement.*;
import ru.arixcompany.ui.draggable.DraggableComponent;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DraggableFile extends ClientFile {

    Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    public DraggableFile() {
        super("draggables");
    }

    @Override
    public void saveToFile(File path) throws FileSaveException {
        saveToFile(path, getName() + ".json");
    }

    @Override
    public void saveToFile(File path, String fileName) throws FileSaveException {
        JsonObject root = new JsonObject();

        for (DraggableComponent comp : Arix.getInstance().getDraggableRepo().getDraggableComponents()) {
            JsonObject obj = new JsonObject();

            obj.addProperty("x", comp.getX());
            obj.addProperty("y", comp.getY());

            comp.settings().forEach(setting -> addSettingToJson(obj, setting));

            root.add(comp.getName().toLowerCase(), obj);
        }

        File file = new File(path, fileName);
        writeJson(root, file);
        super.saveToFile(path, fileName);
    }

    @Override
    public void loadFromFile(File path) throws FileLoadException {
        loadFromFile(path, getName() + ".json");
    }

    @Override
    public void loadFromFile(File path, String fileName) throws FileLoadException {
        File file = new File(path, fileName);
        JsonObject root = readJson(file);
        if (root == null) return;

        for (DraggableComponent comp : Arix.getInstance().getDraggableRepo().getDraggableComponents()) {
            String key = comp.getName().toLowerCase();
            JsonObject obj = root.getAsJsonObject(key);
            if (obj == null) continue;

            if (obj.has("x") && obj.has("y")) {
                float x = obj.get("x").getAsFloat();
                float y = obj.get("y").getAsFloat();
                comp.setPosition(x, y);
            }

            comp.settings().forEach(setting -> loadSettingFromJson(obj, setting));
        }

        super.loadFromFile(path, fileName);
    }

    private void addSettingToJson(JsonObject obj, Setting setting) {
        if (setting instanceof BooleanSetting bs) {
            obj.addProperty(setting.getName(), bs.isValue());
        }
        if (setting instanceof ValueSetting vs) {
            obj.addProperty(setting.getName(), vs.getValue());
        }
        if (setting instanceof BindSetting bind) {
            obj.addProperty(setting.getName(), bind.getKey());
        }
        if (setting instanceof SelectSetting ss) {
            obj.addProperty(setting.getName(), ss.getSelected());
        }
        if (setting instanceof ListSetting ls) {
            obj.addProperty(setting.getName(), String.join(",", ls.getSelected()));
        }
        if (setting instanceof TextSetting ts) {
            obj.addProperty(setting.getName(), ts.getText());
        }
        if (setting instanceof ColorSetting cs) {
            JsonObject colorObj = new JsonObject();
            colorObj.addProperty("current", cs.getCurrent());
            colorObj.addProperty("saturation", cs.getSaturation());
            colorObj.addProperty("brightness", cs.getBrightness());
            obj.add(setting.getName(), colorObj);
        }
    }

    private void loadSettingFromJson(JsonObject obj, Setting setting) {
        JsonElement el = obj.get(setting.getName());
        if (el == null || el.isJsonNull()) return;

        try {
            if (setting instanceof BooleanSetting bs) {
                bs.setValue(el.getAsBoolean());
            }
            if (setting instanceof ValueSetting vs) {
                vs.setValue(el.getAsFloat());
            }
            if (setting instanceof BindSetting bind) {
                bind.setKey(el.getAsInt());
            }
            if (setting instanceof SelectSetting ss) {
                ss.setSelected(el.getAsString());
            }
            if (setting instanceof ListSetting ls) {
                String str = el.getAsString();
                List<String> list = new ArrayList<>(Arrays.asList(str.split(",")));
                list.removeIf(s -> !ls.getList().contains(s));
                ls.setSelected(list);
            }
            if (setting instanceof TextSetting ts) {
                ts.setText(el.getAsString());
            }
            if (setting instanceof ColorSetting cs) {
                if (el.isJsonObject()) {
                    JsonObject co = el.getAsJsonObject();
                    if (co.has("current")) cs.setCurrent(co.get("current").getAsFloat());
                    if (co.has("saturation")) cs.setSaturation(co.get("saturation").getAsFloat());
                    if (co.has("brightness")) cs.setBrightness(co.get("brightness").getAsFloat());
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void writeJson(JsonObject json, File file) throws FileSaveException {
        try (Writer w = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            GSON.toJson(json, w);
        } catch (IOException e) {
            throw new FileSaveException("Failed to save draggables", e);
        }
    }

    private JsonObject readJson(File file) throws FileLoadException {
        if (!file.exists()) return null;
        try (Reader r = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            JsonElement el = JsonParser.parseReader(r);
            if (el == null || !el.isJsonObject()) return null;
            return el.getAsJsonObject();
        } catch (IOException e) {
            throw new FileLoadException("Failed to load draggables", e);
        } catch (JsonSyntaxException | JsonIOException e) {
            throw new FileLoadException("Failed to parse draggables JSON", e);
        }
    }
}