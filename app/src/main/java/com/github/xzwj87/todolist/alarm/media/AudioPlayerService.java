package com.github.xzwj87.todolist.alarm.media;


import com.github.xzwj87.todolist.R;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by JasonWang on 2016/3/25.
 */

public class AudioPlayerService extends Service
        implements AudioManager.OnAudioFocusChangeListener{
    private static final String TAG = "AudioPlayerService";

    private AudioManager mAudioMgr;
    private MediaPlayer mPlayer;
    private Vibrator mVibrator;
    private int mRingerMode;
    private int mAlarmDuration;
    private float mVolume;

    private SharedPreferences mSharePref;
    private Resources mResource;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate(){
        Context context = getApplicationContext();
        mResource = context.getResources();
        mAudioMgr = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        mVibrator = (Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
        mSharePref = PreferenceManager.getDefaultSharedPreferences(context);

        Uri uri = Uri.parse(mSharePref.getString(mResource.getString(R.string.setting_alarm_ringtone_key),
                Settings.System.DEFAULT_RINGTONE_URI.toString()));

        mPlayer = MediaPlayer.create(context, uri);

        String volume = mSharePref.getString(mResource.getString
                (R.string.setting_alarm_volume_key), "1");
        mVolume = Integer.valueOf(volume);

        String alarmDuration = mSharePref.getString(mResource.getString
                (R.string.setting_alarm_duration_key), "90");
        mAlarmDuration = Integer.valueOf(alarmDuration)*1000;
    }

    @Override
    public int onStartCommand(Intent intent,int flags,int resId){
        mRingerMode = mAudioMgr.getRingerMode();
        if(mRingerMode == AudioManager.RINGER_MODE_NORMAL) {
            play();
        }else if(mVibrator.hasVibrator()){
            mVibrator.vibrate(mAlarmDuration);
        }

        return super.onStartCommand(intent,flags,resId);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(mRingerMode == AudioManager.RINGER_MODE_NORMAL) {
            stop();
            mAudioMgr.abandonAudioFocus(this);
        }else if(mVibrator.hasVibrator()){
            mVibrator.cancel();
        }
    }

    protected void play(){
        Log.d(TAG, "play()");

        int audioFocus = mAudioMgr.requestAudioFocus(this,AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
        if(audioFocus == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mPlayer.start();
            mPlayer.setLooping(true);
            mPlayer.setVolume(mVolume,mVolume);
        }
    }

    protected void stop(){
        Log.d(TAG,"stop()");

        if(mPlayer.isPlaying()){
            mPlayer.stop();
        }
        mAudioMgr.abandonAudioFocus(this);
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        Log.d(TAG, "onAudioFocusChange(): changedFocus = " + focusChange);

        if(focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT){
            mPlayer.pause();
        }else if(focusChange == AudioManager.AUDIOFOCUS_GAIN){
            mPlayer.start();
            mPlayer.setLooping(true);
        }else if(focusChange == AudioManager.AUDIOFOCUS_LOSS){
            stop();
        }
    }
}
