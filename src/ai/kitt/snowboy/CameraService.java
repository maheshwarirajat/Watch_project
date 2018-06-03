package ai.kitt.snowboy;

import android.annotation.TargetApi;
import android.app.Service;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;

import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import static android.os.Environment.getExternalStorageDirectory;
import static android.view.SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS;


/**
 * Created by shekhar on 03/06/18.
 */

public class CameraService extends Service {
    private boolean safeToTakePicture = false;
    static int x=0;
    //Camera variables
    //a surface holder
    private SurfaceHolder sHolder;
    //a variable to control the camera
    private Camera mCamera;
    //the camera parameters
    private Parameters parameters;
    /** Called when the activity is first created. */
    @Override
    public void onCreate()
    {
        super.onCreate();

    }
    @Override
    public void onStart(Intent intent, int startId) {
        // TODO Auto-generated method stub
        super.onStart(intent, startId);

        mCamera = Camera.open();
        SurfaceView sv = new SurfaceView(getApplicationContext());


        try {
            mCamera.setPreviewDisplay(sv.getHolder());
            parameters = mCamera.getParameters();

            //set camera parameters
            mCamera.setParameters(parameters);
            mCamera.startPreview();
            safeToTakePicture = true;
            mCamera.takePicture(null,null,mCall);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


        //Get a surface
        sHolder = sv.getHolder();

        //tells Android that this surface will have its data constantly replaced
        sHolder.setType(SURFACE_TYPE_PUSH_BUFFERS);
    }



    Camera.PictureCallback mCall = new Camera.PictureCallback()
    {

        public void onPictureTaken(byte[] data, Camera camera)
        {
            //decode the data obtained by the camera into a Bitmap

            FileOutputStream outStream = null;
            try{
                x++;
                outStream = new FileOutputStream(String.format("/sdcard/%d.jpg",System.currentTimeMillis()));
                outStream.write(data);
                outStream.close();
                mCamera.release();
                Toast.makeText(getApplicationContext(), "picture clicked", Toast.LENGTH_LONG).show();
                refreshCamera();
            } catch (FileNotFoundException e){
                Log.d("CAMERA", e.getMessage());
            } catch (IOException e){
                Log.d("CAMERA", e.getMessage());
            }

        }
    };


    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

    public void refreshCamera() {
        if (sHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }
        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here
        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(sHolder);
            mCamera.startPreview();
        } catch (Exception e) {

        }
    }
}
