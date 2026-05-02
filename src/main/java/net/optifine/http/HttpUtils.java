package net.optifine.http;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.optifine.Config;

public class HttpUtils {
    private static String playerItemsUrl = null;
    public static final String SERVER_URL = "http://s.optifine.net";
    public static final String POST_URL = "http://optifine.net";

    public static byte[] get(String urlStr) throws IOException {
        HttpURLConnection httpurlconnection = null;

        try {
            URL url = new URL(urlStr);
            httpurlconnection = (HttpURLConnection)url.openConnection(Minecraft.getInstance().getProxy());
            httpurlconnection.setDoInput(true);
            httpurlconnection.setDoOutput(false);
            httpurlconnection.connect();
            if (httpurlconnection.getResponseCode() / 100 != 2) {
                if (httpurlconnection.getErrorStream() != null) {
                    Config.readAll(httpurlconnection.getErrorStream());
                }

                throw new IOException("HTTP response: " + httpurlconnection.getResponseCode());
            } else {
                InputStream inputstream = httpurlconnection.getInputStream();
                byte[] abyte = new byte[httpurlconnection.getContentLength()];
                int i = 0;

                do {
                    int j = inputstream.read(abyte, i, abyte.length - i);
                    if (j < 0) {
                        throw new IOException("Input stream closed: " + urlStr);
                    }

                    i += j;
                } while (i < abyte.length);

                return abyte;
            }
        } finally {
            if (httpurlconnection != null) {
                httpurlconnection.disconnect();
            }
        }
    }

    public static String post(String urlStr, Map headers, byte[] content) throws IOException {
        HttpURLConnection conn = null;

        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection)url.openConnection(Minecraft.getInstance().getProxy());
            conn.setRequestMethod("POST");
            if (headers != null) {
                Set keys = headers.keySet();

                for (Object o : keys) {
                    String key = (String) o;
                    Object var10000 = headers.get(key);
                    String val = "" + String.valueOf(var10000);
                    conn.setRequestProperty(key, val);
                }
            }

            conn.setRequestProperty("Content-Type", "text/plain");
            conn.setRequestProperty("Content-Length", "" + content.length);
            conn.setRequestProperty("Content-Language", "en-US");
            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            OutputStream os = conn.getOutputStream();
            os.write(content);
            os.flush();
            os.close();
            InputStream in = conn.getInputStream();
            InputStreamReader isr = new InputStreamReader(in, "ASCII");
            BufferedReader br = new BufferedReader(isr);
            StringBuffer sb = new StringBuffer();

            String line;
            while((line = br.readLine()) != null) {
                sb.append(line);
                sb.append('\r');
            }

            br.close();
            String var11 = sb.toString();
            return var11;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }

        }
    }

    public static synchronized String getPlayerItemsUrl() {
        if (playerItemsUrl == null) {
            try {
                boolean flag = Config.parseBoolean(System.getProperty("player.models.local"), false);
                if (flag) {
                    File file1 = Minecraft.getInstance().gameDirectory;
                    File file2 = new File(file1, "playermodels");
                    playerItemsUrl = file2.toURI().toURL().toExternalForm();
                }
            } catch (Exception exception) {
                Config.warn(exception.getClass().getName() + ": " + exception.getMessage());
            }

            if (playerItemsUrl == null) {
                playerItemsUrl = "http://s.optifine.net";
            }
        }

        return playerItemsUrl;
    }
}
