package com.chatchat.textures;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.view.View;

public class MainActivity extends Activity {

    OpenGLView view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        view = new OpenGLView(this);
        setContentView(view);
    }

    public void onResume() {
        super.onResume();
        view.onResume();
    }

    public void onPause() {
        super.onPause();
        view.onPause();
    }
}
