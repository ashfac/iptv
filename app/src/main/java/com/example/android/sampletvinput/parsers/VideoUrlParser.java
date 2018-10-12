package com.example.android.sampletvinput.parsers;

public abstract class VideoUrlParser {
    public abstract String getParserId();

    public abstract String parseVideoUrl(String videoUrl);
}
