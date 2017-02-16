package com.chatchat.textures;

/**
 * Created by whitelegg_n on 09/03/2016.
 */
import android.hardware.Camera;
import android.graphics.SurfaceTexture;
import java.io.IOException;

public class CameraCapturer
 {

    Camera camera;


    public void openCamera() throws Exception
    {

        camera = Camera.open();

    }

    public void startPreview(SurfaceTexture surfaceTexture) throws IOException
    {
        camera.setPreviewTexture(surfaceTexture);
   //     surfaceTexture.setOnFrameAvailableListener(this);
        camera.startPreview();
    }

    public void stopPreview()
    {
        camera.stopPreview();
        releaseCamera();
    }

    public void releaseCamera()
    {
        if(camera!=null)
        {
            camera.release();
            camera=null;
        }
    }



    public boolean isActive()
    {
        return camera!=null;
    }
}