package ru.arixcompany.features.file.files;

import com.google.gson.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import ru.arixcompany.Arix;
import ru.arixcompany.ui.clickgui.Gui;
import ru.arixcompany.features.file.ClientFile;
import ru.arixcompany.features.file.exception.FileLoadException;
import ru.arixcompany.features.file.exception.FileSaveException;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.Theme;
import ru.arixcompany.features.module.setting.Setting;
import ru.arixcompany.features.module.setting.implement.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ModuleFile extends ClientFile {

    Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    public ModuleFile() {
        super("autoCfg");
    }

    @Override
    public void saveToFile(File path) throws FileSaveException {
        saveToFile(path, getName() + ".json");
    }

    @Override
    public void saveToFile(File path, String fileName) throws FileSaveException {
        JsonObject root = new JsonObject();

        JsonObject modulesObject = createJsonObjectFromModules();
        root.add("modules", modulesObject);

        JsonObject miscObject = createMiscObject();
        root.add("misc", miscObject);

        File file = new File(path, fileName);
        writeJsonToFile(root, file);
        super.saveToFile(path, fileName);
    }

    @Override
    public void loadFromFile(File path) throws FileLoadException {
        loadFromFile(path, getName() + ".json");
    }

    @Override
    public void loadFromFile(File path, String fileName) throws FileLoadException {
        File file = new File(path, fileName);
        JsonObject root = readJsonFromFile(file);

        if (root == null) return;

        if (root.has("modules") && root.get("modules").isJsonObject()) {
            updateModulesFromJsonObject(root.getAsJsonObject("modules"));
        } else {
            updateModulesFromJsonObject(root);
        }

        if (root.has("misc") && root.get("misc").isJsonObject()) {
            loadMiscObject(root.getAsJsonObject("misc"));
        }

        super.loadFromFile(path, fileName);
    }


    private JsonObject createJsonObjectFromModules() {
        JsonObject modulesObject = new JsonObject();

        for (Module module : Arix.getInstance().getModuleRepo().getModules()) {
            JsonObject moduleObject = new JsonObject();
            moduleObject.addProperty("bind", module.getBind());
            moduleObject.addProperty("state", module.isState());
            moduleObject.addProperty("bindMode", module.getBindMode().name());

            module.settings().forEach(setting -> addSettingToJsonObject(moduleObject, setting));

            modulesObject.add(module.getName().toLowerCase(), moduleObject);
        }

        return modulesObject;
    }

    private void addSettingToJsonObject(JsonObject moduleObject, Setting setting) {
        if (setting instanceof GroupSetting groupSetting) {
            JsonObject groupObj = new JsonObject();
            groupObj.addProperty("__open", groupSetting.isOpen());
            groupSetting.getChildren().forEach(child -> addSettingToJsonObject(groupObj, child));
            moduleObject.add(setting.getName(), groupObj);
            return;
        }
        if (setting instanceof BooleanSetting booleanSetting) {
            JsonObject boolObj = new JsonObject();
            boolObj.addProperty("value", booleanSetting.isValue());
            boolObj.addProperty("key", booleanSetting.getKey());
            moduleObject.add(setting.getName(), boolObj);
            return;
        }
        if (setting instanceof ValueSetting valueSetting) {
            moduleObject.addProperty(setting.getName(), valueSetting.getValue());
        }
        if (setting instanceof BindSetting bindSetting) {
            moduleObject.addProperty(setting.getName(), bindSetting.getKey());
        }
        if (setting instanceof SelectSetting selectSetting) {
            moduleObject.addProperty(setting.getName(), selectSetting.getSelected());
        }
        if (setting instanceof ListSetting listSetting) {
            List<String> selected = listSetting.getSelected();
            String selectedAsString = String.join(",", selected);
            moduleObject.addProperty(setting.getName(), selectedAsString);
        }
        if (setting instanceof TextSetting stringSetting) {
            moduleObject.addProperty(setting.getName(), stringSetting.getText());
        }
        if (setting instanceof ColorSetting colorSetting) {
            JsonObject colorObj = new JsonObject();
            colorObj.addProperty("current", colorSetting.getCurrent());
            colorObj.addProperty("saturation", colorSetting.getSaturation());
            colorObj.addProperty("brightness", colorSetting.getBrightness());
            moduleObject.add(setting.getName(), colorObj);
        }
    }

    private void updateModulesFromJsonObject(JsonObject modulesObject) {
        for (Module module : Arix.getInstance().getModuleRepo().getModules()) {
            String key = module.getName().toLowerCase();

            if (key.equals("misc")) continue;

            JsonObject moduleObject = modulesObject.getAsJsonObject(key);
            if (moduleObject == null) continue;

            if (moduleObject.has("bind") && moduleObject.has("state")) {
                module.setBind(moduleObject.get("bind").getAsInt());
                module.setState(moduleObject.get("state").getAsBoolean());
            }
            if (moduleObject.has("bindMode")) {
                try {
                    module.setBindMode(Module.BindMode.valueOf(moduleObject.get("bindMode").getAsString()));
                } catch (Exception ignored) {}
            }

            module.settings().forEach(setting -> updateSettingFromJsonObject(moduleObject, setting));
        }
    }

    private void updateSettingFromJsonObject(JsonObject moduleObject, Setting setting) {
        JsonElement settingElement = moduleObject.get(setting.getName());
        if (settingElement == null || settingElement.isJsonNull()) return;

        try {
            if (setting instanceof GroupSetting groupSetting) {
                if (!settingElement.isJsonObject()) return;
                JsonObject groupObj = settingElement.getAsJsonObject();
                if (groupObj.has("__open")) {
                    boolean open = groupObj.get("__open").getAsBoolean();
                    if (open != groupSetting.isOpen()) groupSetting.toggle();
                }
                groupSetting.getChildren().forEach(child -> updateSettingFromJsonObject(groupObj, child));
                return;
            }
            if (setting instanceof BooleanSetting booleanSetting) {
                if (settingElement.isJsonObject()) {
                    JsonObject boolObj = settingElement.getAsJsonObject();
                    if (boolObj.has("value")) {
                        booleanSetting.setValue(boolObj.get("value").getAsBoolean());
                    }
                    if (boolObj.has("key")) {
                        booleanSetting.setKey(boolObj.get("key").getAsInt());
                    }
                } else {
                    // Backward compatibility for old format
                    booleanSetting.setValue(settingElement.getAsBoolean());
                }
            }
            if (setting instanceof ValueSetting valueSetting) {
                valueSetting.setValue(settingElement.getAsFloat());
            }
            if (setting instanceof BindSetting bindSetting) {
                bindSetting.setKey(settingElement.getAsInt());
            }
            if (setting instanceof SelectSetting selectSetting) {
                selectSetting.setSelected(settingElement.getAsString());
            }
            if (setting instanceof ListSetting listSetting) {
                String asString = settingElement.getAsString();
                List<String> selectedList = new ArrayList<>(Arrays.asList(asString.split(",")));
                selectedList.removeIf(s -> !listSetting.getList().contains(s));
                listSetting.setSelected(selectedList);
            }
            if (setting instanceof TextSetting stringSetting) {
                stringSetting.setText(settingElement.getAsString());
            }
            if (setting instanceof ColorSetting colorSetting) {
                if (settingElement.isJsonObject()) {
                    JsonObject colorObj = settingElement.getAsJsonObject();
                    if (colorObj.has("current")) {
                        colorSetting.setCurrent(colorObj.get("current").getAsFloat());
                    }
                    if (colorObj.has("saturation")) {
                        colorSetting.setSaturation(colorObj.get("saturation").getAsFloat());
                    }
                    if (colorObj.has("brightness")) {
                        colorSetting.setBrightness(colorObj.get("brightness").getAsFloat());
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private JsonObject createMiscObject() {
        Arix main = Arix.getInstance();

        Theme theme = Gui.selectedTheme != null
                ? Gui.selectedTheme
                : main.getCurrentTheme();

        main.setCurrentTheme(theme);

        JsonObject miscObject = new JsonObject();
        miscObject.addProperty("Theme", theme.name());

        return miscObject;
    }

    private void loadMiscObject(JsonObject miscObject) {
        Theme theme = readEnum(miscObject, "Theme", Theme.class, Theme.PURPLE);

        Arix main = Arix.getInstance();
        main.setCurrentTheme(theme);

        Gui.selectedTheme = theme;
        Gui.preSelectedTheme = theme;
    }

    private void writeJsonToFile(JsonObject jsonObject, File file) throws FileSaveException {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            GSON.toJson(jsonObject, writer);
        } catch (IOException e) {
            throw new FileSaveException("Failed to save to file", e);
        }
    }

    private JsonObject readJsonFromFile(File file) throws FileLoadException {
        if (!file.exists()) return null;

        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (element == null || !element.isJsonObject()) return null;
            return element.getAsJsonObject();
        } catch (IOException e) {
            throw new FileLoadException("Failed to load from file", e);
        } catch (JsonSyntaxException | JsonIOException e) {
            throw new FileLoadException("Failed to parse JSON from file", e);
        }
    }

    private static <E extends Enum<E>> E readEnum(JsonObject obj, String key, Class<E> enumClass, E defaultValue) {
        if (obj == null || !obj.has(key)) return defaultValue;

        try {
            String value = obj.get(key).getAsString();
            return Enum.valueOf(enumClass, value);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }
}