/*
 * Copyright 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.sampletvinput;

import android.net.Uri;
import android.nfc.Tag;
import android.util.Log;

import com.example.android.sampletvinput.feeds.ArconaiTvFeed;
import com.example.android.sampletvinput.feeds.M3U8Feed;
import com.example.android.sampletvinput.rich.RichFeedUtil;

import com.google.android.media.tv.companionlibrary.model.Channel;
import com.google.android.media.tv.companionlibrary.model.Program;
import com.google.android.media.tv.companionlibrary.EpgSyncJobService;
import com.google.android.media.tv.companionlibrary.XmlTvParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * EpgSyncJobService that periodically runs to update channels and programs.
 */
public class SampleJobService extends EpgSyncJobService {
    private static boolean ENABLE_ARCONAI_CHANNELS = false;

    @Override
    public void resetTvListings() {
        RichFeedUtil.resetTvListings();
    }

    @Override
    public List<Channel> getChannels() {
        // Add channels through an XMLTV file
        XmlTvParser.TvListing listings = RichFeedUtil.getRichTvListings(this);
        List<Channel> channelList = new ArrayList<>();

        if(listings != null) {
            channelList.addAll(listings.getChannels());
        }

        // Add channels through an m3u8 file
        channelList.addAll(RichFeedUtil.getM3u8Listings(this));

        // add arconai tv channels
        if (ENABLE_ARCONAI_CHANNELS) {
            try {
                ArconaiTvFeed.init();
                channelList.addAll(ArconaiTvFeed.getChannels());
            } catch (IOException e) {
                Log.e("", "failed to initialize arconai tv feed");
            }
        }

        return channelList;
    }

    @Override
    public List<Program> getProgramsForChannel(Uri channelUri, Channel channel, long startMs,
            long endMs) {

        List<Program> programs = null;
        XmlTvParser.TvListing listings = RichFeedUtil.getRichTvListings(getApplicationContext());

        if(listings != null) {
            programs = listings.getPrograms(channel);
        }

        if(programs == null || programs.size() == 0) {
            if (ArconaiTvFeed.isAcronaiChannel(channel)) {
                programs = ArconaiTvFeed.getProgramsForChannel(channel);
            } else {
                programs = M3U8Feed.getProgramsForChannel(getApplicationContext(), channel, startMs, endMs);
            }
        }

        return programs;
    }
}
