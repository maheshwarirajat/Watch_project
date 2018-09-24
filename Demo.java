package ai.kitt.snowboy;

import ai.kitt.snowboy.audio.RecordingThread;
import ai.kitt.snowboy.audio.PlaybackThread;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.SmsManager;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;


import ai.kitt.snowboy.audio.AudioDataSaver;
import ai.kitt.snowboy.demo.R;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
//import com.google.android.gms.vision.CameraSource;

import static android.content.ContentValues.TAG;
//import static com.google.android.gms.internal.zzagz.runOnUiThread;


import android.os.AsyncTask;


public class Demo extends Activity {

    private Button record_button;
    private Button play_button;
    private TextView log;
    private ScrollView logView;
    private Button location_button;
    private TextView location;
    static String strLog = null;

    private int preVolume = -1;
    private static long activeTimes = 0;

    private RecordingThread recordingThread;
    private PlaybackThread playbackThread;

    //////////////////////////////////////////// added
    private int CAMERA_ACCESS_CODE = 1234;
    //private File imagefile;
    private File default_location = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
    private Camera camera;
    private int cameraId = 0;
    private LocationManager locationManager;
    private LocationListener locationlistener;
    private String sms_text = "HELP ME";
    private FusedLocationProviderClient mFusedLocationClient;
    protected Location mLastLocation;
    private boolean golablflag = false;
    ProgressDialog dialog;
    private String SERVER_URL = "http://ajaneya.ddnsfree.com/UploadToServer.php?";
    private int imageCount = 0;
    private int MY_PERMISSIONS_REQUEST_LOCATION = 200;
    private Location RealLocation = null;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    private LocationCallback mLocationCallback;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        setUI();
        requestPermissionLocation();
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);// Make to run your application only in portrait mode
        setProperVolume();
        AppResCopy.copyResFromAssetsToSD(this);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        ////////check_permisssions();

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            check_permisssions();
//
//        } else {
//            // write your logic here
//        }
        ////////////


        activeTimes = 0;
        recordingThread = new RecordingThread(handle, new AudioDataSaver());
        playbackThread = new PlaybackThread();
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    // Update UI with location data
                    // ...
                    DatabaseReference myRef = database.getReference("message");

                    myRef.setValue(location);
                }
            }

            ;
        };
    }

    @Override
    protected void onResume() {
        super.onResume();

            startLocationUpdates();

    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mFusedLocationClient.requestLocationUpdates(null,
                mLocationCallback,
                null /* Looper */);
    }

    android.hardware.Camera.PictureCallback mPicture = new android.hardware.Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            File pictureFileDir = default_location;

            if (!pictureFileDir.exists() && !pictureFileDir.mkdirs()) {
                showToast("Can't create directory to save image.");
                return;

            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyymmddhhmmss");
            String date = dateFormat.format(new Date());
            String photoFile = "Incident_" + date + ".jpg";

            String filename = pictureFileDir.getPath() + File.separator + photoFile;

            File pictureFile = new File(filename);

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
                showToast("New Image saved:" + photoFile);
                updateLog("image saved" + filename, "red");
                global.Location = filename;

                Log.v(TAG, "gloabl" + filename);
                //uploadFile(global.Location);
                //Intent intent= new Intent(Demo.this,uploadActivity.class);

                //startActivity(intent);

            } catch (Exception error) {
                showToast("Image could not be saved." + error.getMessage());
            }

        }
    };

