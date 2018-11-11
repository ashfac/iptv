package com.example.android.sampletvinput.parsers;

import android.util.Log;

import com.example.android.sampletvinput.util.SimpleHttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class FilmOnParser extends VideoUrlParser {
    private static final String TAG = "FilmOnParser";
    private static final String PARSER_ID = "filmon";

    private static final String KEY_CODE = "code";
    private static final String KEY_DATA = "data";
    private static final String KEY_STREAMS = "streams";
    private static final String KEY_QUALITY = "quality";
    private static final String KEY_QUALITY_HIGH = "high";
    private static final String KEY_URL = "url";

    private static final int CODE_SUCCESS = 200;

    @Override
    public String getParserId() {
        return PARSER_ID;
    }

    @Override
    public String parseVideoUrl(String videoUrl){
        String streamUrl = null;
        try {
            String html_response = SimpleHttpClient.GET(videoUrl);

            if (html_response != null && html_response.length() > 0) {
                try {
                    JSONObject jsonObject = new JSONObject(html_response);

                    if (jsonObject.getInt(KEY_CODE) == CODE_SUCCESS) {
                        JSONArray jsonArray = jsonObject.getJSONObject(KEY_DATA).getJSONArray(KEY_STREAMS);

                        for (int i=0; i < jsonArray.length(); i++) {
                            JSONObject jsonStream = jsonArray.getJSONObject(i);
                            if(jsonStream.getString(KEY_QUALITY).equals(KEY_QUALITY_HIGH)) {
                                streamUrl = jsonStream.getString(KEY_URL);
                            }
                        }

                        if ((streamUrl == null) && (jsonArray.length() > 0)) {
                            streamUrl = jsonArray.getJSONObject(0).getString(KEY_URL);
                        }

                    }
                } catch (JSONException e) {
                    Log.e(TAG, "JsonEpg parsing error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "parseVideoUrl " + e.getMessage());
        }

        return streamUrl;
    }
}
