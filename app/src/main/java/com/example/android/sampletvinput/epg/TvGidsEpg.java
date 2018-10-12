package com.example.android.sampletvinput.epg;

import android.util.Log;

import com.example.android.sampletvinput.util.SimpleHttpClient;
import com.google.android.media.tv.companionlibrary.model.InternalProviderData;
import com.google.android.media.tv.companionlibrary.model.Program;
import com.google.android.media.tv.companionlibrary.utils.TvContractUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class TvGidsEpg {
    private static final String TAG = "TvGidsEpg";
    private static final String EPG_URL_IDENTIFIER = "tvgids.nl";
    private static final String EPG_URL_EXTENSION = "&day=";
    private static final String[] EPG_URL_EXTENSION_DAYS = {"0", "1"};

    private static final String EPG_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final long MS_IN_HALF_HOUR = 30 * 60 * 1000;
    private static final long MS_IN_ONE_HOUR = 2 * MS_IN_HALF_HOUR;

    public static String getEpgUrlIdentifier() {
        return EPG_URL_IDENTIFIER;
    }

    public static List<Program> getAllPrograms(String channelNumber, String channelName, String videoUrl, String logoUrl, String epgUrl) {
        List<Program> programs = new ArrayList<>();

        for (int i=0; i < EPG_URL_EXTENSION_DAYS.length; i++) {
            programs.addAll(getAllProgramsExt(channelNumber, channelName, videoUrl, logoUrl,
                    epgUrl+ EPG_URL_EXTENSION + EPG_URL_EXTENSION_DAYS[i]));
        }

        return programs;
    }

    private static List<Program> getAllProgramsExt(String channelNumber, String channelName, String videoUrl, String logoUrl, String epgUrl) {
        String channelId = channelNumber + "-" + channelName;
        InternalProviderData internalProviderData = new InternalProviderData();
        internalProviderData.setVideoType(TvContractUtils.SOURCE_TYPE_HLS);
        internalProviderData.setVideoUrl(videoUrl);
        List<Program> programs = new ArrayList<>();

        if (epgUrl != null) {
            try {
                String epgString = SimpleHttpClient.GET(epgUrl);

                if(epgString != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(epgString);

                        String key = jsonObject.keys().next();
                        Object obj = jsonObject.get(key);

                        if(obj instanceof JSONArray) {
                            JSONArray jsonArray = jsonObject.getJSONArray(key);

                            for (int i = 0; i < jsonArray.length(); i++) {
                                Program program = createProgram(channelId, logoUrl, internalProviderData, jsonArray.getJSONObject(i));

                                if(program != null) {
                                    programs.add(program);
                                }
                            }

                        } else {
                            jsonObject = jsonObject.getJSONObject(key);

                            Iterator<String> iterator = jsonObject.keys();

                            while (iterator.hasNext()) {
                                Program program = createProgram(channelId, logoUrl, internalProviderData, jsonObject.getJSONObject(iterator.next()));

                                if(program != null) {
                                    programs.add(program);
                                }
                            }
                        }

                    } catch (JSONException e) {
                        Log.e(TAG, "JsonEpg parsing error: " + e.getMessage());
                    }
                }

            } catch (IOException e) {
                Log.e(TAG, "EPG Url GET failed: " + e.getMessage());
            }
        }

        return programs;
    }

    private static Program createProgram(String channelId, String logoUrl, InternalProviderData internalProviderData, JSONObject jsonObject) throws JSONException
    {
        Program program = null;
        String epgProgramTitle = jsonObject.getString("titel");
        String epgStartTime = jsonObject.getString("datum_start");
        String epgEndTime = jsonObject.getString("datum_end");

        DateFormat dateFormat = new SimpleDateFormat(EPG_DATE_FORMAT);

        long epgStartTimeMs = 0;
        long epgEndTimeMs = 0;

        try {
            Date date = dateFormat.parse(epgStartTime);
            epgStartTimeMs = date.getTime();

            date = dateFormat.parse(epgEndTime);
            epgEndTimeMs = date.getTime();
        } catch (ParseException e) {
            Log.e(TAG, e.getMessage());
        }

        if (epgStartTimeMs == 0) {
            epgStartTimeMs = System.currentTimeMillis();
            epgEndTimeMs = epgStartTimeMs + MS_IN_ONE_HOUR;
        }

        program = new Program.Builder()
                .setChannelId(channelId.hashCode())
                .setTitle(epgProgramTitle)
                .setPosterArtUri(logoUrl)
                .setStartTimeUtcMillis(epgStartTimeMs)
                .setEndTimeUtcMillis(epgEndTimeMs)
                .setInternalProviderData(internalProviderData)
                .build();

        return program;
    }
}
