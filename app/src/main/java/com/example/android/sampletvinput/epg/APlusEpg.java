package com.example.android.sampletvinput.epg;

import android.util.Log;

import com.example.android.sampletvinput.util.SimpleHttpClient;
import com.example.android.sampletvinput.util.Util;
import com.google.android.media.tv.companionlibrary.model.InternalProviderData;
import com.google.android.media.tv.companionlibrary.model.Program;
import com.google.android.media.tv.companionlibrary.utils.TvContractUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class APlusEpg {
    private static final String TAG = "APlusEpg";
    private static final String EPG_URL_IDENTIFIER = "a-plus.tv";
    private static final String[] DAYS_OF_WEEK = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

    private static final long MS_IN_ONE_HOUR = 60 * 60 * 1000;
    private static final long MS_IN_12_HOUR = 12 * MS_IN_ONE_HOUR;
    private static final long MS_IN_24_HOUR = 24 * MS_IN_ONE_HOUR;
    private static final long TIME_ZONE_CORRECTION = - (5 * MS_IN_ONE_HOUR);

    private static long sEpgEndTimeLastMs = 0;

    public static String getEpgUrlIdentifier() {
        return EPG_URL_IDENTIFIER;
    }

    public static List<Program> getAllPrograms(String channelNumber, String channelName, String videoUrl, String logoUrl, String epgUrl) {
        String channelId = channelNumber + "-" + channelName;
        InternalProviderData internalProviderData = new InternalProviderData();
        internalProviderData.setVideoType(TvContractUtils.SOURCE_TYPE_HLS);
        internalProviderData.setVideoUrl(videoUrl);
        List<Program> programs = new ArrayList<>();

        if (epgUrl != null) {
            try {
                String epgString = SimpleHttpClient.GET(epgUrl);

                if(epgString.length() > 0) {
                    String[] dailySchedule = epgString.split("<div class=\"cbp-item ");

                    int startIndex = 1;
                    if(dailySchedule.length > 1) {
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEEE");
                        Date date = new Date();
                        String currentDay = simpleDateFormat.format(date).toLowerCase();

                        while (!dailySchedule[startIndex].startsWith(currentDay)) {
                            startIndex++;
                        }

                        long timeCorrection = ((System.currentTimeMillis() / MS_IN_24_HOUR) * MS_IN_24_HOUR) + TIME_ZONE_CORRECTION;

                        for (int i = 0; i < dailySchedule.length-1; i++) {
                            int index = (startIndex + i) % dailySchedule.length;

                            if(index == 0) { index++; }

                            if(!dailySchedule[index].startsWith(currentDay)) {
                                timeCorrection += MS_IN_24_HOUR;
                                currentDay = dailySchedule[index].substring(0, dailySchedule[index].indexOf('\"'));
                            }

                            Program program = createProgram(channelId, logoUrl, internalProviderData,
                                    dailySchedule[index],
                                    ((index == dailySchedule.length - 1) ||
                                            ((index < dailySchedule.length - 1) && !dailySchedule[index+1].startsWith(currentDay)) )
                                            ? null : dailySchedule[index + 1],
                                    timeCorrection);

                            if (program != null) {
                                programs.add(program);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "EPG Url GET failed: " + e.getMessage());
            }
        }

        return programs;
    }

    private static Program createProgram(String channelId,
                                         String logoUrl,
                                         InternalProviderData internalProviderData,
                                         String currentProgram,
                                         String nextProgram,
                                         long timeCorrection) {

        String epgProgramTitle = Util.Html.getTag(currentProgram, "title\">", "</div>");
        String epgLogo = Util.Html.getTag(currentProgram, "<img src=\"", "\"");
        String epgStartTime = Util.Html.getTag(currentProgram, "desc\">", "</div>");
        String epgEndTime = (nextProgram == null) ? "24:00" : Util.Html.getTag(nextProgram, "desc\">", "</div>");

        long epgStartTimeMs = convertTimeMs(epgStartTime) + timeCorrection;
        long epgEndTimeMs = convertTimeMs(epgEndTime) + timeCorrection;

        if( (sEpgEndTimeLastMs != 0)
                && (epgStartTimeMs > sEpgEndTimeLastMs)
                && ((epgStartTimeMs - sEpgEndTimeLastMs) > (2 * MS_IN_ONE_HOUR))) {
            epgStartTimeMs = sEpgEndTimeLastMs;
        }

        if( (epgEndTimeMs < epgStartTimeMs) || ((epgEndTimeMs - epgStartTimeMs) > MS_IN_12_HOUR) ) {
            epgEndTimeMs = epgStartTimeMs + MS_IN_ONE_HOUR;
        }

        if(nextProgram == null) {
            sEpgEndTimeLastMs = 0;
        } else {
            sEpgEndTimeLastMs = epgEndTimeMs;
        }

        Program program = null;
        try {
            program = new Program.Builder()
                    .setChannelId(channelId.hashCode())
                    .setTitle(epgProgramTitle)
                    .setDescription(" ")
                    .setPosterArtUri(epgLogo)
                    .setStartTimeUtcMillis(epgStartTimeMs)
                    .setEndTimeUtcMillis(epgEndTimeMs)
                    .setInternalProviderData(internalProviderData).build();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, e.getMessage());
        }

        return program;

    }

    private static long convertTimeMs(String timeHHmm) {
        timeHHmm = timeHHmm.replace(" ", "");

        String[] timeNow = timeHHmm.split(":");

        long time = (60 * Long.parseLong(timeNow[0]) )
                + Long.parseLong(timeNow[1].substring(0, 2));

        time = (1000 *60 * time);

        if(timeNow[1].substring(timeNow[1].length()-2).toLowerCase().equals("pm")) {
            if(Long.parseLong(timeNow[0]) != 12) {
                time += MS_IN_12_HOUR;
            }
        }

        return time;
    }
}
