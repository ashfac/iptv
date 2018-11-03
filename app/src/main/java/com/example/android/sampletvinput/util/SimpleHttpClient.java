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
    public static final int DEFAULT_TIMEOUT = 3000;

    public static final String HTTP_GET = "GET";
    public static final String HTTP_POST = "POST";

    public static String GET(String urlStr) throws IOException {
        return execute(urlStr, HTTP_GET, null, null, DEFAULT_TIMEOUT);
    }

    public static String GET(String urlStr, String userAgent) throws IOException {
        return execute(urlStr, HTTP_GET, userAgent, null, DEFAULT_TIMEOUT);
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

    public static boolean isValidUrl(String urlStr, String userAgent) {
        URL url = null;
        HttpURLConnection urlConnection = null;

        if(urlStr == null) {
            return false;
        }

        try {
            url = new URL(urlStr);
            urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.setConnectTimeout(DEFAULT_TIMEOUT);
            urlConnection.setRequestMethod(HTTP_GET);

            if(userAgent != null) {
                urlConnection.setRequestProperty("User-Agent", userAgent);
            }

            if(urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                return true;
            }
        } catch (IOException e) {
            Log.e(TAG, "isValidUrl: " + e.getMessage());
        }

        return false;

    }


    private static String execute(String urlStr,
                           String httpMethod,
                           String userAgent,
                           String queryParams,
                           int timeout) throws IOException
    {
        URL url = null;
        HttpURLConnection urlConnection = null;
        InputStream inStream = null;
        OutputStream outStream = null;
        String response = null;

        if(urlStr == null) {
            return null;
        }

        try
        {
            url = new URL(urlStr);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(timeout);
            urlConnection.setRequestMethod(httpMethod);

            if(userAgent != null) {
                urlConnection.setRequestProperty("User-Agent", userAgent);
            }

            if(httpMethod.contentEquals(HTTP_POST) && queryParams != null) {
                urlConnection.setDoInput(true);
                urlConnection.setDoOutput(true);
                outStream = new BufferedOutputStream(urlConnection.getOutputStream());

                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outStream, "UTF-8"));
                writer.write(queryParams);
                writer.flush();
                writer.close();
                outStream.close();

                urlConnection.connect();
            }

            int responseCode=urlConnection.getResponseCode();

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                inStream = new BufferedInputStream(urlConnection.getInputStream());
                response = getInput(inStream);
            } else {
                response="";
            }
        } catch (Exception e) {
            if(urlConnection != null) {
                inStream = new BufferedInputStream(urlConnection.getInputStream());
                response = getInput(inStream);
            }
        }
        finally
        {
            if(urlConnection != null && urlConnection.getErrorStream() != null)
            {
                String errorResponse = " : ";
                errorResponse = errorResponse + getInput(urlConnection.getErrorStream());
                response = response + errorResponse;
            }

            if (urlConnection != null)
            {
                urlConnection.disconnect();
            }
        }

        return response;
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
