package net.simplyrin.socialdownloader.android;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

public class HttpClient {

    public static String rawWithAgent(String url) {
        try {
            HttpURLConnection connection = getHttpURLConnection(url);
            InputStream is = connection.getInputStream();
            return IOUtils.toString(is, Charset.forName("UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static HttpURLConnection getHttpURLConnection(String url) {
        url = url.replace(" ", "%20");
        System.out.println("Fetching: " + url);
        try {
            URL u = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) u.openConnection();
            connection.setRequestMethod("GET");
            connection.setUseCaches(true);
            connection.addRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setReadTimeout(15000);
            connection.setConnectTimeout(15000);
            connection.setDoOutput(false);
            return connection;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


}
