// This is a working minimal camera2 app.

package com.chatchat.textures;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CameraHandler implements OpenGLView.SurfaceTextureHandler {


    CameraManager cameraManager;
    Size previewSize;
    CameraDevice.StateCallback stateCallback;
    String cameraId;
    Handler backgroundHandler = null;
    HandlerThread backgroundThread = null;
    CameraDevice device = null;
    CameraCaptureSession session = null;



    // Implementing a semaphore (from the Android sample) is necessary to prevent the camera going into an unstable state due to
    // the camera not closing properly on shutdown of this app. Semaphores basically lock code so only one thread can access it.
    // You acquire() a permit (code will block until acquired) and then you're done with it you release() it.
    // Note for example how we acquire the permit before we close the camera. This will block the thread which is closing
    // the camera until a permit is available. Idea is to prevent the app finishing before the camera has been closed.
    Semaphore cameraOpenCloseLock = new Semaphore(1);
    Context ctx;

    SurfaceTexture surfaceTexture = null;

    public CameraHandler(Context ctx) {
        this.ctx = ctx;
    }
    // onCreate() simply sets up the state callback and surface texture listener - nothing else.
    public void onCreate() {




        // create a state callback. This handles different events in the camera's lifecycle; we can for example
        // handle the camera being opened so we can start up a preview when we know the camera is opened.
        stateCallback = new CameraDevice.StateCallback() {


            // runs when the camera is opened. Store it as an attribute of the class and start up
            // our preview session - the idea is we only start up a preview session once the camera has been opened.
            public void onOpened(CameraDevice camera) {
                Log.e("camera", "the device has been opened");
                cameraOpenCloseLock.release();
                device = camera;
                createPreviewSession();
            }

            // runs when the camera is disconnected. Close the camera and set the 'device' attribute to null
            public void onDisconnected(CameraDevice camera) {
                cameraOpenCloseLock.release();
                if (camera != null) {
                    camera.close();
                    device = null;
                }
            }

            // runs when there's an error. Just close camera and set device to null again, for now
            public void onError(CameraDevice camera, int error) {
                Log.e("camera", "error " + error);
                if (camera != null) {
                    camera.close();
                    device = null;
                }
            }
        };

        //texview = findViewById(R.id.texview);

        // request the camera permissions
        // requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
    }


    // Set the camera up when we resume the app
    public void onResume() {



        // only setup the camera if the texture view is available...
        // (if it's not available, this will be done in onSurfaceTextureAvailable() above
        if (surfaceTexture != null) {
            openBackgroundThread();
            setupAndOpenCamera();
        } else {
            // do nothing, for now...

        }
    }

    // set up and open the camera. setupCamera() returns the ID of the back facing camera, then we open it
    // (as long as we can find a back facing camera; if not setupCamera() returns null)
    private void setupAndOpenCamera() {
        String id = setupCamera();
        if (id != null) {
            cameraId = id;
            openCamera();
        }
    }


    // set up the camera. This involves searching the list of available cameras for a back-facing camera and
    // also finding the preview size supported by this camera. The ID of the found camera is returned.
    String setupCamera() {
        try {
            // Obtain a CameraManager. Similar in concept to LocationManager, AlarmManager etc - manages the camera
            cameraManager = (CameraManager) ctx.getSystemService(Context.CAMERA_SERVICE);

            // Loop through the camera ID list of the camera manager. Each device will have at least one camera but
            // maybe more - e.g. a back facing camera (environment) or front facing camera (e.g. for selfies)
            for (String id : cameraManager.getCameraIdList()) {

                // Get the characteristics of this camera
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);

                // If it's a back facing camera, select it
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {

                    // Obtain the correct preview size (width, height) for this camera
                    StreamConfigurationMap streamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    previewSize = streamConfigurationMap.getOutputSizes(SurfaceTexture.class)[0];

                    // and return the camera ID.
                    return id;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        // return null if no back facing camera found
        return null;
    }

    // Open a given camera specified by ID (the camera ID was returned from setupCamera())
    void openCamera() {
        // Check the permission has been granted
        if (ctx.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            try {
                if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                    Toast.makeText(ctx, "can't get a lock on the camera", Toast.LENGTH_SHORT).show();
                } else {

                    // Open the camera with that ID. We have to pass in the state callback object (see above) which will handle
                    // different events in the camera's state (e.g. opened, disconnected etc - see above)
                    // We only want to show a preview when the camera is opened, and this will happen once the onOpened()
                    // callback has been called.
                    cameraManager.openCamera(cameraId, stateCallback, backgroundHandler);

                }
            } catch (Exception e) {
                Log.d("camera", e.toString());
            }
        } else { // if not request the needed permissions (camera, and storage for saving picture)
            ((MainActivity)ctx).requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
    }



    public void receiveSurfaceTexture(SurfaceTexture st) {
        surfaceTexture = st;
        onResume();
    }

    // create a preview session...
    void createPreviewSession() {

        // get the surface texture associated with the texture view

        // set its size to the supported preview size of this camera (obtained in setupCamera(), above)
        surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());

        // Create a Surface to draw on from the surface texture
        final Surface surface = new Surface(surfaceTexture);


        if (device != null) {
            // create a capture session for this device
            ArrayList<Surface> surfaceList = new ArrayList();
            surfaceList.add(surface);
            try {
                device.createCaptureSession(surfaceList, new CameraCaptureSession.StateCallback() {

                    // callback which runs when the camera device has been configured, i.e. ready to go
                    public void onConfigured(CameraCaptureSession session) {
                        if (device != null) {


                            try {
                                // build a request and set it to be a repeating request i.e. each time there's a new frame,
                                // show it on the surface
                                // create a capture request builder and associate it with the surface
                                CaptureRequest.Builder builder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                                builder.addTarget(surface);
                                CaptureRequest captureRequest = builder.build();
                                if (captureRequest != null) {
                                    session.setRepeatingRequest(captureRequest, null, backgroundHandler);
                                }
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    public void onConfigureFailed(CameraCaptureSession session) {
                        Log.e("camera", "onConfigureFailed: something went wrong...");
                    }
                }, backgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    void openBackgroundThread() {
        backgroundThread = new HandlerThread("camera_background_Thread");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    void closeBackgroundThread() {
        backgroundThread.quitSafely();
        backgroundThread = null;
        backgroundHandler = null;
    }

    // close the camera when we pause the app
    public void onPause() {

        closeCamera();
        closeBackgroundThread();
    }

    // clean up the camera capture session and the device
    private void closeCamera() {
        try {
            cameraOpenCloseLock.acquire();
            if (session != null) {
                session.close();
                session = null;
            }
            if (device != null) {
                device.close();
                device = null;
            }
        } catch (InterruptedException e) {
        } finally {
            cameraOpenCloseLock.release();
        }
    }
}
