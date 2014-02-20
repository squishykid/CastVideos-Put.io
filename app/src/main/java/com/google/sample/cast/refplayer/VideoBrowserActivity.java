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

package com.google.sample.cast.refplayer;

import com.google.sample.cast.refplayer.settings.CastPreference;
import com.google.sample.castcompanionlibrary.cast.VideoCastManager;
import com.google.sample.castcompanionlibrary.cast.callbacks.IVideoCastConsumer;
import com.google.sample.castcompanionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.google.sample.castcompanionlibrary.widgets.MiniController;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

public class VideoBrowserActivity extends ActionBarActivity {

    private static final String TAG = "VideoBrowserActivity";
    private VideoCastManager mCastManager;
    private IVideoCastConsumer mCastConsumer;
    private MiniController mMini;
    private MenuItem mediaRouteMenuItem;

    SharedPreferences sharedPreferences;
    int requestCode;

    /*
     * (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VideoCastManager.checkGooglePlaySevices(this);
        setContentView(R.layout.video_browser);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!sharedPreferences.getBoolean("loggedIn", false)) {
            Intent setupIntent = new Intent(this, Setup.class);
            startActivityForResult(setupIntent, requestCode);
        }

        ActionBar actionBar = getSupportActionBar();

        mCastManager = CastApplication.getCastManager(this);

        // -- Adding MiniController
        mMini = (MiniController) findViewById(R.id.miniController1);
        mCastManager.addMiniController(mMini);

        mCastConsumer = new VideoCastConsumerImpl() {

            @Override
            public void onFailed(int resourceId, int statusCode) {

            }

            @Override
            public void onConnectionSuspended(int cause) {
                Log.d(TAG, "onConnectionSuspended() was called with cause: " + cause);
                com.google.sample.cast.refplayer.utils.Utils.
                        showToast(VideoBrowserActivity.this, R.string.connection_temp_lost);
            }

            @Override
            public void onConnectivityRecovered() {
                com.google.sample.cast.refplayer.utils.Utils.
                        showToast(VideoBrowserActivity.this, R.string.connection_recovered);
            }

            @Override
            public void onCastDeviceDetected(final RouteInfo info) {
                if (!CastPreference.isFtuShown(VideoBrowserActivity.this)) {
                    CastPreference.setFtuShown(VideoBrowserActivity.this);

                    Log.d(TAG, "Route is visible: " + info);
                    new Handler().postDelayed(new Runnable() {

                        @Override
                        public void run() {
                            if (mediaRouteMenuItem.isVisible()) {
                                Log.d(TAG, "Cast Icon is visible: " + info.getName());
                                showFtu();
                            }
                        }
                    }, 1000);
                }
            }
        };

        setupActionBar(actionBar);
        mCastManager.reconnectSessionIfPossible(this, false);
    }

    private void setupActionBar(ActionBar actionBar) {
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        getSupportActionBar().setIcon(R.drawable.actionbar_logo_castvideos);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);

        mediaRouteMenuItem = mCastManager.
                addMediaRouterButton(menu, R.id.media_route_menu_item);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent i = new Intent(VideoBrowserActivity.this, CastPreference.class);
                startActivity(i);
                break;
            case R.id.action_logout:
                sharedPreferences.edit().remove("token").remove("loggedIn").commit();
                finish();
                break;
        }
        return true;
    }

    private void showFtu() {
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!mCastManager.isConnected()) {
            return super.onKeyDown(keyCode, event);
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            changeVolume(CastApplication.VOLUME_INCREMENT);
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            changeVolume(-CastApplication.VOLUME_INCREMENT);
        } else {
            return super.onKeyDown(keyCode, event);
        }
        return true;
    }

    private void changeVolume(double volumeIncrement) {
        if (mCastManager == null) {
            return;
        }
        try {
            mCastManager.incrementVolume(volumeIncrement);
        } catch (Exception e) {
            Log.e(TAG, "onVolumeChange() Failed to change volume", e);
            com.google.sample.cast.refplayer.utils.Utils.handleException(this, e);
        }
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume() was called");
        mCastManager = CastApplication.getCastManager(this);
        if (null != mCastManager) {
            mCastManager.addVideoCastConsumer(mCastConsumer);
            mCastManager.incrementUiCounter();
        }

        super.onResume();
    }

    @Override
    protected void onPause() {
        mCastManager.decrementUiCounter();
        mCastManager.removeVideoCastConsumer(mCastConsumer);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (null != mCastManager) {
            mMini.removeOnMiniControllerChangedListener(mCastManager);
        }
        super.onDestroy();
    }

}
