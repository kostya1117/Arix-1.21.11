package mcp.client;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

import net.minecraft.client.main.Main;

public class Start {
    public static void main(String[] args) throws IOException {
        /*
         * start minecraft game application
         * --version is just used as 'launched version' in snoop data and is required
         * Working directory is used as gameDir if not provided
         */
        int INDEX = 29;
        String assets = findAssets(INDEX);

        Main.main(concat(new String[]{"--version", "mcp", "--accessToken", "0", "--assetsDir", assets, "--assetIndex", Integer.toString(INDEX), "--userProperties", "{}"}, args));
    }

    public static <T> T[] concat(T[] first, T[] second) {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    private static String findAssets(int index) throws IOException {
        String ret = System.getenv("assetDirectory");
        if (ret != null)
            return ret;

        File projectAssets = new File("assets");
        if (projectAssets.exists() && projectAssets.isDirectory()) {
            File projectIndexes = new File(projectAssets, "indexes");
            if (projectIndexes.exists() && hasIndex(projectAssets, index)) {
                return projectAssets.getCanonicalPath();
            }
        }
        return new File("assets").getCanonicalPath();
    }

    private static boolean hasIndex(File root, int idx) {
        return new File(root, "indexes/" + idx + ".json").exists();
    }
}