package com.example.gestureextraction;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;


import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Objects;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int VIDEO_RECORD_CODE = 101;
    private VideoView v;
    private String randomfilename;
    private static File mediaFile;

    protected String characters = "0123456789";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MediaController mediaController = new MediaController(this);
        v = findViewById(R.id.videoViewScreen3);
        v.setMediaController(mediaController);

        // Check if phone has a camera
        if (isCameraPresentInPhone()) {
            Log.i("VIDEO_RECORD_TAG", "Camera is detected.");
            getCameraPermissions();
        } else {
            Log.i("VIDEO_RECORD_TAG", "No camera detected.");
        }

        Button recordButton = (Button) findViewById(R.id.recordButton);
        if (!isCameraPresentInPhone()) {
            recordButton.setEnabled(false);
        } else {
            getCameraPermissions();
            recordButton.setEnabled(true);
        }

        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startRecording();
            }
        });

        // Upload the mp4 to the webserver
        Button uploadButton = (Button) findViewById(R.id.uploadButton);
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                assert mediaFile != null;
                if (mediaFile.exists()
                        && mediaFile != null
                        ) {
                    UploadTask up1 = new UploadTask();
                    Toast.makeText(getApplicationContext(), "Stating to Upload", Toast.LENGTH_LONG).show();
                    up1.execute();
                } else if (!mediaFile.exists()) {
                    Toast.makeText(getApplicationContext(), "You must make a video to upload.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public void startRecording() {
        File dir = new File(Environment.getExternalStorageDirectory() + "/Gestures/");
        if (!dir.exists()) {
            dir.mkdir();
        }
        Random rng = new Random();
        randomfilename = generateString(rng, characters, 10);
        System.out.println(randomfilename);
        mediaFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Gestures/" + randomfilename + ".mp4");

        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 5);
        Uri fileUri = Uri.fromFile(mediaFile);

        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
        startActivityForResult(intent, VIDEO_RECORD_CODE);
    }

    public static String generateString(Random rng, String characters, int length)
    {
        char[] text = new char[length];
        for (int i = 0; i < length; i++)
        {
            text[i] = characters.charAt(rng.nextInt(characters.length()));
        }
        return new String(text);
    }

    protected void onActivityResult(int requestCode,
                                    int resultCode, Intent data) {

        if (requestCode == VIDEO_RECORD_CODE) {
            if (resultCode == RESULT_OK) {
                Log.e("RECORD_VIDEO_TAG", "Video is recorded and available at path" + mediaFile.getPath());
                v.setVideoPath(mediaFile.getPath());
                v.start();
            } else if (resultCode == RESULT_CANCELED) {
                Log.e("RECORD_VIDEO_TAG", "Video is recording is cancelled.");
            } else {
                Log.e("RECORD_VIDEO_TAG", "Video is recording failed.");
            }
        }
    }

    private boolean isCameraPresentInPhone() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
    }

    private void getCameraPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }
    }

    public class UploadTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... strings) {
            String TAG = "UPLOAD_VIDEO";

            FTPClient ftpClient = new FTPClient();
            //use same server?
            String server = getString(R.string.server);
            String username = getString(R.string.username);
            String password = getString(R.string.password);

            try {
                ftpClient.connect(server, 21);
                ftpClient.login(username, password);
                ftpClient.enterLocalPassiveMode();
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

                Log.e(TAG, "FTP Reply String:" + ftpClient.getReplyString());

                ftpClient.changeWorkingDirectory("GestureExamples"); //create new directory in this server??
                Log.e(TAG, "FTP Reply String:" + ftpClient.getReplyString());

                File videoFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Gestures/" + randomfilename + ".mp4");

                InputStream inputStream = new FileInputStream(videoFile);
                ftpClient.storeFile( randomfilename + ".mp4", inputStream);
                Log.e(TAG, "FTP Reply String:" + ftpClient.getReplyString());
                inputStream.close();

                ftpClient.logout();
                ftpClient.disconnect();

                return null;
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }


        @Override
        protected void onProgressUpdate(String... text) {
            Toast.makeText(getApplicationContext(), "In Background Task " + text[0], Toast.LENGTH_LONG).show();
        }

    }
}
