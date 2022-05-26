package com.example.android.sampletvinput.epg;

import android.content.Context;
import android.util.Log;

import com.example.android.sampletvinput.feeds.M3U8Feed;
import com.google.android.media.tv.companionlibrary.model.Channel;
import com.google.android.media.tv.companionlibrary.model.InternalProviderData;
import com.google.android.media.tv.companionlibrary.model.Program;
import com.google.android.media.tv.companionlibrary.utils.TvContractUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ElectronicProgramGuide {
    private static final String TAG = "ElectronicProgramGuide";

    private static final long MS_IN_ONE_HOUR = 60 * 60 * 1000;
    private static final long MS_IN_24_HOUR = 24 * MS_IN_ONE_HOUR;

    public static List<Program> getProgramsForChannel(Context context, Channel channel, long startMs, long endMs) {

        List<Program> programs = new ArrayList<>();

        InternalProviderData channelInternalProviderData = channel.getInternalProviderData();

        String epgUrl = null;
        String logoUrl = null;

        try {
            Object obj = channelInternalProviderData.get(M3U8Feed.EPG_URL);
            epgUrl = (obj == null) ? null : obj.toString();

            obj = channelInternalProviderData.get(M3U8Feed.EXT_LOGO);
            logoUrl = (obj == null) ? null : obj.toString();
        } catch (InternalProviderData.ParseException e) {
            Log.e(TAG, e.getMessage());
        }

        String channelNumber = channel.getDisplayNumber();
        String channelName = channel.getDisplayName();
        String videoUrl = channelInternalProviderData.getVideoUrl();
        String channelId = channelNumber + "-" + channelName;
        InternalProviderData programInternalProviderData = new InternalProviderData();
        programInternalProviderData.setVideoUrl(videoUrl);

        if(videoUrl.contains("iptv.wtf")) {
            programInternalProviderData.setVideoType(TvContractUtils.SOURCE_TYPE_MPEG_DASH);
        } else {
            programInternalProviderData.setVideoType(TvContractUtils.SOURCE_TYPE_HLS);
        }

        boolean fullSync = ( (endMs - startMs) > MS_IN_ONE_HOUR );

        if ( fullSync && (epgUrl != null) ) {
            if(epgUrl.contains(TvWizInEpg.getEpgUrlIdentifier())) {
                programs = TvWizInEpg.getAllPrograms(channelNumber, channelName, videoUrl, epgUrl);

            } else if(epgUrl.contains(TvWishEpg.getEpgUrlIdentifier())) {
                programs = TvWishEpg.getAllPrograms(channelNumber, channelName, videoUrl, logoUrl, epgUrl);

            } else if(epgUrl.contains(AlJazeeraEpg.getEpgUrlIdentifier())) {
                programs = AlJazeeraEpg.getAllPrograms(channelNumber, channelName, videoUrl, logoUrl, epgUrl);

            } else if(epgUrl.contains(TvGidsEpg.getEpgUrlIdentifier())) {
                programs = TvGidsEpg.getAllPrograms(channelNumber, channelName, videoUrl, logoUrl, epgUrl);

            } else if(epgUrl.contains(PtvEpg.getEpgUrlIdentifier())) {
                programs = PtvEpg.getAllPrograms(channelNumber, channelName, videoUrl, logoUrl, epgUrl);

            } else if(epgUrl.contains(HumTvEpg.getEpgUrlIdentifier())) {
                programs = HumTvEpg.getAllPrograms(channelNumber, channelName, videoUrl, logoUrl, epgUrl);

            } else if(epgUrl.contains(APlusEpg.getEpgUrlIdentifier())) {
                programs = APlusEpg.getAllPrograms(channelNumber, channelName, videoUrl, logoUrl, epgUrl);

            } else if(epgUrl.contains(Urdu1Epg.getEpgUrlIdentifier())) {
                programs = Urdu1Epg.getAllPrograms(channelNumber, channelName, videoUrl, logoUrl, epgUrl);

            } else if(epgUrl.contains(JsonEpg.getEpgUrlIdentifier())) {
                programs = JsonEpg.getAllPrograms(context, channelNumber, channelName, videoUrl, logoUrl, epgUrl);
            }
        }

        if(programs.size() == 0) {
            // for the first time sync & if there is no epg url set, it will end up here
            // add 24 Hrs long programs for 2 days if there is no epg url set or
            // if the epg url did not return any programs

            long startTimeMs;
            long endTimeMs;

            if(fullSync) {
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                startTimeMs = calendar.getTimeInMillis();
                endTimeMs = startTimeMs + MS_IN_24_HOUR;
            } else {
                startTimeMs = (System.currentTimeMillis() / MS_IN_ONE_HOUR) * MS_IN_ONE_HOUR;
                endTimeMs = startTimeMs + (2 * MS_IN_ONE_HOUR);
            }

            programs.add(createProgram(channelId, channelName, logoUrl, programInternalProviderData,
                    startTimeMs, endTimeMs));

            if(fullSync) {
                startTimeMs = endTimeMs;
                endTimeMs = startTimeMs + MS_IN_24_HOUR;

                programs.add(createProgram(channelId, channelName, logoUrl, programInternalProviderData,
                        startTimeMs, endTimeMs));
            }
        } else {
            // add filler programs where necessary

            // before first program
            long currentTimeMs = (System.currentTimeMillis() / MS_IN_ONE_HOUR) * MS_IN_ONE_HOUR;
            Program firstProgram = programs.get(0);
            if(currentTimeMs < firstProgram.getStartTimeUtcMillis()) {
                programs.add(0, createProgram(channelId, channelName, logoUrl, programInternalProviderData,
                        currentTimeMs, firstProgram.getStartTimeUtcMillis()));
            }

            // in between programs
            int lastIndex = programs.size() - 1;
            for (int i=1; i < lastIndex; i++) {
                Program currentProgram = programs.get(i);
                Program nextProgram = programs.get(i+1);

                if(nextProgram.getStartTimeUtcMillis() > currentProgram.getEndTimeUtcMillis()) {
                    i++;
                    programs.add(i, createProgram(channelId, channelName, logoUrl, programInternalProviderData,
                            currentProgram.getEndTimeUtcMillis(), nextProgram.getStartTimeUtcMillis()));
                    lastIndex++;
                }
            }

            //after last program
            Program lastProgram = programs.get(programs.size()-1);
            if(lastProgram.getEndTimeUtcMillis() < endMs) {
                programs.add(createProgram(channelId, channelName, logoUrl, programInternalProviderData,
                        lastProgram.getEndTimeUtcMillis(), endMs));
            }
        }

        return programs;
    }

    private static Program createProgram(String channelId,
                                         String channelName,
                                         String logoUrl,
                                         InternalProviderData programInternalProviderData,
                                         long startTimeMs,
                                         long endTimeMs) {
        return new Program.Builder()
                .setChannelId(channelId.hashCode())
                .setTitle(channelName)
                .setPosterArtUri(logoUrl)
                .setStartTimeUtcMillis(startTimeMs)
                .setEndTimeUtcMillis(endTimeMs)
                .setInternalProviderData(programInternalProviderData)
                .build();
    }
}