//    android.hardware.Camera.PictureCallback jpegcallback = new android.hardware.Camera.PictureCallback() {
//        @Override
//        public  void onPictureTaken(byte data[], Camera camera){
//            uploadFile(global.Location);
//        }
//
//    };


    public Handler handle = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            MsgEnum message = MsgEnum.getMsgEnum(msg.what);
            switch (message) {
                case MSG_ACTIVE:
                    activeTimes++;
                    global.Location = "";
                    updateLog(" ----> Detected " + activeTimes + " times", "green");
                    // Toast.makeText(Demo.this, "Active "+activeTimes, Toast.LENGTH_SHORT).show();
                    showToast("Active " + activeTimes);
                    //sms_text="HELP ME LALAN KUMAR" + '\n' + "My location is";

                    ////// CAMERA CODE

                    // do we have a camera?
//                    if (!getPackageManager()
//                            .hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
//                        showToast("No camera on this device");
//
//                    } else {
//                        cameraId = findFrontFacingCamera();
//                        if (cameraId < 0) {
//                            showToast("No front facing camera found.");
//
//                        } else {
//                            camera = Camera.open(cameraId);
//                        }
//                    }

                    try {
                        releaseCameraAndPreview();
                        camera = Camera.open(0);
                    } catch (Exception e) {
                        Log.e(getString(R.string.app_name), "failed to open Camera");
                        e.printStackTrace();
                    }


                    ///// GPS LOCATION
                    updateLog("before getlocation");
                    getLocation();
                    updateLog("after getlocation");
                    //RetrieveLocation();
                    ////Firebase Code



                    ////Firebase Code

                    ////// SMS CODE

                    String phoneNo = "9582816345";


                    PackageManager pm = getApplicationContext().getPackageManager();

                    if (!pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) &&
                            !pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_CDMA)) {
                        showToast("Sorry, your device probably can't send SMS...");
                    }

                    try {
                        SmsManager smsManager = SmsManager.getDefault();
                        //smsManager.sendTextMessage(phoneNo, null, sms_text, null, null);
                        updateLog("after message", "red");
                        showToast("SMS Sent!");
                    } catch (Exception e) {
                        showToast("SMS faild, please try again later!");
                        e.printStackTrace();
                    }


