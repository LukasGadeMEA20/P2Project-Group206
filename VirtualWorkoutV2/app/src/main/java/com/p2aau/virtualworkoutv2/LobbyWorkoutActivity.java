package com.p2aau.virtualworkoutv2;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewParent;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import com.p2aau.virtualworkoutv2.classes.ExerciseConstant;
import com.p2aau.virtualworkoutv2.openvcall.model.AGEventHandler;
import com.p2aau.virtualworkoutv2.openvcall.model.ConstantApp;
import com.p2aau.virtualworkoutv2.openvcall.model.DuringCallEventHandler;
import com.p2aau.virtualworkoutv2.openvcall.ui.BaseActivity;
import com.p2aau.virtualworkoutv2.openvcall.ui.layout.GridVideoViewContainer;
import com.p2aau.virtualworkoutv2.openvcall.ui.layout.SmallVideoViewAdapter;
import com.p2aau.virtualworkoutv2.openvcall.ui.layout.SmallVideoViewDecoration;
import com.p2aau.virtualworkoutv2.propeller.UserStatusData;
import com.p2aau.virtualworkoutv2.propeller.ui.RecyclerItemClickListener;
import com.p2aau.virtualworkoutv2.propeller.ui.RtlLinearLayoutManager;

import org.w3c.dom.Text;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;

public class LobbyWorkoutActivity extends BaseActivity implements DuringCallEventHandler {

    // -- Attributes -- //
    // - Attributes for timer - //
    private TextView mTextViewCountDown;
    private CountDownTimer mCountDownTimer;
    private MediaPlayer alarmPlayer;

    private boolean mTimerRunning;
    private long mTimeLeftInMillis;

    // - Attributes for exercise - //
    private TextView exerciseName;

    // - Attributes for webcam - //
    public static final int LAYOUT_TYPE_DEFAULT = 0;
    public static final int LAYOUT_TYPE_SMALL = 1;

    // should only be modified under UI thread
    private final HashMap<Integer, SurfaceView> mUidsList = new HashMap<>(); // uid = 0 || uid == EngineConfig.mUid
    public int mLayoutType = LAYOUT_TYPE_DEFAULT;
    private GridVideoViewContainer mGridVideoViewContainer;
    private RelativeLayout mSmallVideoViewDock;

    private volatile boolean mVideoMuted = false;
    private volatile boolean mAudioMuted = false;

    private volatile int mAudioRouting = Constants.AUDIO_ROUTE_DEFAULT;

    private boolean mIsLandscape = false;

    private SmallVideoViewAdapter mSmallVideoViewAdapter;

