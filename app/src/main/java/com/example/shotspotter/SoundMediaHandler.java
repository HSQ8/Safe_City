package com.example.shotspotter;

import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;

/*
* adapted from https://github.com/halibobo/SoundMeter
 */

public class SoundMediaHandler {

    private MediaRecorder mMediaRecorder ;
    public boolean isRecording = false ;

    public float getMaxAmplitude() {
        if (mMediaRecorder != null) {
            try {
                return mMediaRecorder.getMaxAmplitude();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                return 0;
            }
        } else {
            return 5;
        }
    }

    /**
     * start record
     * @return whether we successfully started recording
     */
    public boolean startRecorder(String filename){


        try {
            mMediaRecorder = new MediaRecorder();

            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mMediaRecorder.setOutputFile(filename);

            mMediaRecorder.prepare();
            mMediaRecorder.start();
            isRecording = true;
        } catch(IOException exception) {
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
            isRecording = false ;
            exception.printStackTrace();
            Log.v(SoundData.APP_TAG, Log.getStackTraceString(exception));
        }catch(IllegalStateException e){
            stopRecording();
            e.printStackTrace();
            isRecording = false ;
            Log.v(SoundData.APP_TAG, Log.getStackTraceString(e));
        }
        return isRecording;

    }

    public void stopRecording() {
        if (mMediaRecorder != null){
            if(isRecording){
                try{
                    mMediaRecorder.stop();
                    mMediaRecorder.release();
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
            mMediaRecorder = null;
            isRecording = false ;
        }
    }

}
