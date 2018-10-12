package com.example.android.sampletvinput.parsers;

import android.util.Log;

import com.example.android.sampletvinput.util.SimpleHttpClient;
import com.example.android.sampletvinput.util.Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class DaCastParser extends VideoUrlParser{
    private static final String TAG = "DaCastParser";
    private static final String PARSER_ID = "dacast";
    private static final String KEY_STREAM = "hls";

    @Override
    public String getParserId() {
        return PARSER_ID;
    }

    @Override
    public String parseVideoUrl(String videoUrl){
        try {
            String html_response = SimpleHttpClient.GET(videoUrl);

            if (html_response != null && html_response.length() > 0) {
                try {
                    JSONObject jsonObject = new JSONObject(html_response);
                    videoUrl = jsonObject.getString(KEY_STREAM);
                } catch (JSONException e) {
                    Log.e(TAG, "JsonEpg parsing error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "getVideoUrl " + e.getMessage());
        }

        return videoUrl;
    }
}
