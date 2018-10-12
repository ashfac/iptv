package com.example.android.sampletvinput.epg;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.example.android.sampletvinput.feeds.M3U8Feed;
import com.example.android.sampletvinput.util.Util;
import com.google.android.media.tv.companionlibrary.model.InternalProviderData;
import com.google.android.media.tv.companionlibrary.model.Program;
import com.google.android.media.tv.companionlibrary.utils.TvContractUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class JsonEpg {
    private static final String TAG = "JsonEpg";
    private static final String EPG_URL_IDENTIFIER = "file:";

    private static final String[] DAYS_OF_WEEK = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

    public static String getEpgUrlIdentifier() {
        return EPG_URL_IDENTIFIER;
    }

    public static List<Program> getAllPrograms(Context context, String channelNumber, String channelName, String videoUrl, String logoUrl, String epgUrl) {
        String channelId = channelNumber + "-" + channelName;
        InternalProviderData internalProviderData = new InternalProviderData();
        internalProviderData.setVideoType(TvContractUtils.SOURCE_TYPE_HLS);
        internalProviderData.setVideoUrl(videoUrl);
        List<Program> programs = new ArrayList<>();

        epgUrl = epgUrl.replace("file:", Util.FileSystem.getEpgDirectory(context));
        File file = new File(epgUrl);
        if(file.exists()) {
            Uri uri = Uri.parse("file://" + epgUrl);

            InputStream inputStream = null;
            try {
                inputStream = new BufferedInputStream(context.getContentResolver().openInputStream(uri));
                String epgString = Util.StringHelper.StreamtoString(inputStream);

                if(inputStream != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(epgString);

                        String dayOfTheWeek = Util.DateTime.getCurrentDayLong();

                        int startDay = 0;
                        while(startDay < DAYS_OF_WEEK.length) {
                            if(dayOfTheWeek.contentEquals(DAYS_OF_WEEK[startDay])) {
                                break;
                            }

                            startDay++;
                        }

                        for (int currentDay = 0; currentDay < DAYS_OF_WEEK.length; currentDay++) {
                            String epgDay = DAYS_OF_WEEK[(startDay + currentDay) % DAYS_OF_WEEK.length];

                            long timeCorrection = Util.DateTime.getMidnightTimeMsUtc()
                                    + convertTimeZoneMs(jsonObject.getString("timezone"))
                                    + (currentDay * Util.DateTime.MS_IN_24_HOUR);

                            JSONArray jsonArray = jsonObject.getJSONArray("programs");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonProgram = jsonArray.getJSONObject(i);

                                JSONArray days = jsonProgram.getJSONArray("days");

                                if(containsEpgDay(days, epgDay)) {
                                    Program program = createProgram(channelId, logoUrl, internalProviderData,
                                            jsonProgram, timeCorrection);

                                    if (program != null) {
                                        programs.add(program);
                                    }
                                }
                            }
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JsonEpg parsing error: " + e.getMessage());
                    }
                }
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Error in fetching " + uri, e);
            } catch (IOException e) {
                Log.e(TAG, "Error: " + e.getMessage());
            }
        }

        return programs;
    }

    private static Program createProgram(String channelId,
                                         String logoUrl,
                                         InternalProviderData internalProviderData,
                                         JSONObject jsonObject,
                                         long timeCorrection) throws JSONException {

        String epgProgramTitle = jsonObject.getString("name");
        String epgStartTime = jsonObject.getString("starttime");
        String epgEndTime = jsonObject.getString("endtime");
        String epgDescription = " ";
        String epgLogo = logoUrl;

        if(jsonObject.has("summary")) {
            epgDescription = jsonObject.getString("summary");
        }

        if(jsonObject.has("poster")) {
            epgLogo = jsonObject.getString("poster");
        }

        long epgStartTimeMs = convertTimeMs(epgStartTime) + timeCorrection;
        long epgEndTimeMs = convertTimeMs(epgEndTime) + timeCorrection;

        Program program = null;
        try {
            program = new Program.Builder()
                    .setChannelId(channelId.hashCode())
                    .setTitle(epgProgramTitle)
                    .setDescription(epgDescription)
                    .setPosterArtUri(epgLogo)
                    .setStartTimeUtcMillis(epgStartTimeMs)
                    .setEndTimeUtcMillis(epgEndTimeMs)
                    .setInternalProviderData(internalProviderData).build();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, e.getMessage());
        }

        return program;

    }

    private static long convertTimeZoneMs(String timeHHmm) {
        // format: +05:00
        long addSub = (timeHHmm.charAt(0) == '+') ? -1 : +1;

        return (addSub * convertTimeMs(timeHHmm.substring(1)));
    }

    private static long convertTimeMs(String timeHHmm) {
        String[] timeStr = timeHHmm.split(":");
        long time = (60 * Long.parseLong(timeStr[0]) )
                + Long.parseLong(timeStr[1]);

        return (1000 *60 * time);
    }

    private static boolean containsEpgDay(JSONArray jsonDays, String epgDay) {
        if(jsonDays.length() == DAYS_OF_WEEK.length) {
            return true;
        } else {
            for (int i=0; i < jsonDays.length(); i++) {
                try {
                    if(epgDay.equals(jsonDays.get(i))) {
                        return true;
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "containsEpgDay: " + e.getMessage());
                }
            }
        }

        return false;
    }
}

