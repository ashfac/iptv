package com.example.android.sampletvinput.util;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class SimpleHttpClient
{
    private static String TAG = "SimpleHttpClient";

    public static final String ENCODING_UTF_8 = "UTF-8";
    public static final int DEFAULT_TIMEOUT = 5000;

    public static final String HTTP_GET = "GET";
    public static final String HTTP_POST = "POST";

    public static String GET(String urlStr) throws IOException {
        return execute(urlStr, HTTP_GET, null, null, DEFAULT_TIMEOUT);
    }

    public static String GET(String urlStr, String userAgent) throws IOException {
        return execute(urlStr, HTTP_GET, userAgent, null, DEFAULT_TIMEOUT);
    }

    public static String GET(String urlStr, String userAgent, int timeout) throws IOException {
        return execute(urlStr, HTTP_GET, userAgent, null, timeout);
    }

    public static String POST(String urlStr, String queryParams) throws IOException {
        return execute(urlStr, HTTP_POST, null, queryParams, DEFAULT_TIMEOUT);
    }

    public static String POST(String urlStr, String userAgent, String queryParams) throws IOException {
        return execute(urlStr, HTTP_POST, userAgent, queryParams, DEFAULT_TIMEOUT);
    }

    public static boolean isValidUrl(String urlStr) {
        return isValidUrl(urlStr, null);
    }

    public static boolean isValidUrl(final String urlStr, final String userAgent) {
        if(urlStr == null) {
            return false;
        }

        final boolean[] executed = {false};
        final boolean[] response = {false};
        final long startTime = System.currentTimeMillis();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(urlStr);
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                    urlConnection.setConnectTimeout(DEFAULT_TIMEOUT);
                    urlConnection.setRequestMethod(HTTP_GET);

                    if(userAgent != null) {
                        urlConnection.setRequestProperty("User-Agent", userAgent);
                    }

                    if(urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        response[0] = true;
                    }
                } catch (IOException e) {
                    Log.e(TAG, "isValidUrl: " + e.getMessage());
                } finally {
                    executed[0] = true;
                }
            }
        }).start();

        while (((System.currentTimeMillis() - startTime) <= DEFAULT_TIMEOUT)) {
            if (executed[0] == true || (System.currentTimeMillis() - startTime) >= DEFAULT_TIMEOUT) {
                return response[0];
            }
            try {
                Thread.sleep(50);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return false;

    }


    private static String execute(final String urlStr,
                           final String httpMethod,
                           final String userAgent,
                           final String queryParams,
                           final int timeout) throws IOException
    {
        if(urlStr == null) {
            return null;
        }

        final String[] response = {null};
        final boolean[] executed = {false};
        final long startTime = System.currentTimeMillis();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(urlStr);
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setConnectTimeout(timeout);
                    urlConnection.setReadTimeout(timeout);
                    urlConnection.setRequestMethod(httpMethod);

                    if (userAgent != null) {
                        urlConnection.setRequestProperty("User-Agent", userAgent);
                    }

                    if (httpMethod.contentEquals(HTTP_POST) && queryParams != null) {
                        urlConnection.setDoInput(true);
                        urlConnection.setDoOutput(true);
                        OutputStream outStream = new BufferedOutputStream(urlConnection.getOutputStream());

                        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outStream, "UTF-8"));
                        writer.write(queryParams);
                        writer.flush();
                        writer.close();
                        outStream.close();

                        urlConnection.connect();
                    }

                    int responseCode = urlConnection.getResponseCode();

                    if (responseCode == HttpsURLConnection.HTTP_OK) {
                        InputStream inStream = new BufferedInputStream(urlConnection.getInputStream());
                        response[0] = getInput(inStream);
                    } else {
                        response[0] = null;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "error getting url: " + urlStr + " with message: " + e.getMessage());
                } finally {
                    executed[0] = true;
                }
            }
        }).start();

        while (((System.currentTimeMillis() - startTime) <= timeout)) {
            if (executed[0] == true || (System.currentTimeMillis() - startTime) >= timeout) {
                return response[0];
            }
            try {
                Thread.sleep(50);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return response[0];
    }

    private static String getInput(InputStream in) throws IOException
    {
        StringBuilder sb = new StringBuilder(8192);
        byte[] b = new byte[1024];
        int bytesRead = 0;

        while (true)
        {
            bytesRead = in.read(b);
            if (bytesRead < 0)
            {
                break;
            }
            String s = new String(b, 0, bytesRead, ENCODING_UTF_8);
            sb.append(s);
        }

        return sb.toString();
    }

}
