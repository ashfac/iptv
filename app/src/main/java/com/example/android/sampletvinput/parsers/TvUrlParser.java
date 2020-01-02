package com.example.android.sampletvinput.parsers;

import com.example.android.sampletvinput.feeds.M3U8Feed;
import com.example.android.sampletvinput.util.SimpleHttpClient;
import com.example.android.sampletvinput.util.Util;

public class TvUrlParser {
    private static VideoUrlParser[] videoUrlParserArray = {
              new YouTubeParser()
            , new MjunoonTvParser()
            , new ArconaiTvParser()
            , new UstreaMixParser()
            , new TvPcUsParser()
            , new EuroNewsParser()
            // , new DaCastParser()
            // , new FilmOnParser()
            // , new StreamLinkParser()
    };

    public static String parseVideoUrl(String videoUrl) {
        String videoUrlArray[] = videoUrl.split(M3U8Feed.EXT_URL_SEPARATOR);
        String returnUrl = null;

        for(String url : videoUrlArray) {
            returnUrl = null;
            boolean parserFound = false;
            for (VideoUrlParser videoUrlParser: videoUrlParserArray) {
                if(url.contains(videoUrlParser.getParserId())) {
                    parserFound = true;
                    returnUrl = videoUrlParser.parseVideoUrl(url);
                    break;
                }
            }

            if(parserFound == true) {
                return returnUrl;
            } else {
                returnUrl = url;
            }
        }

        return returnUrl;
    }
}
