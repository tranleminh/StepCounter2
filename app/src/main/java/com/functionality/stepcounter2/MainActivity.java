package com.functionality.stepcounter2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements SensorEventListener{

    private static final String FILE_NAME = "StepCounter";
    private static final String EXTENSION = ".csv";
    private static final String FIRST_LINE = "Timestamp; Delta\n";
    private static final SimpleDateFormat df = new SimpleDateFormat("dd-MM-YYYY_hh-mm-ss");
    private StorageReference mStorageRef;
    private FirebaseDatabase db;
    private DatabaseReference dbRef;

    private String lines = "";
    private long moment = -1;
    private long delta = 0;
    //private String currentFileName;

    private SensorManager sensorManager;
    private Sensor sensor;
    private TextView TvSteps;
    private EditText TvName;
    private TextView Status;
    private TextView Log;
    private Button BtnStart;
    private Button BtnStop;

    private int nbSteps = 0;

    public void createTextFile(String username, String date, String lines) {
        String name = FILE_NAME +  "_" + username + "_" + date + EXTENSION;
        //currentFileName = name;
        //File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), name);
        FileOutputStream fos = null;
        try {
            fos = openFileOutput(name, MODE_PRIVATE);
            fos.write(FIRST_LINE.getBytes());
            fos.write(lines.getBytes());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                    Uri file = Uri.fromFile(new File(getFilesDir(), name));
                    StorageReference fileRef = mStorageRef.child(name);
                    Toast.makeText(MainActivity.this, "URI : " + file, Toast.LENGTH_LONG).show();
                    fileRef.putFile(file)
                            .continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                                @Override
                                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                                    if (!task.isSuccessful()) {
                                        throw task.getException();
                                    }
                                    return mStorageRef.getDownloadUrl();
                                }
                            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            if (task.isSuccessful()) {
                                //Uri downloadUri = task.getResult();
                                Log.setText("Upload complete");
                            } else {
                                Log.setText("upload failed: " + task.getException().getMessage());
                                //dbRef.setValue(currentFileName);
                            }
                        }
                    });
                    deleteFile(name);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseDatabase.getInstance();
        dbRef = db.getReference();

        TvSteps = findViewById(R.id.tv_steps);
        Log = findViewById(R.id.log);
        TvName = findViewById(R.id.tv_name);
        Status = findViewById(R.id.status);
        BtnStart = findViewById(R.id.btn_start);
        BtnStop = findViewById(R.id.btn_stop);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        mStorageRef = FirebaseStorage.getInstance().getReference();

        BtnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Click again on start button to reset all measurements
                Status.setText("Counter started");
                TvSteps.setText("Walked steps : " + nbSteps);
                //Distance.setText("Walked distance : " + dist + "m");

                sensorManager.registerListener(MainActivity.this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        });

        BtnStop.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                Date date = Calendar.getInstance().getTime();
                sensorManager.unregisterListener(MainActivity.this);
                Status.setText("Counter stopped");
                createTextFile(TvName.getText().toString(), df.format(date), lines);
                lines = "";
                moment = -1;
                delta = 0;
                nbSteps = 0;
            }
        });
    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            nbSteps++;
            if (moment != -1) {
                delta = (sensorEvent.timestamp/1000000 - moment);
            }
            moment = sensorEvent.timestamp/1000000;
        }
        TvSteps.setText("Walked steps : " + nbSteps);
        if (delta > 100)
            lines += moment + "; " + delta + "\n ";
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
