package com.spotifydemo2;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.ErrorCallback;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.Capabilities;
import com.spotify.protocol.types.Image;
import com.spotify.protocol.types.PlayerContext;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Repeat;

import java.util.List;
import java.util.Locale;

public class SpotifyRemoteModule extends ReactContextBaseJavaModule {

    private static final String TAG = SpotifyRemoteModule.class.getSimpleName();

    private static final String CLIENT_ID = "4f41ce2608e54d298790057aa271d9f9";
    private static final String REDIRECT_URI = "http://localhost:8888/callback";

    private static final String TRACK_URI = "spotify:track:4IWZsfEkaK49itBwCTFDXQ";
    private static final String ALBUM_URI = "spotify:album:4nZ5wPL5XxSY2OuDgbnYdc";
    private static final String ARTIST_URI = "spotify:artist:3WrFJ7ztbogyGnTHbHJFl2";
    private static final String PLAYLIST_URI = "spotify:playlist:37i9dQZEVXbMDoHDwVN2tF";
    private static final String PODCAST_URI = "spotify:show:2tgPYIeGErjk6irHRhk9kj";

    private static SpotifyAppRemote mSpotifyAppRemote;

    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    Button mConnectButton, mConnectAuthorizeButton;
    Button mSubscribeToPlayerContextButton;
    Button mPlayerContextButton;
    Button mSubscribeToPlayerStateButton;
    Button mPlayerStateButton;
    ImageView mCoverArtImageView;
    AppCompatTextView mImageLabel;
    AppCompatTextView mImageScaleTypeLabel;
    AppCompatImageButton mToggleShuffleButton;
    AppCompatImageButton mPlayPauseButton;
    AppCompatImageButton mToggleRepeatButton;
    AppCompatSeekBar mSeekBar;
    AppCompatImageButton mPlaybackSpeedButton;

    List<View> mViews;
    TrackProgressBar mTrackProgressBar;

    Subscription<PlayerState> mPlayerStateSubscription;
    Subscription<PlayerContext> mPlayerContextSubscription;
    Subscription<Capabilities> mCapabilitiesSubscription;

    private final ErrorCallback mErrorCallback = this::logError;


    SpotifyRemoteModule(ReactApplicationContext context) {
        super(context);
    }

