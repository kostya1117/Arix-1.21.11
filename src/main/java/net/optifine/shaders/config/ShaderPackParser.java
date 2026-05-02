package net.optifine.shaders.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.optifine.Config;
import net.optifine.expr.ExpressionFloatArrayCached;
import net.optifine.expr.ExpressionFloatCached;
import net.optifine.expr.ExpressionParser;
import net.optifine.expr.ExpressionType;
import net.optifine.expr.IExpression;
import net.optifine.expr.IExpressionBool;
import net.optifine.expr.IExpressionFloat;
import net.optifine.expr.IExpressionFloatArray;
import net.optifine.expr.ParseException;
import net.optifine.render.GlAlphaState;
import net.optifine.render.GlBlendState;
import net.optifine.shaders.IShaderPack;
import net.optifine.shaders.Program;
import net.optifine.shaders.SMCLog;
import net.optifine.shaders.ShaderUtils;
import net.optifine.shaders.Shaders;
import net.optifine.shaders.ShadersCompatibility;
import net.optifine.shaders.uniform.CustomUniform;
import net.optifine.shaders.uniform.CustomUniforms;
import net.optifine.shaders.uniform.ShaderExpressionResolver;
import net.optifine.shaders.uniform.UniformType;
import net.optifine.util.DynamicDimension;
import net.optifine.util.LineBuffer;
import net.optifine.util.StrUtils;

public class ShaderPackParser {
    public static final Pattern PATTERN_VERSION = Pattern.compile("^\\s*#version\\s+(\\d+).*$");
    public static final Pattern PATTERN_INCLUDE = Pattern.compile("^\\s*#include\\s+\"([A-Za-z0-9_/\\.]+)\".*$");
    private static final Set<String> setConstNames = makeSetConstNames();
    private static final Map<String, Integer> mapAlphaFuncs = makeMapAlphaFuncs();
    private static final Map<String, Integer> mapBlendFactors = makeMapBlendFactors();

