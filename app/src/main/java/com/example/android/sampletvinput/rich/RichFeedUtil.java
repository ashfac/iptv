/*
 * Copyright 2015 The Android Open Source Project
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

package com.example.android.sampletvinput.rich;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.example.android.sampletvinput.R;
import com.example.android.sampletvinput.feeds.M3U8Feed;
import com.example.android.sampletvinput.util.Util;
import com.google.android.media.tv.companionlibrary.XmlTvParser;
import com.google.android.media.tv.companionlibrary.model.Channel;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

/**
 * Static helper methods for fetching the channel feed.
 */
public class RichFeedUtil {
    private static final String TAG = "RichFeedUtil";

    // A key for the channel display number used in the app link intent from the xmltv_feed.
    public static final String EXTRA_DISPLAY_NUMBER = "display-number";

    private static XmlTvParser.TvListing sSampleTvListing;

    private static List<Channel> sM3u8Channels;

    private static final int URLCONNECTION_CONNECTION_TIMEOUT_MS = 3000;  // 3 sec
    private static final int URLCONNECTION_READ_TIMEOUT_MS = 10000;  // 10 sec

    private RichFeedUtil() {
    }

    public static void resetTvListings() {
        sSampleTvListing = null;
        sM3u8Channels = null;
    }

    public static XmlTvParser.TvListing getRichTvListings(Context context) {
        if (sSampleTvListing != null) {
            return sSampleTvListing;
        }

        String xmlFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/iptv_channels.xml";
        File file = new File(xmlFile);
        if(file.exists()) {
            Uri catalogUri = Uri.parse("file://" + xmlFile);

            try (InputStream inputStream = getInputStream(context, catalogUri)) {
                sSampleTvListing = XmlTvParser.parse(inputStream);
            } catch (IOException e) {
                Log.e(TAG, "Error in fetching " + catalogUri, e);
            } catch (XmlTvParser.XmlTvParseException e) {
                Log.e(TAG, "Error in parsing " + catalogUri, e);
            }
        }
        return sSampleTvListing;
    }

    public static List<Channel> getM3u8Listings(Context context) {
        if (sM3u8Channels != null) {
            return sM3u8Channels;
        }

        String playlistPath = Util.FileSystem.getPlaylistPath(context);
        File file = new File(playlistPath);
        if(file.exists()) {
            Uri catalogUri = Uri.parse("file://" + playlistPath);

            try (InputStream inputStream = getInputStream(context, catalogUri)) {
                sM3u8Channels = M3U8Feed.getChannels(context, inputStream);
            } catch (IOException e) {
                Log.e(TAG, "Error in fetching " + catalogUri, e);
            }
        }

        return sM3u8Channels;
    }

    public static InputStream getInputStream(Context context, Uri uri) throws IOException {
        InputStream inputStream = null;
        if (ContentResolver.SCHEME_ANDROID_RESOURCE.equals(uri.getScheme())
                || ContentResolver.SCHEME_ANDROID_RESOURCE.equals(uri.getScheme())
                || ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            try {
                inputStream = context.getContentResolver().openInputStream(uri);
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Error in fetching " + uri, e);
            }
        } else {
            URLConnection urlConnection = new URL(uri.toString()).openConnection();
            urlConnection.setConnectTimeout(URLCONNECTION_CONNECTION_TIMEOUT_MS);
            urlConnection.setReadTimeout(URLCONNECTION_READ_TIMEOUT_MS);
            inputStream = urlConnection.getInputStream();
        }

        return inputStream == null ? null : new BufferedInputStream(inputStream);
    }
}
