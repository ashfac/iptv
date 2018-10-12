package com.example.android.sampletvinput.parsers;

import android.util.Log;

import com.example.android.sampletvinput.util.JavaScriptUnpacker;
import com.example.android.sampletvinput.util.SimpleHttpClient;

import java.io.IOException;

public class ArconaiTvParser extends VideoUrlParser {
    private static String TAG = "ArconaiTvParser";
    private static final String PARSER_ID = "arconaitv";

    @Override
    public String getParserId() {
        return PARSER_ID;
    }

    @Override
    public String parseVideoUrl(String videoUrl){
        try {
            String html_response = SimpleHttpClient.GET(videoUrl);

            String evalFunc = extractEvalFunc(html_response);
            videoUrl = extractVideoUrl(evalFunc);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }

        return videoUrl;
    }

    private static String extractEvalFunc(String data)
    {
        String evalFunc = data.substring(data.indexOf("eval(function(p,a,c,k,e,d)"));
        evalFunc = evalFunc.substring(0, evalFunc.indexOf("{}))") + 4);

        return evalFunc;
    }

    private static String extractVideoUrl(String evalFunc)
    {
        String videoUrl = JavaScriptUnpacker.unpack(evalFunc);

        if(videoUrl != null) {
            videoUrl = videoUrl.substring(videoUrl.indexOf("https"));
            videoUrl = videoUrl.substring(0, videoUrl.indexOf("\\"));
        }

        return videoUrl;
    }
}
