package com.example.android.sampletvinput.parsers;

import android.util.Log;

import com.example.android.sampletvinput.util.SimpleHttpClient;
import com.example.android.sampletvinput.util.Util;
import com.google.android.media.tv.companionlibrary.model.Program;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class EuroNewsParser extends VideoUrlParser {
    private static final String TAG = "EuroNewsParser";
    private static final String PARSER_ID = "euronews.com";

    private static final String TAG_URL = "url";
    private static final String TAG_URL_PRIMARY = "primary";
    private static final String TAG_URL_BACKUP = "backup";

    @Override
    public String getParserId() {
        return PARSER_ID;
    }

    @Override
    public String parseVideoUrl(String videoUrl){
        String html_response = null;

        try {
            html_response = SimpleHttpClient.GET(videoUrl, Util.USER_AGENT_FIREFOX);

            try {
                JSONObject jsonObject = new JSONObject(html_response);

                String url = "https:" + jsonObject.getString(TAG_URL);

                html_response = SimpleHttpClient.GET(url, Util.USER_AGENT_FIREFOX);

                jsonObject = new JSONObject(html_response);

                videoUrl = jsonObject.getString(TAG_URL_PRIMARY);

                if(SimpleHttpClient.isValidUrl(videoUrl, Util.USER_AGENT_FIREFOX)) {
                    return videoUrl;
                } else {
                    videoUrl = jsonObject.getString(TAG_URL_BACKUP);
                }

            } catch (JSONException e) {
                Log.e(TAG, "JsonEpg parsing error: " + e.getMessage());
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        return videoUrl;
    }
}
