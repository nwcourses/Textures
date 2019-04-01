package com.chatchat.textures;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.view.View;

public class MainActivity extends Activity {

    OpenGLView view;
    CameraHandler cameraHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cameraHandler = new CameraHandler(this);
        cameraHandler.onCreate();
        view = new OpenGLView(this);
        view.setSurfaceTextureHandler(cameraHandler);
        setContentView(view);
    }

    public void onResume() {
        super.onResume();
        cameraHandler.onResume();
        view.onResume();

    }

    public void onPause() {
        super.onPause();
        view.onPause();
        cameraHandler.onPause();
    }
}
