/*
 * Copyright (C) 2013 Google Inc. All Rights Reserved.
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

package com.google.sample.cast.refplayer.browser;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.common.images.WebImage;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class VideoProvider {

    private static final String TAG = "VideoProvider";
    private static String TAG_MEDIA = "videos";
    private static String THUMB_PREFIX_URL =
            "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/";
    private static String TAG_CATEGORIES = "categories";
    private static String TAG_STUDIO = "studio";
    private static String TAG_SOURCES = "sources";
    private static String TAG_SUBTITLE = "subtitle";
    private static String TAG_THUMB = "image-480x270"; // "thumb";
    private static String TAG_IMG_780_1200 = "image-780x1200";
    private static String TAG_TITLE = "title";
    private static String TAG_FILES = "files";
    private static String TAG_NAME = "name";
    private static String TAG_SCREENSHOT = "screenshot";
    private static String TAG_ICON = "icon";
    private static String TAG_ID = "id";
    private static String TAG_MP4_AVAILABLE = "is_mp4_available";
    private static String BASE_URL = "https://api.put.io/v2/";
    private static String STREAM_MP4 = "/mp4/stream";
    private static String STREAM = "/stream";

    private static List<MediaInfo> mediaList;

    protected JSONObject parseUrl(String urlString, String tokenString) {
        InputStream is = null;
        try {
            java.net.URL url = new java.net.URL(urlString + tokenString);
            URLConnection urlConnection = url.openConnection();
            is = new BufferedInputStream(urlConnection.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    urlConnection.getInputStream(), "iso-8859-1"), 8);
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String json = sb.toString();
            return new JSONObject(json);
        } catch (Exception e) {
            Log.d(TAG, "Failed to parse the json for media list", e);
            return null;
        } finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    public static List<MediaInfo> buildMedia(String url, String token) throws JSONException {

        if (null != mediaList) {
            return mediaList;
        }
        mediaList = new ArrayList<MediaInfo>();
        JSONObject jsonObj = new VideoProvider().parseUrl(url, token);
        JSONArray files = jsonObj.getJSONArray(TAG_FILES);
        if (null != files) {
            for (int j = 0; j < files.length(); j++) {
                JSONObject video = files.getJSONObject(j);
                String subTitle = "ID: " + video.getString(TAG_ID);
                String videoUrl;
                if (video.getString(TAG_MP4_AVAILABLE) == "true") {
                    videoUrl = BASE_URL + "files/" + video.getString(TAG_ID) + STREAM_MP4 + "?oauth_token=" + token;
                } else {
                    videoUrl = BASE_URL + "files/" + video.getString(TAG_ID) + STREAM + "?oauth_token=" + token;
                }

                String imageurl = video.getString(TAG_ICON);
                String bigImageurl = video.getString(TAG_SCREENSHOT);
                String title = video.getString(TAG_NAME);
                String studio = "STUDIO";//video.getString(TAG_STUDIO);
                mediaList.add(buildMediaInfo(title, studio, subTitle, videoUrl, imageurl,
                        bigImageurl));

            }
        }
        return mediaList;
    }

    /*public static List<MediaInfo> buildMedia(String url) throws JSONException {

        if (null != mediaList) {
            return mediaList;
        }
        mediaList = new ArrayList<MediaInfo>();
        JSONObject jsonObj = new VideoProvider().parseUrl(url);
        JSONArray categories = jsonObj.getJSONArray(TAG_FILES);
        if (null != categories) {
            for (int i = 0; i < categories.length(); i++) {
                JSONObject category = categories.getJSONObject(i);
                category.getString(TAG_NAME);
                JSONArray videos = category.getJSONArray(getJsonMediaTag());
                if (null != videos) {
                    for (int j = 0; j < videos.length(); j++) {
                        JSONObject video = videos.getJSONObject(j);
                        String subTitle = video.getString(TAG_SUBTITLE);
                        JSONArray videoUrls = video.getJSONArray(TAG_SOURCES);
                        if (null == videoUrls || videoUrls.length() == 0) {
                            continue;
                        }
                        String videoUrl = videoUrls.getString(0);
                        String imageurl = getThumbPrefix() + video.getString(TAG_THUMB);
                        String bigImageurl = getThumbPrefix() + video.getString(TAG_IMG_780_1200);
                        String title = video.getString(TAG_TITLE);
                        String studio = video.getString(TAG_STUDIO);
                        mediaList.add(buildMediaInfo(title, studio, subTitle, videoUrl, imageurl,
                                bigImageurl));
                    }
                }
            }
        }
        return mediaList;
    }*/

    private static MediaInfo buildMediaInfo(String title,
            String subTitle, String studio, String url, String imgUrl, String bigImageUrl) {
        MediaMetadata movieMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);

        movieMetadata.putString(MediaMetadata.KEY_SUBTITLE, subTitle);
        movieMetadata.putString(MediaMetadata.KEY_TITLE, title);
        movieMetadata.putString(MediaMetadata.KEY_STUDIO, studio);
        movieMetadata.addImage(new WebImage(Uri.parse(imgUrl)));
        movieMetadata.addImage(new WebImage(Uri.parse(bigImageUrl)));

        return new MediaInfo.Builder(url)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType(getMediaType())
                .setMetadata(movieMetadata)
                .build();
    }

    private static String getMediaType() {
        return "video/mp4";
    }

    private static String getJsonMediaTag() {
        return TAG_MEDIA;
    }

    private static String getThumbPrefix() {
        return THUMB_PREFIX_URL;
    }
}
