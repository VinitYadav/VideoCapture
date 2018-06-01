package com.videocapture;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    private SurfaceHolder surfaceHolder;
    private MediaRecorder mediaRecorder = new MediaRecorder();
    private Button mButton;
    private boolean isRecording;
    private Camera mCamera;
    private final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1001;
    private String[] ALL_PERMISSIONS = {Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Ge permission
        getPermission();
        // Set click listener on start/stop button
        onClickButton();
    }

    @Override
    protected void onDestroy() {
        releaseCamera();
        releaseMediaRecorder();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ID_MULTIPLE_PERMISSIONS:
                Map<String, Integer> perms = new HashMap<>();
                // Initialize the map with both permissions
                perms.put(Manifest.permission.RECORD_AUDIO, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.CAMERA, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                // Fill with actual results from user
                if (grantResults.length > 0) {
                    for (int i = 0; i < permissions.length; i++)
                        perms.put(permissions[i], grantResults[i]);
                    // Check for both permissions
                    if (perms.get(Manifest.permission.RECORD_AUDIO)
                            == PackageManager.PERMISSION_GRANTED
                            && perms.get(Manifest.permission.CAMERA)
                            == PackageManager.PERMISSION_GRANTED
                            && perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_GRANTED) {
                        init();
                    } else {
                        Toast.makeText(MainActivity.this,
                                "Go to settings and enable all permissions",
                                Toast.LENGTH_LONG).show();
                    }
                }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void init() {
        SurfaceView surfaceView = findViewById(R.id.surface_camera);

        // Initialize camera
        mCamera = Camera.open();

        // Set surface holder
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    /**
     * Click on start or stop button
     */
    private void onClickButton() {
        mButton = findViewById(R.id.button);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRecording) {
                    isRecording = false;
                    stopRecording();
                    buttonUI();
                } else {
                    try {
                        if (!hasPermissions(MainActivity.this, ALL_PERMISSIONS)) {
                            ActivityCompat.requestPermissions(MainActivity.this, ALL_PERMISSIONS,
                                    REQUEST_ID_MULTIPLE_PERMISSIONS);
                        } else {
                            isRecording = true;
                            startVideoRecording();
                            buttonUI();
                        }
                    } catch (Exception e) {
                        String message = e.getMessage();
                        Toast.makeText(getApplicationContext(),
                                "Problem in start video", Toast.LENGTH_LONG).show();
                        mediaRecorder.release();
                    }

                }
            }
        });
    }

    /**
     * Set start or stop button UI
     */
    private void buttonUI() {
        if (isRecording) {
            mButton.setText("Stop");
        } else {
            mButton.setText("Start");
        }
    }

    /**
     * Get permission
     */
    private void getPermission() {
        if (!hasPermissions(this, ALL_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, ALL_PERMISSIONS,
                    REQUEST_ID_MULTIPLE_PERMISSIONS);
        } else {
            init();
        }
    }

    /**
     * Check permission granted or not
     */
    private boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Start video recording
     */
    private void startVideoRecording() throws IOException {
        mediaRecorder = new MediaRecorder();
        mCamera.unlock();
        mediaRecorder.setCamera(mCamera);

        mediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
        mediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());

        // Save video in movie folder in internal storage
        String state = Environment.getExternalStorageState();
        String videoName = "/videoDemo" + System.currentTimeMillis() + ".mp4";
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            mediaRecorder.setOutputFile(Environment.getExternalStoragePublicDirectory(Environment
                    .DIRECTORY_MOVIES) + videoName);
        } else {
            mediaRecorder.setOutputFile(getFilesDir().getAbsolutePath() + videoName);
        }
        mediaRecorder.prepare();
        mediaRecorder.start();

        Toast.makeText(getApplicationContext(),
                "Video started", Toast.LENGTH_LONG).show();
    }


    /**
     * Stop recording
     */
    protected void stopRecording() {
        mediaRecorder.stop();
        mediaRecorder.release();
        mediaRecorder = null;
        Toast.makeText(getApplicationContext(),
                "Video saved", Toast.LENGTH_LONG).show();
    }

    /**
     * Release media recorder
     */
    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }

    /**
     * Release camera
     */
    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera.lock();
            mCamera = null;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (mCamera != null) {
            Camera.Parameters params = mCamera.getParameters();
            mCamera.setParameters(params);
        } else {
            Toast.makeText(getApplicationContext(),
                    "Camera not available!", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mCamera.stopPreview();
        mCamera.release();
    }
}