    private double height = 0.25;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby_workout);

        getSupportActionBar().hide(); // Hides the top action bar, as it serves no purpose other than being ugly

        // Finds the view for where the name of the exercise will be, and sets it
        exerciseName = (TextView) findViewById(R.id.text_view_exercise_description);
        exerciseName.setText(ExerciseConstant.EXERCISE.getExerciseName());

        // Finds the view and gets the time to complete
        VideoView videoView = findViewById(R.id.exercise_video);
        mTimeLeftInMillis = ExerciseConstant.EXERCISE.getTimeToComplete();

        // Finds the video in the resources based on what video the exercise got given
        String videoPath = "android.resource://" + getPackageName() + "/" + ExerciseConstant.EXERCISE.getVideo();
        Uri uri = Uri.parse(videoPath); // Parses the path to a variable the video class wants
        videoView.setVideoURI(uri); // Sets the video viewers video to the one specified
        videoView.start(); // Starts the video
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setLooping(true);
            }
        }); // Once the video is ready, we tell it that it should loop

        // Timer
        mTextViewCountDown = findViewById(R.id.text_view_countdown);
        updateCountDownText();
        startTimer();
    }

    // Gets run before the onCreate above, as it comes from the super class "BaseActivity".
    // This is from the Agora code example for adding webcam, with slight customization.
    @Override
    protected void initUIandEvent() {
        addEventHandler(this); // Tells the program it is this activity that gets worked on.
        String channelName = ConstantApp.ACTION_KEY_CHANNEL_NAME; // The channel name which the user joins for webcam

        // Finds the view in which the program has to confine in
        mGridVideoViewContainer = (GridVideoViewContainer) findViewById(R.id.grid_video_view_container_own);

        // Generates the local video and the view at which the webcam will place
        SurfaceView surfaceV = RtcEngine.CreateRendererView(getApplicationContext());
        preview(true, surfaceV, 0);
        surfaceV.setZOrderOnTop(false);
        surfaceV.setZOrderMediaOverlay(false);

        mUidsList.put(0, surfaceV); // get first surface view

        // Initializes the container with all the views
        mGridVideoViewContainer.initViewContainer(this, 0, mUidsList, mIsLandscape, false, height); // first is now full view

        // Connects to the server and joins the channel
        joinChannel(channelName, config().mUid);

        // Runs anything optional that is missing.
        optional();
    }

    // - Destroys the UI and the event to leave the channel, to remove the video call from the screen - //
    @Override
    protected void deInitUIandEvent() {
        optionalDestroy();
        doLeaveChannel();
        removeEventHandler(this);
        mUidsList.clear();
    }

    // - Leaves the channel - //
    private void doLeaveChannel() {
        leaveChannel(config().mChannel);
        preview(false, null, 0);
    }

    // - Timer - //
    private void startTimer() {
        mCountDownTimer = new CountDownTimer(mTimeLeftInMillis, 1000) {
            @Override // each tick
            public void onTick(long millisUntilFinished) {
                mTimeLeftInMillis = millisUntilFinished;
                updateCountDownText();

                // Once the timer gets below six seconds it starts playing the alarm
                if(millisUntilFinished<=6000) {
                    playExerciseTimerAlarm();
                }
            }

            @Override // on finish
            public void onFinish() {
                mTimerRunning = false;
                updatePage();
            }

        }.start();
    }

    // - Updates text - //
    private void updateCountDownText() {
        int minutes = (int) (mTimeLeftInMillis / 1000) / 60;
        int seconds = (int) (mTimeLeftInMillis / 1000) % 60;
        String timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        mTextViewCountDown.setText(timeLeftFormatted);

    }

    // - Players alarm sound - //
    public void playExerciseTimerAlarm() {
        if(alarmPlayer==null) {
            alarmPlayer = MediaPlayer.create(this, R.raw.tickdown);
            alarmPlayer.start();
            alarmPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    alarmPlayer.release();
                }
            });
        }

    }

    // - Next page method - //
    public void updatePage() {

        deInitUIandEvent(); // turns off webcam and leaves lobby
        if (checkWorkoutStatus()) { // Checks how long the user is in exercise program, and if they are finished or not
            Intent intent = new Intent(LobbyWorkoutActivity.this, InbetweenWorkoutsActivity.class);
            ExerciseConstant.CURRENT_EXERCISE++; // counts up
            ExerciseConstant.EXERCISE = ExerciseConstant.EXERCISE_PROGRAM.getListOfExercises().get(ExerciseConstant.CURRENT_EXERCISE-1); // gets the next exercise in the list
            startActivity(intent);
        } else { // else if it is finished, it will go to the end screen
            Intent intent = new Intent(LobbyWorkoutActivity.this, EndScreenActivity.class);
            startActivity(intent);
        }
    }

    // - Method for checking if it is finished with the workout - //
    public boolean checkWorkoutStatus(){
        boolean bool;
        if (ExerciseConstant.CURRENT_EXERCISE < ExerciseConstant.MAX_EXERCISE){ // checks if it still below the max amount of exercises
            bool = true;
        } else { // else it will return false, meaning it is finished
            bool = false;
        }
        return bool;
    }
    // - Code from Agora - //
    private void optional() {
        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
    }

    // - Code from Agora - //
    private void optionalDestroy() {
    }

    // - Code from Agora - //
    @Override
    public void onUserJoined(int uid) {

    }

    // - Code from Agora - //
    @Override
    public void onFirstRemoteVideoDecoded(int uid, int width, int height, int elapsed) {
        doRenderRemoteUi(uid);
    }

    // - Code from Agora - //
    private void doRenderRemoteUi(final int uid) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isFinishing()) {
                    return;
                }

                if (mUidsList.containsKey(uid)) {
                    return;
                }

                /*
                  Creates the video renderer view.
                  CreateRendererView returns the SurfaceView type. The operation and layout of the
                  view are managed by the app, and the Agora SDK renders the view provided by the
                  app. The video display view must be created using this method instead of
                  directly calling SurfaceView.
                 */
                SurfaceView surfaceV = RtcEngine.CreateRendererView(getApplicationContext());
                mUidsList.put(uid, surfaceV);

                boolean useDefaultLayout = mLayoutType == LAYOUT_TYPE_DEFAULT;

                surfaceV.setZOrderOnTop(true);
                surfaceV.setZOrderMediaOverlay(true);

                /*
                  Initializes the video view of a remote user.
                  This method initializes the video view of a remote stream on the local device. It affects only the video view that the local user sees.
                  Call this method to bind the remote video stream to a video view and to set the rendering and mirror modes of the video view.
                 */
                rtcEngine().setupRemoteVideo(new VideoCanvas(surfaceV, VideoCanvas.RENDER_MODE_HIDDEN, uid));

                if (useDefaultLayout) {
                    switchToDefaultVideoView();
                } else {
                    int bigBgUid = mSmallVideoViewAdapter == null ? uid : mSmallVideoViewAdapter.getExceptedUid();
                    switchToSmallVideoView(bigBgUid);
                }
            }
        });
    }

    // - Code from Agora - //
    @Override
    public void onJoinChannelSuccess(String channel, int uid, int elapsed) {

    }

    // - Code from Agora - //
    @Override
    public void onUserOffline(int uid, int reason) {
        doRemoveRemoteUi(uid);
    }

    // - Code from Agora - //
    private void doRemoveRemoteUi(final int uid) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isFinishing()) {
                    return;
                }

                Object target = mUidsList.remove(uid);
                if (target == null) {
                    return;
                }

                int bigBgUid = -1;
                if (mSmallVideoViewAdapter != null) {
                    bigBgUid = mSmallVideoViewAdapter.getExceptedUid();
                }

                if (mLayoutType == LAYOUT_TYPE_DEFAULT || uid == bigBgUid) {
                    switchToDefaultVideoView();
                } else {
                    switchToSmallVideoView(bigBgUid);
                }
            }
        });
    }

    // - Code from Agora - //
    @Override
    public void onExtraCallback(int type, Object... data) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isFinishing()) {
                    return;
                }

                doHandleExtraCallback(type, data);
            }
        });
    }

    // - Code from Agora - //
    private void doHandleExtraCallback(int type, Object... data) {
        int peerUid;
        boolean muted;

        switch (type) {
            case AGEventHandler.EVENT_TYPE_ON_USER_AUDIO_MUTED:
                peerUid = (Integer) data[0];
                muted = (boolean) data[1];

                if (mLayoutType == LAYOUT_TYPE_DEFAULT) {
                    HashMap<Integer, Integer> status = new HashMap<>();
                    status.put(peerUid, muted ? UserStatusData.AUDIO_MUTED : UserStatusData.DEFAULT_STATUS);
                    mGridVideoViewContainer.notifyUiChanged(mUidsList, config().mUid, status, null);
                }

                break;

            case AGEventHandler.EVENT_TYPE_ON_USER_VIDEO_MUTED:
                peerUid = (Integer) data[0];
                muted = (boolean) data[1];

                //doHideTargetView(peerUid, muted);

                break;

            case AGEventHandler.EVENT_TYPE_ON_USER_VIDEO_STATS:
                IRtcEngineEventHandler.RemoteVideoStats stats = (IRtcEngineEventHandler.RemoteVideoStats) data[0];

                mGridVideoViewContainer.cleanVideoInfo();

                break;

            case AGEventHandler.EVENT_TYPE_ON_SPEAKER_STATS:
                IRtcEngineEventHandler.AudioVolumeInfo[] infos = (IRtcEngineEventHandler.AudioVolumeInfo[]) data[0];

                if (infos.length == 1 && infos[0].uid == 0) { // local guy, ignore it
                    break;
                }

                if (mLayoutType == LAYOUT_TYPE_DEFAULT) {
                    HashMap<Integer, Integer> volume = new HashMap<>();

                    for (IRtcEngineEventHandler.AudioVolumeInfo each : infos) {
                        peerUid = each.uid;
                        int peerVolume = each.volume;

                        if (peerUid == 0) {
                            continue;
                        }
                        volume.put(peerUid, peerVolume);
                    }
                    mGridVideoViewContainer.notifyUiChanged(mUidsList, config().mUid, null, volume);
                }

                break;

            case AGEventHandler.EVENT_TYPE_ON_APP_ERROR:
                int subType = (int) data[0];

                if (subType == ConstantApp.AppError.NO_CONNECTION_ERROR) {
                    String msg = getString(R.string.msg_connection_error);
                    showLongToast(msg);
                }

                break;

            case AGEventHandler.EVENT_TYPE_ON_DATA_CHANNEL_MSG:

                peerUid = (Integer) data[0];
                final byte[] content = (byte[]) data[1];

                break;

            case AGEventHandler.EVENT_TYPE_ON_AGORA_MEDIA_ERROR: {
                int error = (int) data[0];
                String description = (String) data[1];


                break;
            }

            case AGEventHandler.EVENT_TYPE_ON_AUDIO_ROUTE_CHANGED:
                notifyHeadsetPlugged((int) data[0]);

                break;

        }
    }

    // - Code from Agora - //
    private void switchToDefaultVideoView() {
        if (mSmallVideoViewDock != null) {
            mSmallVideoViewDock.setVisibility(View.GONE);
        }
        mGridVideoViewContainer.initViewContainer(this, config().mUid, mUidsList, mIsLandscape, false, height);

        mLayoutType = LAYOUT_TYPE_DEFAULT;
        boolean setRemoteUserPriorityFlag = false;
        int sizeLimit = mUidsList.size();
        if (sizeLimit > ConstantApp.MAX_PEER_COUNT + 1) {
            sizeLimit = ConstantApp.MAX_PEER_COUNT + 1;
        }
        for (int i = 0; i < sizeLimit; i++) {
            int uid = mGridVideoViewContainer.getItem(i).mUid;
            if (config().mUid != uid) {
                if (!setRemoteUserPriorityFlag) {
                    setRemoteUserPriorityFlag = true;
                    rtcEngine().setRemoteUserPriority(uid, Constants.USER_PRIORITY_HIGH);
                } else {
                    rtcEngine().setRemoteUserPriority(uid, Constants.USER_PRIORITY_NORMAL);
                }
            }
        }
    }

    // - Code from Agora - //
    private void switchToSmallVideoView(int bigBgUid) {
        HashMap<Integer, SurfaceView> slice = new HashMap<>(1);
        slice.put(bigBgUid, mUidsList.get(bigBgUid));
        Iterator<SurfaceView> iterator = mUidsList.values().iterator();
        while (iterator.hasNext()) {
            SurfaceView s = iterator.next();
            s.setZOrderOnTop(true);
            s.setZOrderMediaOverlay(true);
        }

        mUidsList.get(bigBgUid).setZOrderOnTop(false);
        mUidsList.get(bigBgUid).setZOrderMediaOverlay(false);

        mGridVideoViewContainer.initViewContainer(this, bigBgUid, slice, mIsLandscape, false, height);

        bindToSmallVideoView(bigBgUid);

        mLayoutType = LAYOUT_TYPE_SMALL;

        //requestRemoteStreamType(mUidsList.size());
    }

    // - Code from Agora - //
    private void bindToSmallVideoView(int exceptUid) {
        if (mSmallVideoViewDock == null) {
            ViewStub stub = (ViewStub) findViewById(R.id.small_video_view_dock);
            mSmallVideoViewDock = (RelativeLayout) stub.inflate();
        }

        boolean twoWayVideoCall = mUidsList.size() == 2;

        RecyclerView recycler = (RecyclerView) findViewById(R.id.small_video_view_container);

        boolean create = false;

        if (mSmallVideoViewAdapter == null) {
            create = true;
            mSmallVideoViewAdapter = new SmallVideoViewAdapter(this, config().mUid, exceptUid, mUidsList, height);
            mSmallVideoViewAdapter.setHasStableIds(true);
        }
        recycler.setHasFixedSize(true);

        if (twoWayVideoCall) {
            recycler.setLayoutManager(new RtlLinearLayoutManager(getApplicationContext(), RtlLinearLayoutManager.HORIZONTAL, false));
        } else {
            recycler.setLayoutManager(new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.HORIZONTAL, false));
        }
        recycler.addItemDecoration(new SmallVideoViewDecoration());
        recycler.setAdapter(mSmallVideoViewAdapter);
        recycler.addOnItemTouchListener((RecyclerView.OnItemTouchListener) new RecyclerItemClickListener(getBaseContext(), new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {

            }

            @Override
            public void onItemLongClick(View view, int position) {

            }

            @Override
            public void onItemDoubleClick(View view, int position) {
                onSmallVideoViewDoubleClicked(view, position);
            }
        }));

        recycler.setDrawingCacheEnabled(true);
        recycler.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_AUTO);

        if (!create) {
            mSmallVideoViewAdapter.setLocalUid(config().mUid);
            mSmallVideoViewAdapter.notifyUiChanged(mUidsList, exceptUid, null, null);
        }
        for (Integer tempUid : mUidsList.keySet()) {
            if (config().mUid != tempUid) {
                if (tempUid == exceptUid) {
                    rtcEngine().setRemoteUserPriority(tempUid, Constants.USER_PRIORITY_HIGH);
                } else {
                    rtcEngine().setRemoteUserPriority(tempUid, Constants.USER_PRIORITY_NORMAL);
                }
            }
        }
        recycler.setVisibility(View.VISIBLE);
        mSmallVideoViewDock.setVisibility(View.VISIBLE);
    }

    // - Code from Agora - //
    private void onSmallVideoViewDoubleClicked(View view, int position) {
        switchToDefaultVideoView();
    }

    // - Code from Agora - //
    public void notifyHeadsetPlugged(final int routing) {
        mAudioRouting = routing;
    }
}

