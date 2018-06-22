package ai.kitt.snowboy;

import ai.kitt.snowboy.audio.RecordingThread;
import ai.kitt.snowboy.audio.PlaybackThread;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
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

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


import ai.kitt.snowboy.audio.AudioDataSaver;
import ai.kitt.snowboy.demo.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import static android.content.ContentValues.TAG;
import static com.google.android.gms.internal.zzagz.runOnUiThread;


public class Demo extends Activity {

    private Button record_button;
    private Button play_button;
    private TextView log;
    private ScrollView logView;
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
    private String SERVER_URL = "http://10.225.67.114/UploadToServer.php?";



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        setUI();

        setProperVolume();

        AppResCopy.copyResFromAssetsToSD(this);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        //check_permisssions();
        activeTimes = 0;
        recordingThread = new RecordingThread(handle, new AudioDataSaver());
        playbackThread = new PlaybackThread();
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


    public Handler handle = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            MsgEnum message = MsgEnum.getMsgEnum(msg.what);
            switch (message) {
                case MSG_ACTIVE:
                    activeTimes++;
                    global.Location="";
                    updateLog(" ----> Detected " + activeTimes + " times", "green");
                    // Toast.makeText(Demo.this, "Active "+activeTimes, Toast.LENGTH_SHORT).show();
                    showToast("Active " + activeTimes);
                    //sms_text="HELP ME LALAN KUMAR" + '\n' + "My location is";

                    ////// CAMERA CODE

                    // do we have a camera?
                    if (!getPackageManager()
                            .hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
                        showToast("No camera on this device");

                    } else {
                        cameraId = findFrontFacingCamera();
                        if (cameraId < 0) {
                            showToast("No front facing camera found.");

                        } else {
                            camera = Camera.open(cameraId);
                        }
                    }
                    camera.startPreview();
                    camera.takePicture(null, null, new photohandler(getApplicationContext(), default_location));

                    ///// GPS LOCATION
                    updateLog("before getlocation");
                    getLocation();
                    updateLog("after getlocation");

                    ////// SMS CODE

                    String phoneNo = "8447828766";


                    PackageManager pm = getApplicationContext().getPackageManager();

                    if (!pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) &&
                            !pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_CDMA)) {
                        showToast("Sorry, your device probably can't send SMS...");
                    }

                    try {
                        SmsManager smsManager = SmsManager.getDefault();
                        //smsManager.sendTextMessage(phoneNo, null, sms_text, null, null);
                        showToast("SMS Sent!");
                    } catch (Exception e) {
                        showToast("SMS faild, please try again later!");
                        e.printStackTrace();
                    }


                    uploadFile(global.Location);
                    Log.v(TAG, "globaal"+global.Location);


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


    private void getLocation() {
        updateLog("im in");
        mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {

            @Override
            public void onSuccess(Location location) {

                if (!golablflag) {
                    sms_text = "HELP ME " + "\n " + "longitude-> " + location.getLongitude() + " latitude-> " + location.getLatitude();
                    golablflag = true;
                } else if (location != null) {
                    sms_text = "HELP ME" + "\n " + "longitude-> " + location.getLongitude() + " latitude-> " + location.getLatitude();
                } else {
                    Log.v(TAG, "getLastLocation:exception");
                }
            }
        });
        updateLog("Im out");
    }


    private int findFrontFacingCamera() {
        int cameraId = -1;
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            CameraInfo info = new CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                Log.d(TAG, "Camera found");
                cameraId = i;
                break;
            }
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


    //android upload file to server
    public void uploadFile( String selectedFilePath) {


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
            Log.v(TAG,"gloabal"+selectedFilePath);
            showToast(selectedFilePath);
            showToast("file issue");
            return;
        } else {
            try {

                //Log.v(TAG, "im in try");


                FileInputStream fileInputStream = new FileInputStream(selectedFile);
                URL url = new URL(SERVER_URL);
                connection = (HttpURLConnection) url.openConnection();
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
                showToast("im after path");
                dataOutputStream = new DataOutputStream(connection.getOutputStream());

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

                //Log.v(TAG, "before while");


                //loop repeats till bytesRead = -1, i.e., no bytes are left to read
                while (bytesRead > 0) {
                    //write the bytes read from inputstream
                    dataOutputStream.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }
                //Log.v(TAG, "after while");

                dataOutputStream.writeBytes(lineEnd);
                dataOutputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                serverResponseCode = connection.getResponseCode();
                String serverResponseMessage = connection.getResponseMessage();

                Log.i(TAG, "Server Response is: " + serverResponseMessage + ": " + serverResponseCode);

                //response code of 200 indicates the server status OK
                if (serverResponseCode == 200) {
                    Log.v(TAG, "file upload complete");
                }

                Log.v(TAG, "now close");

                //closing the input and output streams
                fileInputStream.close();
                dataOutputStream.flush();
                dataOutputStream.close();

                Log.v(TAG, "CLosed ");


            } catch (FileNotFoundException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showToast("FIle not found");
                    }
                });
            } catch (MalformedURLException e) {
                e.printStackTrace();
                showToast("URL error");

            } catch (IOException e) {
                e.printStackTrace();
                showToast("Cannot read/Write file");
            }
            dialog.dismiss();
            //postData("langitudes----","longitues----");

            //return serverResponseCode;
        }

    }



}