//                    camera.startPreview();
//                    //camera.takePicture(null, null, new photohandler(getApplicationContext(), default_location));
//                    camera.takePicture(null, null, mPicture);
//                    updateLog("after camera", "red");
//
//                    MyAsyncTask myAsyncTasks = new MyAsyncTask();
//                    myAsyncTasks.execute();
//                    updateLog("after async task", "red");


                    break;
                case MSG_INFO:
                    updateLog(" ----> " + message);
                    break;
                case MSG_VAD_SPEECH:
                    updateLog(" ----> normal voice", "blue");
                    break;
                case MSG_VAD_NOSPEECH:
                    updateLog(" ----> no speech", "blue");
                    break;
                case MSG_ERROR:
                    updateLog(" ----> " + msg.toString(), "red");
                    break;
                default:
                    super.handleMessage(msg);
                    break;


            }


        }
    };

    private void readStream(InputStream in) {

    }


    private void releaseCameraAndPreview() {
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }

    public class MyAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Void doInBackground(Void... params) {
            long time1 = System.currentTimeMillis();
            long time = 0;
            while (time < 2000) {
                long time2 = System.currentTimeMillis();
                time = time2 - time1;
            }


            updateLog(Long.toString(time)
                    , "blue");
            updateLog("location is " + global.Location, "red");
            uploadFile(global.Location);
            updateLog("after upload file", "red");
            return null;

        }

        @Override
        protected void onPostExecute(Void v) {
            if (imageCount < 5) {
                camera.startPreview();
                //camera.takePicture(null, null, new photohandler(getApplicationContext(), default_location));
                camera.takePicture(null, null, mPicture);
                imageCount++;
                updateLog("after camera" + imageCount, "red");

                MyAsyncTask myAsyncTasks = new MyAsyncTask();
                myAsyncTasks.execute();
                updateLog("after async task", "red");
            } else {
                Trigger trigger = new Trigger();
                trigger.execute();
            }


        }

    }

    public class Trigger extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                URL url = new URL("http://ajaneya.ddnsfree.com/python.php?");
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                readStream(in);

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    //android upload file to server
    public void uploadFile(String selectedFilePath) {


        int serverResponseCode = 0;
        HttpURLConnection connection;
        DataOutputStream dataOutputStream;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";


        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;
        File selectedFile = new File(selectedFilePath);


        String[] parts = selectedFilePath.split("/");
        final String fileName = parts[parts.length - 1];

        if (!selectedFile.isFile()) {
            //dialog.dismiss();
            //Log.v(TAG,"gloabal"+selectedFilePath);
            //showToast(selectedFilePath);
            //showToast("file issue");
            updateLog("file not available" + selectedFilePath, "red");
            return;
        } else {
            try {

                //Log.v(TAG, "im in try");
                updateLog("before file input stream", "red");


                FileInputStream fileInputStream = new FileInputStream(selectedFile);

                URL url = new URL(SERVER_URL);
                updateLog("before connection", "red");
                connection = (HttpURLConnection) url.openConnection();
                updateLog("after conection", "red");
                connection.setDoInput(true);//Allow Inputs
                connection.setDoOutput(true);//Allow Outputs
                connection.setUseCaches(false);//Don't use a cached Copy
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.setRequestProperty("ENCTYPE", "multipart/form-data");
                connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                connection.setRequestProperty("uploaded_file", selectedFilePath);
                //showToast(selectedFilePath);
                //creating new dataoutputstream
                updateLog("im before path", "red");

                dataOutputStream = new DataOutputStream(connection.getOutputStream());
                updateLog("im after path", "red");
                //writing bytes to data outputstream
                dataOutputStream.writeBytes(twoHyphens + boundary + lineEnd);

                dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""
                        + selectedFilePath + "\"" + lineEnd);

                dataOutputStream.writeBytes(lineEnd);


                //returns no. of bytes present in fileInputStream
                bytesAvailable = fileInputStream.available();
                //selecting the buffer size as minimum of available bytes or 1 MB
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                //setting the buffer as byte array of size of bufferSize
                buffer = new byte[bufferSize];

                //reads bytes from FileInputStream(from 0th index of buffer to buffersize)
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                Log.v(TAG, "before bytesread");


                //loop repeats till bytesRead = -1, i.e., no bytes are left to read
                while (bytesRead > 0) {
                    //write the bytes read from inputstream
                    dataOutputStream.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }
                updateLog("after bytesread", "red");

                dataOutputStream.writeBytes(lineEnd);
                dataOutputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                serverResponseCode = connection.getResponseCode();
                String serverResponseMessage = connection.getResponseMessage();

                Log.v(TAG, "Server Response is: " + serverResponseMessage + ": " + serverResponseCode);
                updateLog("Server Response is: " + serverResponseMessage + ": " + serverResponseCode, "red");

                //response code of 200 indicates the server status OK
                if (serverResponseCode == 200) {
                    Log.v(TAG, "file upload complete");
                }

                Log.v(TAG, "now close");

                //closing the input and output streams
                fileInputStream.close();
                dataOutputStream.flush();
                dataOutputStream.close();
                //connection.disconnect();

                Log.v(TAG, "CLosed ");


            } catch (FileNotFoundException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //showToast("FIle not found");
                    }
                });
            } catch (MalformedURLException e) {
                e.printStackTrace();
                //showToast("URL error");

            } catch (IOException e) {
                e.printStackTrace();
                //showToast("Cannot read/Write file");
            }
            //dialog.dismiss();
            //postData("langitudes----","longitues----");

            //return serverResponseCode;
        }

    }


    void showToast(CharSequence msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void setUI() {
        record_button = (Button) findViewById(R.id.btn_test1);
        record_button.setOnClickListener(record_button_handle);
        record_button.setEnabled(true);

        play_button = (Button) findViewById(R.id.btn_test2);
        play_button.setOnClickListener(play_button_handle);
        play_button.setEnabled(true);

        location_button = (Button) findViewById(R.id.retrieve_location);
        location_button.setOnClickListener(location_button_handle);

        log = (TextView) findViewById(R.id.log);
        logView = (ScrollView) findViewById(R.id.logView);
    }

    private void setMaxVolume() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        preVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        updateLog(" ----> preVolume = " + preVolume, "green");
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        updateLog(" ----> maxVolume = " + maxVolume, "green");
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0);
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        updateLog(" ----> currentVolume = " + currentVolume, "green");
    }

    private void setProperVolume() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        preVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        updateLog(" ----> preVolume = " + preVolume, "green");
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        updateLog(" ----> maxVolume = " + maxVolume, "green");
        int properVolume = (int) ((float) maxVolume * 0.2);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, properVolume, 0);
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        updateLog(" ----> currentVolume = " + currentVolume, "green");
    }

    private void restoreVolume() {
        if (preVolume >= 0) {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, preVolume, 0);
            updateLog(" ----> set preVolume = " + preVolume, "green");
            int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            updateLog(" ----> currentVolume = " + currentVolume, "green");
        }
    }

    private void startRecording() {
        recordingThread.startRecording();
        updateLog(" ----> recording started ...", "green");
        record_button.setText(R.string.btn1_stop);
    }

    private void stopRecording() {
        recordingThread.stopRecording();
        updateLog(" ----> recording stopped ", "green");
        record_button.setText(R.string.btn1_start);
    }

    private void startPlayback() {
        updateLog(" ----> playback started ...", "green");
        play_button.setText(R.string.btn2_stop);

        // (new PcmPlayer()).playPCM();
        playbackThread.startPlayback();
    }

    private void stopPlayback() {
        updateLog(" ----> playback stopped ", "green");
        play_button.setText(R.string.btn2_start);
        playbackThread.stopPlayback();
    }

    private void sleep() {
        try {
            Thread.sleep(500);
        } catch (Exception e) {
        }
    }

    private OnClickListener record_button_handle = new OnClickListener() {
        // @Override
        public void onClick(View arg0) {
            if (record_button.getText().equals(getResources().getString(R.string.btn1_start))) {
                stopPlayback();
                sleep();
                startRecording();
            } else {
                stopRecording();
                sleep();
            }
        }
    };

    private OnClickListener play_button_handle = new OnClickListener() {
        // @Override
        public void onClick(View arg0) {
            if (play_button.getText().equals(getResources().getString(R.string.btn2_start))) {
                stopRecording();
                sleep();
                startPlayback();
            } else {
                stopPlayback();
            }
        }
    };

    private void getLocation() {
        // Here, thisActivity is the current activity
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return ;
        }
        mFusedLocationClient.getLastLocation().addOnSuccessListener(Demo.this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if(location!=null){
                    updateLog(location.toString(),"blue");

                    DatabaseReference myRef = database.getReference("message");

                    myRef.setValue(location);
                }

            }
        });
    }
    private OnClickListener location_button_handle = new OnClickListener() {
        @Override
        public void onClick(View v) {
            RetrieveLocation();

        }
    };
    private void RetrieveLocation(){
        DatabaseReference myRef = database.getReference("message");
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                Location value = dataSnapshot.getValue(Location.class);
                Log.d(TAG, "Value is: " + value);
                location = (TextView)findViewById(R.id.location_view);
                location.setText(value.toString());
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }
    //

    //

    private void requestPermissionLocation(){
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},1);
    }
    private void requestPermissionCamera(){
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},1);
    }
    private void requestPermissionSms(){
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.SEND_SMS},1);
    }
    private void requestPermissionMicrophone(){
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECORD_AUDIO},1);
    }
    private void requestPermissionStorage(){
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
    }
