package com.example.android.sampletvinput.parsers;

import com.example.android.sampletvinput.feeds.M3U8Feed;
import com.example.android.sampletvinput.util.SimpleHttpClient;
import com.example.android.sampletvinput.util.Util;

public class TvUrlParser {
    private static VideoUrlParser[] videoUrlParserArray = {
            new YouTubeParser(),
            new MjunoonTvParser(),
            new EuroNewsParser(),
            new DaCastParser(),
            new ArconaiTvParser()
    };

    public static String parseVideoUrl(String videoUrl) {
        String videoUrlArray[] = videoUrl.split(M3U8Feed.EXT_URL_SEPARATOR);
        String returnUrl;

        for(String url : videoUrlArray) {
            returnUrl = null;
            for (VideoUrlParser videoUrlParser: videoUrlParserArray) {
                if(url.contains(videoUrlParser.getParserId())) {
                    returnUrl = videoUrlParser.parseVideoUrl(url);
                    break;
                }
            }

            if(returnUrl == null){
                returnUrl = url;
            }
            if(SimpleHttpClient.isValidUrl(returnUrl, Util.USER_AGENT_FIREFOX)) {
                return returnUrl;
            }
        }

        return null;
    }
}
