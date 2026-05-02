package net.optifine;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;
import net.minecraft.resources.Identifier;
import net.optifine.config.ConnectedParser;
import net.optifine.util.PropertiesOrdered;
import net.optifine.util.StrUtils;

public class RandomEntityProperties<T> {
    private String name = null;
    private String basePath = null;
    private RandomEntityContext<T> context;
    private T[] resources = null;
    private RandomEntityRule<T>[] rules = null;
    private int matchingRuleIndex = -1;

    public RandomEntityProperties(String name, Identifier baseLoc, int[] variants, RandomEntityContext<T> context) {
        ConnectedParser connectedparser = new ConnectedParser(context.getName());
        this.name = name;
        this.basePath = connectedparser.parseBasePath(baseLoc.getPath());
        this.context = context;
        this.resources = (T[])(new Object[variants.length]);

        for (int i = 0; i < variants.length; i++) {
            int j = variants[i];
            this.resources[i] = context.makeResource(name, baseLoc, j);
        }
    }

    public RandomEntityProperties(Properties props, String path, Identifier baseResLoc, RandomEntityContext<T> context) {
        ConnectedParser connectedparser = context.getConnectedParser();
        this.name = connectedparser.parseName(path);
        this.basePath = connectedparser.parseBasePath(path);
        this.context = context;
        this.rules = this.parseRules(props, path, baseResLoc);
    }

    public String getName() {
        return this.name;
    }

    public String getBasePath() {
        return this.basePath;
    }

    public T[] getResources() {
        return this.resources;
    }

    public List<T> getAllResources() {
        List<T> list = new ArrayList<>();
        if (this.resources != null) {
            list.addAll(Arrays.asList(this.resources));
        }

        if (this.rules != null) {
            for (int i = 0; i < this.rules.length; i++) {
                RandomEntityRule<T> randomentityrule = this.rules[i];
                if (randomentityrule.getResources() != null) {
                    list.addAll(Arrays.asList(randomentityrule.getResources()));
                }
            }
        }

        return list;
    }

    public T getResource(IRandomEntity randomEntity, T resDef) {
        this.matchingRuleIndex = 0;
        if (this.rules != null) {
            for (int i = 0; i < this.rules.length; i++) {
                RandomEntityRule<T> randomentityrule = this.rules[i];
                if (randomentityrule.matches(randomEntity)) {
                    this.matchingRuleIndex = randomentityrule.getIndex();
                    int j = getRandomId(randomEntity, randomentityrule.isSeedSourceVehicle(), randomentityrule.getSeedOffset());
                    return randomentityrule.getResource(j, resDef);
                }
            }
        }

        if (this.resources != null) {
            int k = getRandomId(randomEntity, false, 0);
            int l = k % this.resources.length;
            return this.resources[l];
        } else {
            return resDef;
        }
    }

    private static int getRandomId(IRandomEntity randomEntityIn, boolean sourceVehicleIn, int seedOffsetIn) {
        int i = sourceVehicleIn ? randomEntityIn.getVehicleId() : randomEntityIn.getId();
        if (seedOffsetIn != 0) {
            i ^= Config.intHash(seedOffsetIn);
        }

        return i;
    }

    private RandomEntityRule<T>[] parseRules(Properties props, String pathProps, Identifier baseResLoc) {
        List list = new ArrayList();
        SortedMap<Integer, String> sortedmap = this.collectIndexTextures(props);

        for (Entry<Integer, String> entry : sortedmap.entrySet()) {
            int i = entry.getKey();
            String s = entry.getValue();
            if (s != null) {
                RandomEntityRule<T> randomentityrule = new RandomEntityRule<>(props, pathProps, baseResLoc, i, s, this.context);
                list.add(randomentityrule);
            }
        }

        return (RandomEntityRule<T>[]) list.toArray(new RandomEntityRule[list.size()]);
    }

    private SortedMap<Integer, String> collectIndexTextures(Properties props) {
        String[] astring = this.context.getResourceKeys();
        SortedMap<Integer, String> sortedmap = new TreeMap<>();

        for (Entry entry : props.entrySet()) {
            String s = (String)entry.getKey();
            String s1 = StrUtils.removePrefix(s, astring);
            if (!Config.equals(s, s1) && s1.startsWith(".")) {
                String s2 = s1.substring(1);
                int i = Config.parseInt(s2, 0);
                if (i > 0) {
                    String s3 = (String)entry.getValue();
                    sortedmap.put(i, s3);
                }
            }
        }

        return sortedmap;
    }

    public boolean isValid(String path) {
        String s = this.context.getResourceNamePlural();
        if (this.resources == null && this.rules == null) {
            Config.warn("No " + s + " specified: " + path);
            return false;
        }

        if (this.rules != null) {
            for (int i = 0; i < this.rules.length; i++) {
                RandomEntityRule randomentityrule = this.rules[i];
                if (!randomentityrule.isValid(path)) {
                    return false;
                }
            }
        }

        if (this.resources != null) {
            for (int j = 0; j < this.resources.length; j++) {
                T t = this.resources[j];
                if (t == null) {
                    return false;
                }
            }
        }

        return true;
    }

    public boolean isDefault() {
        return this.rules != null ? false : this.resources == null;
    }

    public int getMatchingRuleIndex() {
        return this.matchingRuleIndex;
    }

    public static RandomEntityProperties parse(Identifier propLoc, Identifier resLoc, RandomEntityContext context) {
        String s = context.getName();

        try {
            String s1 = propLoc.getPath();
            Config.dbg(s + ": " + resLoc.getPath() + ", properties: " + s1);
            InputStream inputstream = Config.getResourceStream(propLoc);
            if (inputstream == null) {
                Config.warn(s + ": Properties not found: " + s1);
                return null;
            } else {
                Properties properties = new PropertiesOrdered();
                properties.load(inputstream);
                inputstream.close();
                RandomEntityProperties randomentityproperties = new RandomEntityProperties(properties, s1, resLoc, context);
                return !randomentityproperties.isValid(s1) ? null : randomentityproperties;
            }
        } catch (FileNotFoundException filenotfoundexception) {
            Config.warn(s + ": File not found: " + propLoc.getPath());
            return null;
        } catch (IOException ioexception) {
            ioexception.printStackTrace();
            return null;
        }
    }

    @Override
    public String toString() {
        return this.name + ", path: " + this.basePath;
    }
}
