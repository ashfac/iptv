package com.example.android.sampletvinput.parsers;

import android.util.Log;

import com.example.android.sampletvinput.util.SimpleHttpClient;
import com.example.android.sampletvinput.util.Util;

import java.io.IOException;

public class StreamLinkParser extends VideoUrlParser {
    private static final String TAG = "StreamLinkParser";
    private static final String PARSER_ID = "streamlink?streamlink";

    @Override
    public String getParserId() {
        return PARSER_ID;
    }

    @Override
    public String parseVideoUrl(String videoUrl){
        String returnUrl = null;
        try {
            String html_response = SimpleHttpClient.GET(videoUrl, Util.USER_AGENT_FIREFOX, 10000);

            if(html_response != null) {
                if(html_response.contains("http")) {
                    returnUrl = html_response.substring(html_response.indexOf("http")).replace("\n", "");
                }
            }

        } catch (IOException e) {
            Log.e(TAG, "parseVideoUrl " + e.getMessage());
        }

        return returnUrl;
    }
}
