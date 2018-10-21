package com.example.android.sampletvinput.epg;

import android.util.Log;

import com.example.android.sampletvinput.util.SimpleHttpClient;
import com.example.android.sampletvinput.util.Util;
import com.google.android.media.tv.companionlibrary.model.InternalProviderData;
import com.google.android.media.tv.companionlibrary.model.Program;
import com.google.android.media.tv.companionlibrary.utils.TvContractUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class Urdu1Epg {
    private static final String TAG = "Urdu1Epg";
    private static final String EPG_URL_IDENTIFIER = "urdu1.tv";

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
                String epgString = SimpleHttpClient.GET(epgUrl, Util.USER_AGENT_FIREFOX);

                if(epgString.length() > 0) {
                    List<String> weeklySchedule = new ArrayList<String>(Arrays.asList(epgString.split("class=\"slider")));

                    if(weeklySchedule.size() == 9) {
                        weeklySchedule.remove(8);
                        weeklySchedule.remove(0);

                        Calendar calendar = Calendar.getInstance();

                        // make Monday first day of week
                        int startDay = ((calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7);

                        long timeCorrection = Util.DateTime.getMidnightTimeMsUtc() + TIME_ZONE_CORRECTION;

                        String day_tag = weeklySchedule.get(startDay).substring(0, 3);

                        for (int i = 0; i < weeklySchedule.size(); i++) {
                            int currentDay = (startDay + i) % weeklySchedule.size();

                            if(!weeklySchedule.get(currentDay).startsWith(day_tag)) {
                                timeCorrection += MS_IN_24_HOUR;
                                day_tag = weeklySchedule.get(currentDay).substring(0, 3);
                            }

                            String[] dailySchedule = weeklySchedule.get(currentDay).split("class=\"slide\">");

                            if(dailySchedule.length > 1) {
                                for (int index = 1; index < dailySchedule.length; index++) {
                                    Program program = createProgram(channelId, logoUrl, internalProviderData,
                                            dailySchedule[index].replace("\r", "").replace("\n", ""),
                                            (index == dailySchedule.length - 1) ? null : dailySchedule[index + 1].replace("\r", "").replace("\n", ""),
                                            timeCorrection);

                                    if (program != null) {
                                        programs.add(program);
                                    }
                                }
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

        String epgProgramTitle = Util.Html.getTag(currentProgram, "<h2>", "</h2>");
        String epgEpisode = Util.Html.getTag(currentProgram, "<h4>", "</h4>");
        String epgLogo = Util.Html.getTag(currentProgram, "<img src=\"", "\"");
        String epgStartTime = Util.Html.getTagReverse(currentProgram, ">", "</h3>");
        String epgEndTime = (nextProgram == null) ? "12:00 AM" : Util.Html.getTagReverse(nextProgram, ">", "</h3>");

        if(epgLogo == null || epgLogo.startsWith("//")) {
            epgLogo = "https:" + epgLogo;
        }

        if(epgStartTime == null || epgEndTime == null) {
            epgStartTime = "12:00 AM";
            epgEndTime =   "12:00 AM";
        } else {
            epgStartTime = epgStartTime.substring(epgStartTime.indexOf(">")+1);
            epgEndTime = epgEndTime.substring(epgEndTime.indexOf(">")+1);
        }

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
                    .setSeasonTitle(epgEpisode)
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
        if(timeHHmm == null || timeHHmm.length() < 1) {
            // invalid time
            return 0;
        }

        timeHHmm = timeHHmm.replace(" ", "").replace("\n", "");

        String[] timeNow = timeHHmm.split(":");

        if(timeNow.length < 2) {
            // invalid time
            return 0;
        }

        long time = (60 * Long.parseLong(timeNow[0]) )
                + Long.parseLong(timeNow[1].substring(0, 2));

        time = (1000 *60 * time);

        if(timeNow[1].toLowerCase().contains("pm")) {
            if(Long.parseLong(timeNow[0]) != 12) {
                time += MS_IN_12_HOUR;
            }
        } else {
            if(Long.parseLong(timeNow[0]) == 12) {
                time -= MS_IN_12_HOUR;
            }
        }

        return time;
    }
}
