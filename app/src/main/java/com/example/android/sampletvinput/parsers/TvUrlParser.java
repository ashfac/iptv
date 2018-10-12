package com.example.android.sampletvinput.parsers;

public class TvUrlParser {
    private static VideoUrlParser[] videoUrlParserArray = {
            new YouTubeParser(),
            new MjunoonTvParser(),
            new EuroNewsParser(),
            new DaCastParser(),
            new ArconaiTvParser()
    };

    public static String parseVideoUrl(String videoUrl) {
        for (VideoUrlParser videoUrlParser: videoUrlParserArray) {
            if(videoUrl.contains(videoUrlParser.getParserId())) {
                return videoUrlParser.parseVideoUrl(videoUrl);
            }
        }

        return videoUrl;
    }
}