    @NonNull
    @Override
    public String getName() {
            return "SpotifyRemoteModule";
        }
    @RequiresApi(api = Build.VERSION_CODES.M)
    @ReactMethod
    private void connect(boolean showAuthView) {
        SpotifyAppRemote.disconnect(mSpotifyAppRemote);
        SpotifyAppRemote.connect(
                getReactApplicationContext(),
                new ConnectionParams.Builder(CLIENT_ID)
                        .setRedirectUri(REDIRECT_URI)
                        .showAuthView(showAuthView)
                        .build(),
                new Connector.ConnectionListener() {
                    @Override
                    public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                        mSpotifyAppRemote = spotifyAppRemote;
                        //SpotifyRemoteModule.this.onConnected();
                        Toast.makeText(getReactApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(Throwable error) {
                        logError(error);
                        //SpotifyRemoteModule.this.onDisconnected();
                    }
                });
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    @ReactMethod
    public void onPlayPauseButtonClicked(   Promise promise) {
        mSpotifyAppRemote
                .getPlayerApi()
                .getPlayerState()
                .setResultCallback(
                        playerState -> {
                            if (playerState.isPaused) {
                                mSpotifyAppRemote
                                        .getPlayerApi()
                                        .resume()
                                        .setResultCallback(
                                                empty -> logMessage(getReactApplicationContext().getString(R.string.command_feedback, "play")))
                                        .setErrorCallback(mErrorCallback);
                                try {

                                    promise.resolve(playerState.track.name  );
                                } catch(Exception e) {
                                    promise.reject("Create Event Error", e);
                                }
                            } else {
                                mSpotifyAppRemote
                                        .getPlayerApi()
                                        .pause()
                                        .setResultCallback(
                                                empty -> logMessage(getReactApplicationContext().getString(R.string.command_feedback, "pause")))
                                        .setErrorCallback(mErrorCallback);
                            }
                        });
    }

    public void onSubscribedToPlayerStateButtonClicked(View view) {

        if (mPlayerStateSubscription != null && !mPlayerStateSubscription.isCanceled()) {
            mPlayerStateSubscription.cancel();
            mPlayerStateSubscription = null;
        }

        mPlayerStateButton.setVisibility(View.VISIBLE);
        mSubscribeToPlayerStateButton.setVisibility(View.INVISIBLE);

        mPlayerStateSubscription =
                (Subscription<PlayerState>)
                        mSpotifyAppRemote
                                .getPlayerApi()
                                .subscribeToPlayerState()
                                .setEventCallback(mPlayerStateEventCallback)
                                .setLifecycleCallback(
                                        new Subscription.LifecycleCallback() {
                                            @Override
                                            public void onStart() {
                                                logMessage("Event: start");
                                            }

                                            @Override
                                            public void onStop() {
                                                logMessage("Event: end");
                                            }
                                        })
                                .setErrorCallback(
                                        throwable -> {
                                            mPlayerStateButton.setVisibility(View.INVISIBLE);
                                            mSubscribeToPlayerStateButton.setVisibility(View.VISIBLE);
                                            logError(throwable);
                                        });
    }

    private final Subscription.EventCallback<PlayerState> mPlayerStateEventCallback =
            new Subscription.EventCallback<PlayerState>() {
                @Override
                public void onEvent(PlayerState playerState) {
                    if (playerState.track != null) {
                        // Get image from track
                        mSpotifyAppRemote
                                .getImagesApi()
                                .getImage(playerState.track.imageUri, Image.Dimension.LARGE)
                                .setResultCallback(
                                        bitmap -> {
                                            mCoverArtImageView.setImageBitmap(bitmap);
                                            mImageLabel.setText(
                                                    String.format(
                                                            Locale.ENGLISH, "%d x %d", bitmap.getWidth(), bitmap.getHeight()));
                                        });
                        // Invalidate seekbar length and position

                    }

                }
            };


    private void logError(Throwable throwable) {
        Toast.makeText(getReactApplicationContext(), R.string.err_generic_toast, Toast.LENGTH_SHORT).show();
        Log.e(TAG, "", throwable);
    }
    private void logMessage(String msg) {
        logMessage(msg, Toast.LENGTH_SHORT);
    }

    private void logMessage(String msg, int duration) {
        Toast.makeText(getReactApplicationContext(), msg, duration).show();
        Log.d(TAG, msg);
    }



    private class TrackProgressBar {

        private static final int LOOP_DURATION = 500;
        private final SeekBar mSeekBar;
        private final Handler mHandler;

        private final SeekBar.OnSeekBarChangeListener mSeekBarChangeListener =
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        mSpotifyAppRemote
                                .getPlayerApi()
                                .seekTo(seekBar.getProgress())
                                .setErrorCallback(mErrorCallback);
                    }
                };

        private final Runnable mSeekRunnable =
                new Runnable() {
                    @Override
                    public void run() {
                        int progress = mSeekBar.getProgress();
                        mSeekBar.setProgress(progress + LOOP_DURATION);
                        mHandler.postDelayed(mSeekRunnable, LOOP_DURATION);
                    }
                };

        private TrackProgressBar(SeekBar seekBar) {
            mSeekBar = seekBar;
            mSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
            mHandler = new Handler();
        }

        private void setDuration(long duration) {
            mSeekBar.setMax((int) duration);
        }

        private void update(long progress) {
            mSeekBar.setProgress((int) progress);
        }

        private void pause() {
            mHandler.removeCallbacks(mSeekRunnable);
        }

        private void unpause() {
            mHandler.removeCallbacks(mSeekRunnable);
            mHandler.postDelayed(mSeekRunnable, LOOP_DURATION);
        }
    }

}