//    @Override
//    public void onRequestPermissionsResult(int requestCode,
//                                           String permissions[], int[] grantResults) {
//        switch (requestCode) {
//            case MY_PERMISSIONS_REQUEST_LOCATION: {
//                // If request is cancelled, the result arrays are empty.
//                if (grantResults.length > 0
//                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    // permission was granted, yay! Do the
//                    // contacts-related task you need to do.
//                } else {
//                    // permission denied, boo! Disable the
//                    // functionality that depends on this permission.
//                }
//                return;
//            }
//
//            // other 'case' lines to check for other
//            // permissions this app might request.
//        }
//    }
//
//    protected void createLocationRequest() {
//        LocationRequest mLocationRequest = new LocationRequest();
//        mLocationRequest.setInterval(10000);
//        mLocationRequest.setFastestInterval(5000);
//        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
//                .addLocationRequest(mLocationRequest);
//
//        SettingsClient client = LocationServices.getSettingsClient(this);
//        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
//
//        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
//            @Override
//            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
//                // All location settings are satisfied. The client can initialize
//                // location requests here.
//                // ...
//
//            }
//        });
//
//        task.addOnFailureListener(this, new OnFailureListener() {
//            @Override
//            public void onFailure(@NonNull Exception e) {
//                if (e instanceof ResolvableApiException) {
//                    // Location settings are not satisfied, but this can be fixed
//                    // by showing the user a dialog.
//                    try {
//                        // Show the dialog by calling startResolutionForResult(),
//                        // and check the result in onActivityResult().
//                        ResolvableApiException resolvable = (ResolvableApiException) e;
//                        resolvable.startResolutionForResult(Demo.this,
//                                MY_PERMISSIONS_REQUEST_LOCATION);
//                    } catch (IntentSender.SendIntentException sendEx) {
//                        // Ignore the error.
//                    }
//                }
//            }
//        });
//    }

    private int findFrontFacingCamera() {
        int cameraId = -1;
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            CameraInfo info = new CameraInfo();
            Camera.getCameraInfo(i, info);
            Log.d(TAG, "Camera found");
            cameraId = i;
            break;
        }
        return cameraId;
    }

    public void updateLog(final String text) {

        log.post(new Runnable() {
            @Override
            public void run() {
                if (currLogLineNum >= MAX_LOG_LINE_NUM) {
                    int st = strLog.indexOf("<br>");
                    strLog = strLog.substring(st + 4);
                } else {
                    currLogLineNum++;
                }
                String str = "<font color='white'>" + text + "</font>" + "<br>";
                strLog = (strLog == null || strLog.length() == 0) ? str : strLog + str;
                log.setText(Html.fromHtml(strLog));
            }
        });
        logView.post(new Runnable() {
            @Override
            public void run() {
                logView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    static int MAX_LOG_LINE_NUM = 200;
    static int currLogLineNum = 0;
    public void updateLog(final String text, final String color) {
        log.post(new Runnable() {
            @Override
            public void run() {
                if (currLogLineNum >= MAX_LOG_LINE_NUM) {
                    int st = strLog.indexOf("<br>");
                    strLog = strLog.substring(st + 4);
                } else {
                    currLogLineNum++;
                }
                String str = "<font color='" + color + "'>" + text + "</font>" + "<br>";
                strLog = (strLog == null || strLog.length() == 0) ? str : strLog + str;
                log.setText(Html.fromHtml(strLog));
            }
        });
        logView.post(new Runnable() {
            @Override
            public void run() {
                logView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }
    private void emptyLog() {
        strLog = null;
        log.setText("");
    }
    @Override
    public void onDestroy() {
        restoreVolume();
        recordingThread.stopRecording();
        super.onDestroy();
    }


//    private void check_permisssions()
//    {
//        if (ContextCompat.checkSelfPermission(getActivity(),
//                Manifest.permission.SEND_SMS) + ContextCompat
//                .checkSelfPermission(getActivity(),
//                        Manifest.permission.CAMERA)+ContextCompat
//                .checkSelfPermission(getActivity(),
//                        Manifest.permission.RECORD_AUDIO)+ContextCompat
//                .checkSelfPermission(getActivity(),
//                        Manifest.permission.WRITE_EXTERNAL_STORAGE)+ContextCompat
//                .checkSelfPermission(getActivity(),
//                        Manifest.permission.ACCESS_COARSE_LOCATION)+ContextCompat
//                .checkSelfPermission(getActivity(),
//                        Manifest.permission.ACCESS_FINE_LOCATION)
//                != PackageManager.PERMISSION_GRANTED) {
//
//            if (ActivityCompat.shouldShowRequestPermissionRationale
//                    (getActivity(), Manifest.permission.SEND_SMS) ||
//                    ActivityCompat.shouldShowRequestPermissionRationale
//                            (getActivity(), Manifest.permission.CAMERA)) ||
//            ActivityCompat.shouldShowRequestPermissionRationale
//                    (getActivity(), Manifest.permission.RECORD_AUDIO)) ||
//            ActivityCompat.shouldShowRequestPermissionRationale
//                    (getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) ||
//            ActivityCompat.shouldShowRequestPermissionRationale
//                    (getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)) ||
//            ActivityCompat.shouldShowRequestPermissionRationale
//                    (getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
//
//            {
//
//                Snackbar.make(getActivity().findViewById(android.R.id.content),
//                        "Please Grant Permissions to upload profile photo",
//                        Snackbar.LENGTH_INDEFINITE).setAction("ENABLE",
//                        new View.OnClickListener() {
//                            @Override
//                            public void onClick(View v) {
//                                requestPermissions(
//                                        new String[]{Manifest.permission
//                                                .SEND_SMS, Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO,
//                                                Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.ACCESS_COARSE_LOCATION,
//                                                Manifest.permission.ACCESS_FINE_LOCATION},
//                                        PERMISSIONS_MULTIPLE_REQUEST);
//                            }
//                        }).show();
//            } else {
//                requestPermissions(
//                        new String[]{Manifest.permission
//                                .SEND_SMS, Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO,
//                                Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.ACCESS_COARSE_LOCATION,
//                                Manifest.permission.ACCESS_FINE_LOCATION},
//                        PERMISSIONS_MULTIPLE_REQUEST);
//            }
//        } else {
//            // write your logic code if permission already granted
//        }
//    }




}
