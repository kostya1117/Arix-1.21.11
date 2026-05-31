package net.optifine.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.CompositePackResources;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraftforge.resource.DelegatingPackResources;
import net.optifine.Config;

public class ResUtils {
    public static String[] collectFiles(String prefix, String suffix) {
        return collectFiles(new String[]{prefix}, new String[]{suffix});
    }

    public static String[] collectFiles(String[] prefixes, String[] suffixes) {
        Set<String> set = new LinkedHashSet<>();
        PackResources[] apackresources = Config.getResourcePacks();

        for (int i = 0; i < apackresources.length; i++) {
            PackResources packresources = apackresources[i];
            String[] astring = collectFiles(packresources, prefixes, suffixes, null);
            set.addAll(Arrays.asList(astring));
        }

        return set.toArray(new String[set.size()]);
    }

    public static String[] collectFiles(PackResources rp, String prefix, String suffix, String[] defaultPaths) {
        return collectFiles(rp, new String[]{prefix}, new String[]{suffix}, defaultPaths);
    }

    public static String[] collectFiles(PackResources rp, String[] prefixes, String[] suffixes) {
        return collectFiles(rp, prefixes, suffixes, null);
    }

    public static String[] collectFiles(PackResources rp, String[] prefixes, String[] suffixes, String[] defaultPaths) {
        if (!(rp instanceof CompositePackResources compositepackresources)) {
            if (rp instanceof DelegatingPackResources) {
                return new String[0];
            }

            if (rp instanceof VanillaPackResources) {
                return collectFilesFixed(rp, defaultPaths);
            }

            File file1 = null;
            if (rp instanceof FilePackResources filepackresources) {
                file1 = filepackresources.getFile();
            } else {
                if (!(rp instanceof PathPackResources pathpackresources)) {
                    Config.warn("Unknown resource pack type: " + rp);
                    return new String[0];
                }

                file1 = pathpackresources.root.toFile();
            }

            if (file1 == null) {
                return new String[0];
            }

            if (file1.isDirectory()) {
                return collectFilesFolder(file1, "", prefixes, suffixes);
            }

            if (file1.isFile()) {
                return collectFilesZIP(file1, prefixes, suffixes);
            }

            Config.warn("Unknown resource pack file: " + file1);
            return new String[0];
        } else {
            Set<String> set = new LinkedHashSet<>();

            for (PackResources packresources : compositepackresources.packResourcesStack) {
                String[] astring = collectFiles(packresources, prefixes, suffixes, defaultPaths);
                set.addAll(Arrays.asList(astring));
            }

            return set.toArray(new String[set.size()]);
        }
    }

    private static String[] collectFilesFixed(PackResources rp, String[] paths) {
        if (paths == null) {
            return new String[0];
        } else {
            List list = new ArrayList();

            for (String path : paths) {
                if (!isLowercase(path)) {
                    Config.warn("Skipping non-lowercase path: " + path);
                } else {
                    Identifier loc = new Identifier(path);
                    if (Config.hasResource(rp, loc)) {
                        list.add(path);
                    }
                }
            }

            String[] pathArr = (String[])list.toArray(new String[list.size()]);
            return pathArr;
        }
    }

    private static String[] collectFilesFolder(File tpFile, String basePath, String[] prefixes, String[] suffixes) {
        List list = new ArrayList();
        String s = "assets/minecraft/";
        File[] afile = tpFile.listFiles();
        if (afile == null) {
            return new String[0];
        }

        for (File file1 : afile) {
            if (file1.isFile()) {
                String s3 = basePath + file1.getName();
                if (s3.startsWith(s)) {
                    s3 = s3.substring(s.length());
                    if (StrUtils.startsWith(s3, prefixes) && StrUtils.endsWith(s3, suffixes)) {
                        if (!isLowercase(s3)) {
                            Config.warn("Skipping non-lowercase path: " + s3);
                        } else {
                            list.add(s3);
                        }
                    }
                }
            } else if (file1.isDirectory()) {
                String s1 = basePath + file1.getName() + "/";
                String[] astring = collectFilesFolder(file1, s1, prefixes, suffixes);

                Collections.addAll(list, astring); //TEST
            }
        }

        return (String[]) list.toArray(new String[list.size()]);
    }

    private static String[] collectFilesZIP(File tpFile, String[] prefixes, String[] suffixes) {
        List list = new ArrayList();
        String prefixAssets = "assets/minecraft/";

        try {
            ZipFile zf = new ZipFile(tpFile);
            Enumeration<? extends ZipEntry> en = zf.entries();

            while(en.hasMoreElements()) {
                ZipEntry ze = en.nextElement();
                String name = ze.getName();
                if (name.startsWith(prefixAssets)) {
                    name = name.substring(prefixAssets.length());
                    if (StrUtils.startsWith(name, prefixes) && StrUtils.endsWith(name, suffixes)) {
                        if (!isLowercase(name)) {
                            Config.warn("Skipping non-lowercase path: " + name);
                        } else {
                            list.add(name);
                        }
                    }
                }
            }

            zf.close();
            String[] names = (String[])list.toArray(new String[list.size()]);
            return names;
        } catch (IOException var9) {
            var9.printStackTrace();
            return new String[0];
        }
    }

    private static boolean isLowercase(String str) {
        return str.equals(str.toLowerCase(Locale.ROOT));
    }

    public static Properties readProperties(String path, String module) {
        Identifier identifier = new Identifier(path);

        try {
            InputStream inputstream = Config.getResourceStream(identifier);
            if (inputstream == null) {
                return null;
            }

            Properties properties = new PropertiesOrdered();
            properties.load(inputstream);
            inputstream.close();
            Config.dbg(module + ": Loading " + path);
            return properties;
        } catch (FileNotFoundException filenotfoundexception) {
            return null;
        } catch (IOException ioexception) {
            Config.warn(module + ": Error reading " + path);
            return null;
        }
    }

    public static Properties readProperties(InputStream in, String module) {
        if (in == null) {
            return null;
        }

        try {
            Properties properties = new PropertiesOrdered();
            properties.load(in);
            in.close();
            return properties;
        } catch (FileNotFoundException filenotfoundexception) {
            return null;
        } catch (IOException ioexception) {
            return null;
        }
    }
}
