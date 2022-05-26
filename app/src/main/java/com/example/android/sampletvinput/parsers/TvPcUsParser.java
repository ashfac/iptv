package com.example.android.sampletvinput.parsers;

import android.util.Log;

import com.example.android.sampletvinput.util.SimpleHttpClient;
import com.example.android.sampletvinput.util.Util;

import java.io.IOException;

public class TvPcUsParser extends VideoUrlParser {
    private static final String TAG = "TvPcUsParser";
    private static final String PARSER_ID = "tvpc.us";

    private static final String TAG_URL_SEARCH = "application/x-mpegurl";
    private static final String TAG_URL_START = "src: \"";
    private static final String TAG_URL_END = "\"";
    private static final int HTTP_TIMEOUT = 10000;

    @Override
    public String getParserId() {
        return PARSER_ID;
    }

    @Override
    public String parseVideoUrl(String videoUrl){
        try {
            String html_response = SimpleHttpClient.GET(videoUrl, Util.USER_AGENT_FIREFOX, HTTP_TIMEOUT);

            if (html_response != null) {
                int index = html_response.indexOf(TAG_URL_SEARCH);
                if (index > 0) {
                    html_response = html_response.substring(index);

                    videoUrl = Util.Html.getTag(html_response, TAG_URL_START, TAG_URL_END);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        return videoUrl;
    }
}