    public static ShaderOption[] parseShaderPackOptions(IShaderPack shaderPack, String[] programNames, List<Integer> listDimensions) {
        if (shaderPack == null) {
            return new ShaderOption[0];
        }

        Map<String, ShaderOption> map = new HashMap<>();
        collectShaderOptions(shaderPack, "/shaders", programNames, map);

        for (int i : listDimensions) {
            String s = "/shaders/world" + i;
            collectShaderOptions(shaderPack, s, programNames, map);
        }

        Collection<ShaderOption> collection = map.values();
        ShaderOption[] ashaderoption = collection.toArray(new ShaderOption[collection.size()]);
        Comparator<ShaderOption> comparator = new Comparator<ShaderOption>() {
            public int compare(ShaderOption o1, ShaderOption o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        };
        Arrays.sort(ashaderoption, comparator);
        return ashaderoption;
    }

    private static void collectShaderOptions(IShaderPack shaderPack, String dir, String[] programNames, Map<String, ShaderOption> mapOptions) {
        for (int i = 0; i < programNames.length; i++) {
            String s = programNames[i];
            if (!s.equals("")) {
                String s1 = dir + "/" + s + ".csh";
                String s2 = dir + "/" + s + ".vsh";
                String s3 = dir + "/" + s + ".gsh";
                String s4 = dir + "/" + s + ".fsh";
                collectShaderOptions(shaderPack, s1, mapOptions);
                collectShaderOptions(shaderPack, s2, mapOptions);
                collectShaderOptions(shaderPack, s3, mapOptions);
                collectShaderOptions(shaderPack, s4, mapOptions);
            }
        }
    }

    private static void collectShaderOptions(IShaderPack sp, String path, Map<String, ShaderOption> mapOptions) {
        String[] astring = getLines(sp, path);

        for (int i = 0; i < astring.length; i++) {
            String s = astring[i];
            ShaderOption shaderoption = getShaderOption(s, path);
            if (shaderoption != null && !shaderoption.getName().startsWith(ShaderMacros.getPrefixMacro())) {
                String s1 = shaderoption.getName();
                ShaderOption shaderoption1 = mapOptions.get(s1);
                if (shaderoption1 != null) {
                    if (!Config.equals(shaderoption1.getValueDefault(), shaderoption.getValueDefault())) {
                        if (shaderoption1.isEnabled()) {
                            Config.warn("Ambiguous shader option: " + shaderoption.getName());
                            Config.warn(" - in " + Config.arrayToString(shaderoption1.getPaths()) + ": " + shaderoption1.getValueDefault());
                            Config.warn(" - in " + Config.arrayToString(shaderoption.getPaths()) + ": " + shaderoption.getValueDefault());
                        }

                        shaderoption1.setEnabled(false);
                    }

                    if (shaderoption1.getDescription() == null || shaderoption1.getDescription().length() <= 0) {
                        shaderoption1.setDescription(shaderoption.getDescription());
                    }

                    shaderoption1.addPaths(shaderoption.getPaths());
                } else if (!shaderoption.checkUsed() || isOptionUsed(shaderoption, astring)) {
                    mapOptions.put(s1, shaderoption);
                }
            }
        }
    }

    private static boolean isOptionUsed(ShaderOption so, String[] lines) {
        for (int i = 0; i < lines.length; i++) {
            String s = lines[i];
            if (so.isUsedInLine(s)) {
                return true;
            }
        }

        return false;
    }

    private static String[] getLines(IShaderPack sp, String path) {
        try {
            List<String> list = new ArrayList<>();
            LineBuffer linebuffer = loadFile(path, sp, 0, list, 0);
            return linebuffer == null ? new String[0] : linebuffer.getLines();
        } catch (IOException ioexception) {
            Config.dbg(ioexception.getClass().getName() + ": " + ioexception.getMessage());
            return new String[0];
        }
    }

    private static ShaderOption getShaderOption(String line, String path) {
        ShaderOption shaderoption = null;
        if (shaderoption == null) {
            shaderoption = ShaderOptionSwitch.parseOption(line, path);
        }

        if (shaderoption == null) {
            shaderoption = ShaderOptionVariable.parseOption(line, path);
        }

        if (shaderoption != null) {
            return shaderoption;
        }

        if (shaderoption == null) {
            shaderoption = ShaderOptionSwitchConst.parseOption(line, path);
        }

        if (shaderoption == null) {
            shaderoption = ShaderOptionVariableConst.parseOption(line, path);
        }

        return shaderoption != null && setConstNames.contains(shaderoption.getName()) ? shaderoption : null;
    }

    private static Set<String> makeSetConstNames() {
        Set<String> set = new HashSet<>();
        set.add("shadowMapResolution");
        set.add("shadowMapFov");
        set.add("shadowDistance");
        set.add("shadowDistanceRenderMul");
        set.add("shadowIntervalSize");
        set.add("generateShadowMipmap");
        set.add("generateShadowColorMipmap");
        set.add("shadowHardwareFiltering");
        set.add("shadowHardwareFiltering0");
        set.add("shadowHardwareFiltering1");
        set.add("shadowtex0Mipmap");
        set.add("shadowtexMipmap");
        set.add("shadowtex1Mipmap");
        set.add("shadowcolor0Mipmap");
        set.add("shadowColor0Mipmap");
        set.add("shadowcolor1Mipmap");
        set.add("shadowColor1Mipmap");
        set.add("shadowtex0Nearest");
        set.add("shadowtexNearest");
        set.add("shadow0MinMagNearest");
        set.add("shadowtex1Nearest");
        set.add("shadow1MinMagNearest");
        set.add("shadowcolor0Nearest");
        set.add("shadowColor0Nearest");
        set.add("shadowColor0MinMagNearest");
        set.add("shadowcolor1Nearest");
        set.add("shadowColor1Nearest");
        set.add("shadowColor1MinMagNearest");
        set.add("wetnessHalflife");
        set.add("drynessHalflife");
        set.add("eyeBrightnessHalflife");
        set.add("centerDepthHalflife");
        set.add("sunPathRotation");
        set.add("ambientOcclusionLevel");
        set.add("superSamplingLevel");
        set.add("noiseTextureResolution");
        return set;
    }

    public static ShaderProfile[] parseProfiles(Properties props, ShaderOption[] shaderOptions) {
        String PREFIX_PROFILE = "profile.";
        List<ShaderProfile> list = new ArrayList();
        Set keys = props.keySet();

        for (Object o : keys) {
            String key = (String) o;
            if (key.startsWith(PREFIX_PROFILE)) {
                String name = key.substring(PREFIX_PROFILE.length());
                props.getProperty(key);
                Set<String> parsedProfiles = new HashSet();
                ShaderProfile p = parseProfile(name, props, parsedProfiles, shaderOptions);
                if (p != null) {
                    list.add(p);
                }
            }
        }

        if (list.size() <= 0) {
            return null;
        } else {
            ShaderProfile[] profs = list.toArray(new ShaderProfile[list.size()]);
            return profs;
        }
    }

    public static Map<String, IExpressionBool> parseProgramConditions(Properties props, ShaderOption[] shaderOptions) {
        String PREFIX_PROGRAM = "program.";
        Pattern pattern = Pattern.compile("program\\.([^.]+)\\.enabled");
        Map<String, IExpressionBool> map = new HashMap();
        Set keys = props.keySet();

        for (Object o : keys) {
            String key = (String) o;
            Matcher matcher = pattern.matcher(key);
            if (matcher.matches()) {
                String name = matcher.group(1);
                String val = props.getProperty(key).trim();
                IExpressionBool expr = parseOptionExpression(val, shaderOptions);
                if (expr == null) {
                    SMCLog.severe("Error parsing program condition: " + key);
                } else {
                    map.put(name, expr);
                }
            }
        }

        return map;
    }

    private static IExpressionBool parseOptionExpression(String val, ShaderOption[] shaderOptions) {
        try {
            ShaderOptionResolver shaderoptionresolver = new ShaderOptionResolver(shaderOptions);
            ExpressionParser expressionparser = new ExpressionParser(shaderoptionresolver);
            return expressionparser.parseBool(val);
        } catch (ParseException parseexception) {
            SMCLog.warning(parseexception.getClass().getName() + ": " + parseexception.getMessage());
            return null;
        }
    }

    public static Set<String> parseOptionSliders(Properties props, ShaderOption[] shaderOptions) {
        Set<String> set = new HashSet<>();
        String s = props.getProperty("sliders");
        if (s == null) {
            return set;
        }

        String[] astring = Config.tokenize(s, " ");

        for (int i = 0; i < astring.length; i++) {
            String s1 = astring[i];
            ShaderOption shaderoption = ShaderUtils.getShaderOption(s1, shaderOptions);
            if (shaderoption == null) {
                Config.warn("Invalid shader option: " + s1);
            } else {
                set.add(s1);
            }
        }

        return set;
    }

    private static ShaderProfile parseProfile(String name, Properties props, Set<String> parsedProfiles, ShaderOption[] shaderOptions) {
        String s = "profile.";
        String s1 = s + name;
        if (parsedProfiles.contains(s1)) {
            Config.warn("[Shaders] Profile already parsed: " + name);
            return null;
        }

        parsedProfiles.add(name);
        ShaderProfile shaderprofile = new ShaderProfile(name);
        String s2 = props.getProperty(s1);
        String[] astring = Config.tokenize(s2, " ");

        for (int i = 0; i < astring.length; i++) {
            String s3 = astring[i];
            if (s3.startsWith(s)) {
                String s4 = s3.substring(s.length());
                ShaderProfile shaderprofile1 = parseProfile(s4, props, parsedProfiles, shaderOptions);
                if (shaderprofile != null) {
                    shaderprofile.addOptionValues(shaderprofile1);
                    shaderprofile.addDisabledPrograms(shaderprofile1.getDisabledPrograms());
                }
            } else {
                String[] astring1 = Config.tokenize(s3, ":=");
                if (astring1.length == 1) {
                    String s7 = astring1[0];
                    boolean flag = true;
                    if (s7.startsWith("!")) {
                        flag = false;
                        s7 = s7.substring(1);
                    }

                    String s5 = "program.";
                    if (s7.startsWith(s5)) {
                        String s6 = s7.substring(s5.length());
                        if (!Shaders.isProgramPath(s6)) {
                            Config.warn("Invalid program: " + s6 + " in profile: " + shaderprofile.getName());
                        } else if (flag) {
                            shaderprofile.removeDisabledProgram(s6);
                        } else {
                            shaderprofile.addDisabledProgram(s6);
                        }
                    } else {
                        ShaderOption shaderoption1 = ShaderUtils.getShaderOption(s7, shaderOptions);
                        if (!(shaderoption1 instanceof ShaderOptionSwitch)) {
                            Config.warn("[Shaders] Invalid option: " + s7);
                        } else {
                            shaderprofile.addOptionValue(s7, String.valueOf(flag));
                            shaderoption1.setVisible(true);
                        }
                    }
                } else if (astring1.length != 2) {
                    Config.warn("[Shaders] Invalid option value: " + s3);
                } else {
                    String s8 = astring1[0];
                    String s9 = astring1[1];
                    ShaderOption shaderoption = ShaderUtils.getShaderOption(s8, shaderOptions);
                    if (shaderoption == null) {
                        Config.warn("[Shaders] Invalid option: " + s3);
                    } else if (!shaderoption.isValidValue(s9)) {
                        Config.warn("[Shaders] Invalid value: " + s3);
                    } else {
                        shaderoption.setVisible(true);
                        shaderprofile.addOptionValue(s8, s9);
                    }
                }
            }
        }

        return shaderprofile;
    }

    public static Map<String, ScreenShaderOptions> parseGuiScreens(Properties props, ShaderProfile[] shaderProfiles, ShaderOption[] shaderOptions) {
        Map<String, ScreenShaderOptions> map = new HashMap<>();
        parseGuiScreen("screen", props, map, shaderProfiles, shaderOptions);
        return map.isEmpty() ? null : map;
    }

    private static boolean parseGuiScreen(
        String key, Properties props, Map<String, ScreenShaderOptions> map, ShaderProfile[] shaderProfiles, ShaderOption[] shaderOptions
    ) {
        String s = props.getProperty(key);
        if (s == null) {
            return false;
        }

        String s1 = key + "$parent$";
        if (map.containsKey(s1)) {
            Config.warn("[Shaders] Screen circular reference: " + key + " = " + s);
            return false;
        }

        List<ShaderOption> list = new ArrayList<>();
        Set<String> set = new HashSet<>();
        String[] astring = Config.tokenize(s, " ");

        for (int i = 0; i < astring.length; i++) {
            String s2 = astring[i];
            if (s2.equals("<empty>")) {
                list.add(null);
            } else if (set.contains(s2)) {
                Config.warn("[Shaders] Duplicate option: " + s2 + ", key: " + key);
            } else {
                set.add(s2);
                if (s2.equals("<profile>")) {
                    if (shaderProfiles == null) {
                        Config.warn("[Shaders] Option profile can not be used, no profiles defined: " + s2 + ", key: " + key);
                    } else {
                        ShaderOptionProfile shaderoptionprofile = new ShaderOptionProfile(shaderProfiles, shaderOptions);
                        list.add(shaderoptionprofile);
                    }
                } else if (s2.equals("*")) {
                    ShaderOption shaderoption = new ShaderOptionRest("<rest>");
                    list.add(shaderoption);
                } else if (s2.startsWith("[") && s2.endsWith("]")) {
                    String s4 = StrUtils.removePrefixSuffix(s2, "[", "]");
                    if (!s4.matches("^[a-zA-Z0-9_]+$")) {
                        Config.warn("[Shaders] Invalid screen: " + s2 + ", key: " + key);
                    } else {
                        map.put(s1, null);
                        boolean flag = parseGuiScreen("screen." + s4, props, map, shaderProfiles, shaderOptions);
                        map.remove(s1);
                        if (!flag) {
                            Config.warn("[Shaders] Invalid screen: " + s2 + ", key: " + key);
                        } else {
                            ShaderOptionScreen shaderoptionscreen = new ShaderOptionScreen(s4);
                            list.add(shaderoptionscreen);
                        }
                    }
                } else {
                    ShaderOption shaderoption1 = ShaderUtils.getShaderOption(s2, shaderOptions);
                    if (shaderoption1 == null) {
                        Config.warn("[Shaders] Invalid option: " + s2 + ", key: " + key);
                        list.add(null);
                    } else {
                        shaderoption1.setVisible(true);
                        list.add(shaderoption1);
                    }
                }
            }
        }

        ShaderOption[] ashaderoption = list.toArray(new ShaderOption[list.size()]);
        String s3 = props.getProperty(key + ".columns");
        int j = Config.parseInt(s3, 2);
        ScreenShaderOptions screenshaderoptions = new ScreenShaderOptions(key, ashaderoption, j);
        map.put(key, screenshaderoptions);
        return true;
    }

    public static LineBuffer loadShader(
        Program program, ShaderType shaderType, InputStream is, String filePath, IShaderPack shaderPack, List<String> listFiles, ShaderOption[] activeOptions
    ) throws IOException {
        LineBuffer linebuffer = LineBuffer.readAll(new InputStreamReader(is));
        linebuffer = resolveIncludes(linebuffer, filePath, shaderPack, 0, listFiles, 0);
        linebuffer = addMacros(linebuffer, 0);
        linebuffer = remapTextureUnits(linebuffer);
        LineBuffer linebuffer1 = new LineBuffer();

        for (String s : linebuffer) {
            s = applyOptions(s, activeOptions);
            linebuffer1.add(s);
        }

        return ShadersCompatibility.remap(program, shaderType, linebuffer1);
    }

    private static String applyOptions(String line, ShaderOption[] ops) {
        if (ops != null && ops.length > 0) {
            for (int i = 0; i < ops.length; i++) {
                ShaderOption shaderoption = ops[i];
                if (shaderoption.matchesLine(line)) {
                    line = shaderoption.getSourceLine();
                    break;
                }
            }

            return line;
        } else {
            return line;
        }
    }

    public static LineBuffer resolveIncludes(
        LineBuffer reader, String filePath, IShaderPack shaderPack, int fileIndex, List<String> listFiles, int includeLevel
    ) throws IOException {
        String s = "/";
        int i = filePath.lastIndexOf("/");
        if (i >= 0) {
            s = filePath.substring(0, i);
        }

        LineBuffer linebuffer = new LineBuffer();
        int j = 0;

        for (String s1 : reader) {
            j++;
            Matcher matcher = PATTERN_INCLUDE.matcher(s1);
            if (matcher.matches()) {
                String s2 = matcher.group(1);
                boolean flag = s2.startsWith("/");
                String s3 = flag ? "/shaders" + s2 : s + "/" + s2;
                if (!listFiles.contains(s3)) {
                    listFiles.add(s3);
                }

                int k = listFiles.indexOf(s3) + 1;
                LineBuffer linebuffer1 = loadFile(s3, shaderPack, k, listFiles, includeLevel);
                if (linebuffer1 == null) {
                    throw new IOException("Included file not found: " + filePath);
                }

                if (linebuffer1.indexMatch(PATTERN_VERSION) < 0) {
                    linebuffer.add("#line 1 " + k);
                }

                linebuffer.add(linebuffer1.getLines());
                linebuffer.add("#line " + (j + 1) + " " + fileIndex);
            } else {
                linebuffer.add(s1);
            }
        }

        return linebuffer;
    }

    public static LineBuffer addMacros(LineBuffer reader, int fileIndex) throws IOException {
        LineBuffer linebuffer = new LineBuffer(reader.getLines());
        int i = linebuffer.indexMatch(PATTERN_VERSION);
        if (i < 0) {
            Config.warn("Macro insert position not found");
            return reader;
        }

        String s = "#line " + (++i + 1) + " " + fileIndex;
        String[] astring = ShaderMacros.getHeaderMacroLines();
        linebuffer.insert(i, astring);
        i += astring.length;
        ShaderMacro[] ashadermacro = getCustomMacros(linebuffer, i);
        if (ashadermacro.length > 0) {
            LineBuffer linebuffer1 = new LineBuffer();

            for (int j = 0; j < ashadermacro.length; j++) {
                ShaderMacro shadermacro = ashadermacro[j];
                linebuffer1.add(shadermacro.getSourceLine());
            }

            linebuffer.insert(i, linebuffer1.getLines());
            i += linebuffer1.size();
        }

        linebuffer.insert(i, s);
        return linebuffer;
    }

    private static ShaderMacro[] getCustomMacros(LineBuffer lines, int startPos) {
        Set<ShaderMacro> set = new LinkedHashSet<>();

        for (int i = startPos; i < lines.size(); i++) {
            String s = lines.get(i);
            if (s.contains(ShaderMacros.getPrefixMacro())) {
                ShaderMacro[] ashadermacro = findMacros(s, ShaderMacros.getExtensions());
                set.addAll(Arrays.asList(ashadermacro));
                ShaderMacro[] ashadermacro1 = findMacros(s, ShaderMacros.getConstantMacros());
                set.addAll(Arrays.asList(ashadermacro1));
            }
        }

        return set.toArray(new ShaderMacro[set.size()]);
    }

    public static LineBuffer remapTextureUnits(LineBuffer reader) throws IOException {
        if (!Shaders.isRemapLightmap()) {
            return reader;
        }

        LineBuffer linebuffer = new LineBuffer();

        for (String s : reader) {
            String s1 = s.replace("gl_TextureMatrix[1]", "gl_TextureMatrix[2]");
            s1 = s1.replace("gl_MultiTexCoord1", "gl_MultiTexCoord2");
            if (!s1.equals(s)) {
                s1 = s1 + " // Legacy fix, replaced TU 1 with 2";
                s = s1;
            }

            linebuffer.add(s);
        }

        return linebuffer;
    }

    private static ShaderMacro[] findMacros(String line, ShaderMacro[] macros) {
        List<ShaderMacro> list = new ArrayList<>();

        for (int i = 0; i < macros.length; i++) {
            ShaderMacro shadermacro = macros[i];
            if (line.contains(shadermacro.getName())) {
                list.add(shadermacro);
            }
        }

        return list.toArray(new ShaderMacro[list.size()]);
    }

    private static LineBuffer loadFile(String filePath, IShaderPack shaderPack, int fileIndex, List<String> listFiles, int includeLevel) throws IOException {
        if (includeLevel >= 10) {
            throw new IOException("#include depth exceeded: " + includeLevel + ", file: " + filePath);
        }

        includeLevel++;
        InputStream inputstream = shaderPack.getResourceAsStream(filePath);
        if (inputstream == null) {
            return null;
        }

        InputStreamReader inputstreamreader = new InputStreamReader(inputstream, "ASCII");
        LineBuffer linebuffer = LineBuffer.readAll(inputstreamreader);
        return resolveIncludes(linebuffer, filePath, shaderPack, fileIndex, listFiles, includeLevel);
    }

    public static CustomUniforms parseCustomUniforms(Properties props) {
        String UNIFORM = "uniform";
        String VARIABLE = "variable";
        String PREFIX_UNIFORM = UNIFORM + ".";
        String PREFIX_VARIABLE = VARIABLE + ".";
        Map<String, IExpression> mapExpressions = new HashMap();
        List<CustomUniform> listUniforms = new ArrayList();
        Set keys = props.keySet();
        Iterator it = keys.iterator();

        while(true) {
            while(true) {
                String key;
                String[] keyParts;
                do {
                    if (!it.hasNext()) {
                        if (listUniforms.size() <= 0) {
                            return null;
                        }

                        CustomUniform[] cusArr = (CustomUniform[])listUniforms.toArray(new CustomUniform[listUniforms.size()]);
                        CustomUniforms cus = new CustomUniforms(cusArr, mapExpressions);
                        return cus;
                    }

                    key = (String)it.next();
                    keyParts = Config.tokenize(key, ".");
                } while(keyParts.length != 3);

                String kind = keyParts[0];
                String type = keyParts[1];
                String name = keyParts[2];
                String src = props.getProperty(key).trim();
                if (mapExpressions.containsKey(name)) {
                    SMCLog.warning("Expression already defined: " + name);
                } else if (kind.equals(UNIFORM) || kind.equals(VARIABLE)) {
                    SMCLog.info("Custom " + kind + ": " + name);
                    CustomUniform cu = parseCustomUniform(kind, name, type, src, mapExpressions);
                    if (cu != null) {
                        mapExpressions.put(name, cu.getExpression());
                        if (!kind.equals(VARIABLE)) {
                            listUniforms.add(cu);
                        }
                    }
                }
            }
        }
    }

    private static CustomUniform parseCustomUniform(String kind, String name, String type, String src, Map<String, IExpression> mapExpressions) {
        try {
            UniformType uniformtype = UniformType.parse(type);
            if (uniformtype == null) {
                SMCLog.warning("Unknown " + kind + " type: " + uniformtype);
                return null;
            } else {
                ShaderExpressionResolver shaderexpressionresolver = new ShaderExpressionResolver(mapExpressions);
                ExpressionParser expressionparser = new ExpressionParser(shaderexpressionresolver);
                IExpression iexpression = expressionparser.parse(src);
                ExpressionType expressiontype = iexpression.getExpressionType();
                if (!uniformtype.matchesExpressionType(expressiontype)) {
                    SMCLog.warning(
                        "Expression type does not match " + kind + " type, expression: " + expressiontype + ", " + kind + ": " + uniformtype + " " + name
                    );
                    return null;
                } else {
                    iexpression = makeExpressionCached(iexpression);
                    return new CustomUniform(name, uniformtype, iexpression);
                }
            }
        } catch (ParseException parseexception) {
            SMCLog.warning(parseexception.getClass().getName() + ": " + parseexception.getMessage());
            return null;
        }
    }

    private static IExpression makeExpressionCached(IExpression expr) {
        if (expr instanceof IExpressionFloat) {
            return new ExpressionFloatCached((IExpressionFloat)expr);
        } else {
            return expr instanceof IExpressionFloatArray ? new ExpressionFloatArrayCached((IExpressionFloatArray)expr) : expr;
        }
    }

    public static void parseAlphaStates(Properties props) {
        Set keys = props.keySet();

        for (Object o : keys) {
            String key = (String) o;
            String[] keyParts = Config.tokenize(key, ".");
            if (keyParts.length == 2) {
                String type = keyParts[0];
                String programName = keyParts[1];
                if (type.equals("alphaTest")) {
                    Program program = Shaders.getProgram(programName);
                    if (program == null) {
                        SMCLog.severe("Invalid program name: " + programName);
                    } else {
                        String val = props.getProperty(key).trim();
                        GlAlphaState state = parseAlphaState(val);
                        if (state != null) {
                            program.setAlphaState(state);
                        }
                    }
                }
            }
        }

    }

    public static GlAlphaState parseAlphaState(String str) {
        if (str == null) {
            return null;
        }

        String[] astring = Config.tokenize(str, " ");
        if (astring.length == 1) {
            String s = astring[0];
            if (s.equals("off") || s.equals("false")) {
                return new GlAlphaState(false);
            }
        } else if (astring.length == 2) {
            String s2 = astring[0];
            String s1 = astring[1];
            Integer integer = mapAlphaFuncs.get(s2);
            float f = Config.parseFloat(s1, -1.0F);
            if (integer != null && f >= 0.0F) {
                return new GlAlphaState(true, integer, f);
            }
        }

        SMCLog.severe("Invalid alpha test: " + str);
        return null;
    }

    public static void parseBlendStates(Properties props) {
        Set keys = props.keySet();
        Iterator it = keys.iterator();

        while(true) {
            while(true) {
                String key;
                String type;
                String programName;
                String bufferName;
                do {
                    String[] keyParts;
                    do {
                        do {
                            if (!it.hasNext()) {
                                return;
                            }

                            key = (String)it.next();
                            keyParts = Config.tokenize(key, ".");
                        } while(keyParts.length < 2);
                    } while(keyParts.length > 3);

                    type = keyParts[0];
                    programName = keyParts[1];
                    bufferName = keyParts.length == 3 ? keyParts[2] : null;
                } while(!type.equals("blend"));

                Program program = Shaders.getProgram(programName);
                if (program == null) {
                    SMCLog.severe("Invalid program name: " + programName);
                } else {
                    String val = props.getProperty(key).trim();
                    GlBlendState state = parseBlendState(val);
                    if (state != null) {
                        if (bufferName != null) {
                            int index = program.getProgramStage().isAnyShadow() ? ShaderParser.getShadowColorIndex(bufferName) : Shaders.getBufferIndex(bufferName);
                            int maxColorIndex = program.getProgramStage().isAnyShadow() ? 2 : 16;
                            if (index >= 0 && index < maxColorIndex) {
                                program.setBlendStateColorIndexed(index, state);
                                SMCLog.info("Blend " + programName + "." + bufferName + "=" + val);
                            } else {
                                SMCLog.severe("Invalid buffer name: " + bufferName);
                            }
                        } else {
                            program.setBlendState(state);
                        }
                    }
                }
            }
        }
    }

    public static GlBlendState parseBlendState(String str) {
        if (str == null) {
            return null;
        }

        String[] astring = Config.tokenize(str, " ");
        if (astring.length == 1) {
            String s = astring[0];
            if (s.equals("off") || s.equals("false")) {
                return new GlBlendState(false);
            }
        } else if (astring.length == 2 || astring.length == 4) {
            String s4 = astring[0];
            String s1 = astring[1];
            String s2 = s4;
            String s3 = s1;
            if (astring.length == 4) {
                s2 = astring[2];
                s3 = astring[3];
            }

            Integer integer = mapBlendFactors.get(s4);
            Integer integer1 = mapBlendFactors.get(s1);
            Integer integer2 = mapBlendFactors.get(s2);
            Integer integer3 = mapBlendFactors.get(s3);
            if (integer != null && integer1 != null && integer2 != null && integer3 != null) {
                return new GlBlendState(true, integer, integer1, integer2, integer3);
            }
        }

        SMCLog.severe("Invalid blend mode: " + str);
        return null;
    }

    public static void parseRenderScales(Properties props) {
        Set keys = props.keySet();

        for (Object o : keys) {
            String key = (String) o;
            String[] keyParts = Config.tokenize(key, ".");
            if (keyParts.length == 2) {
                String type = keyParts[0];
                String programName = keyParts[1];
                if (type.equals("scale")) {
                    Program program = Shaders.getProgram(programName);
                    if (program == null) {
                        SMCLog.severe("Invalid program name: " + programName);
                    } else {
                        String val = props.getProperty(key).trim();
                        RenderScale scale = parseRenderScale(val);
                        if (scale != null) {
                            program.setRenderScale(scale);
                        }
                    }
                }
            }
        }

    }

    private static RenderScale parseRenderScale(String str) {
        if (str == null) {
            return null;
        }

        String[] astring = Config.tokenize(str, " ");
        float f = Config.parseFloat(astring[0], -1.0F);
        float f1 = 0.0F;
        float f2 = 0.0F;
        if (astring.length > 1) {
            if (astring.length != 3) {
                SMCLog.severe("Invalid render scale: " + str);
                return null;
            }

            f1 = Config.parseFloat(astring[1], -1.0F);
            f2 = Config.parseFloat(astring[2], -1.0F);
        }

        if (Config.between(f, 0.0F, 1.0F) && Config.between(f1, 0.0F, 1.0F) && Config.between(f2, 0.0F, 1.0F)) {
            return new RenderScale(f, f1, f2);
        }

        SMCLog.severe("Invalid render scale: " + str);
        return null;
    }

    public static void parseBuffersFlip(Properties props) {
        Set keys = props.keySet();
        Iterator it = keys.iterator();

        while(true) {
            while(true) {
                String key;
                String type;
                String programName;
                String bufferName;
                do {
                    String[] keyParts;
                    do {
                        if (!it.hasNext()) {
                            return;
                        }

                        key = (String)it.next();
                        keyParts = Config.tokenize(key, ".");
                    } while(keyParts.length != 3);

                    type = keyParts[0];
                    programName = keyParts[1];
                    bufferName = keyParts[2];
                } while(!type.equals("flip"));

                Program program = Shaders.getProgram(programName);
                if (program == null) {
                    SMCLog.severe("Invalid program name: " + programName);
                } else {
                    Boolean[] buffersFlip = program.getBuffersFlip();
                    int buffer = Shaders.getBufferIndex(bufferName);
                    if (buffer >= 0 && buffer < buffersFlip.length) {
                        String valStr = props.getProperty(key).trim();
                        Boolean val = Config.parseBoolean(valStr, (Boolean)null);
                        if (val == null) {
                            SMCLog.severe("Invalid boolean value: " + valStr);
                        } else {
                            buffersFlip[buffer] = val;
                        }
                    } else {
                        SMCLog.severe("Invalid buffer name: " + bufferName);
                    }
                }
            }
        }
    }

    private static Map<String, Integer> makeMapAlphaFuncs() {
        Map<String, Integer> map = new HashMap<>();
        map.put("NEVER", new Integer(512));
        map.put("LESS", new Integer(513));
        map.put("EQUAL", new Integer(514));
        map.put("LEQUAL", new Integer(515));
        map.put("GREATER", new Integer(516));
        map.put("NOTEQUAL", new Integer(517));
        map.put("GEQUAL", new Integer(518));
        map.put("ALWAYS", new Integer(519));
        return Collections.unmodifiableMap(map);
    }

    private static Map<String, Integer> makeMapBlendFactors() {
        Map<String, Integer> map = new HashMap<>();
        map.put("ZERO", new Integer(0));
        map.put("ONE", new Integer(1));
        map.put("SRC_COLOR", new Integer(768));
        map.put("ONE_MINUS_SRC_COLOR", new Integer(769));
        map.put("DST_COLOR", new Integer(774));
        map.put("ONE_MINUS_DST_COLOR", new Integer(775));
        map.put("SRC_ALPHA", new Integer(770));
        map.put("ONE_MINUS_SRC_ALPHA", new Integer(771));
        map.put("DST_ALPHA", new Integer(772));
        map.put("ONE_MINUS_DST_ALPHA", new Integer(773));
        map.put("CONSTANT_COLOR", new Integer(32769));
        map.put("ONE_MINUS_CONSTANT_COLOR", new Integer(32770));
        map.put("CONSTANT_ALPHA", new Integer(32771));
        map.put("ONE_MINUS_CONSTANT_ALPHA", new Integer(32772));
        map.put("SRC_ALPHA_SATURATE", new Integer(776));
        return Collections.unmodifiableMap(map);
    }

    public static DynamicDimension[] parseBufferSizes(Properties props, int countBuffers) {
        DynamicDimension[] bufferSizes = new DynamicDimension[countBuffers];
        Set keys = props.keySet();
        Iterator it = keys.iterator();

        while(true) {
            while(true) {
                String key;
                String[] keyParts;
                do {
                    do {
                        if (!it.hasNext()) {
                            return bufferSizes;
                        }

                        key = (String)it.next();
                    } while(!key.startsWith("size.buffer."));

                    keyParts = Config.tokenize(key, ".");
                } while(keyParts.length != 3);

                String bufferName = keyParts[2];
                int buffer = Shaders.getBufferIndex(bufferName);
                if (buffer >= 0 && buffer < bufferSizes.length) {
                    String val = props.getProperty(key).trim();
                    DynamicDimension dim = parseDynamicDimension(val);
                    if (dim == null) {
                        SMCLog.severe("Invalid buffer size: " + key + "=" + val);
                    } else {
                        bufferSizes[buffer] = dim;
                        if (dim.isRelative()) {
                            SMCLog.info("Relative size " + bufferName + ": " + dim.getWidth() + " " + dim.getHeight());
                        } else {
                            SMCLog.info("Fixed size " + bufferName + ": " + (int)dim.getWidth() + " " + (int)dim.getHeight());
                        }
                    }
                } else {
                    SMCLog.severe("Invalid buffer name: " + key);
                }
            }
        }
    }

    private static DynamicDimension parseDynamicDimension(String str) {
        if (str == null) {
            return null;
        }

        String[] astring = Config.tokenize(str, " ");
        if (astring.length != 2) {
            return null;
        }

        int i = Config.parseInt(astring[0], -1);
        int j = Config.parseInt(astring[1], -1);
        if (i >= 0 && j >= 0) {
            return new DynamicDimension(false, i, j);
        }

        float f = Config.parseFloat(astring[0], -1.0F);
        float f1 = Config.parseFloat(astring[1], -1.0F);
        return f >= 0.0F && f1 >= 0.0F ? new DynamicDimension(true, f, f1) : null;
    }
}
