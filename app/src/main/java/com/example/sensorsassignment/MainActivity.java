package com.example.sensorsassignment;

import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.view.View;

import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

import android.widget.TextView;
import android.hardware.SensorManager;
import android.hardware.Sensor;
import android.content.Context;
import android.widget.Toast;

import org.rajawali3d.surface.IRajawaliSurface;
import org.rajawali3d.surface.RajawaliSurfaceView;

import java.text.DecimalFormat;



public class MainActivity extends AppCompatActivity implements SensorEventListener, View.OnClickListener {

    private SensorManager mSensorManager;
    TextView t1,t2,t3;
    Button b,b1;
    FrameLayout flayout;
    boolean on = true, is_relative = false;

    Handler myHandler;

    Sensor accelerometer;
    Sensor gyro;
    Sensor magnet;
    Sensor gravity;

    long gyro_timeStamp;
    float[] gyro_angles = {0,0,0};

    float[] linear_acceleration = null;
    float[] magnetic_field = null;
    float[] accMagAngles = null;

    static float Fuse_ALPHA = 0.98f;

    float[] fused_angles = {0,0,0};

    //graphics
    Renderer renderer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //UI components
        t1 = findViewById(R.id.t1);
        t2 = findViewById(R.id.t2);
        t3 = findViewById(R.id.t3);
        b = findViewById(R.id.button);
        b1 = findViewById(R.id.button2);
        flayout= findViewById(R.id.f);

        myHandler = new Handler();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        magnet = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

        //button click event registration
        b.setOnClickListener(this);
        b1.setOnClickListener(this);


        //3d display
        final RajawaliSurfaceView surface = new RajawaliSurfaceView(flayout.getContext());
        surface.setFrameRate(20.0);
        surface.setRenderMode(IRajawaliSurface.RENDERMODE_WHEN_DIRTY);
        renderer = new Renderer(flayout.getContext());
        renderer.getCurrentScene().initScene();
        surface.setSurfaceRenderer(renderer);
        flayout.addView(surface);
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {

        float[] angular_velocities;

        switch (event.sensor.getType()) {
            case Sensor.TYPE_GYROSCOPE:
                angular_velocities = event.values.clone();
                long interval = event.timestamp - gyro_timeStamp;
                if(gyro_timeStamp <= 0){ // to avoid the initial interval which is very large nnumber
                    gyro_timeStamp = event.timestamp;
                    break;
                }
                gyro_timeStamp = event.timestamp;
                float dt = interval * 0.000000001f;//converting the  nano seconds to seconds
                gyro_angles[0] = (float) (gyro_angles[0] + (angular_velocities[0] * dt * 57.3))%360;
                gyro_angles[1] = (float) (gyro_angles[1] + (angular_velocities[1] * dt * 57.3))%360;
                gyro_angles[2] = (float) (gyro_angles[2] + (angular_velocities[2] * dt * 57.3))%360;

                break;

            case Sensor.TYPE_MAGNETIC_FIELD:
                magnetic_field  = event.values.clone();
                break;

            case Sensor.TYPE_ACCELEROMETER:
                linear_acceleration = event.values.clone();
                break;
        }

        if(null != linear_acceleration && null != magnetic_field && null != gyro_angles) {
            accMagAngles = CalculateAnglesFromAccelerationMagnetometer(linear_acceleration, magnetic_field);
            gyro_angles = fuse(accMagAngles.clone(), gyro_angles.clone());

            DisplayAngles uiTask = new DisplayAngles(gyro_angles);
            myHandler.post(uiTask);
        }

    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();
        b.setText("Start");
        mSensorManager.unregisterListener(this);
        on = true;
    }

    @Override
    public void onClick(View v) {
        if(R.id.button2 == v.getId()){
            if(!on) {
                if (is_relative) {
                    for (int i = 0; i < gyro_angles.length; i++) {
                        gyro_angles[i] = accMagAngles[i];

                    }
                    Fuse_ALPHA = 1f;
                    Toast toast = Toast.makeText(getApplicationContext(), "Angles changed to relative", Toast.LENGTH_SHORT);
                    toast.show();
                    b1.setText("Change to Absolute");
                    is_relative = false;
                } else {
                    for (int i = 0; i < gyro_angles.length; i++) {
                        gyro_angles[i] = 0;
                    }
                    is_relative = true;
                    Fuse_ALPHA = 1f;
                    Toast toast = Toast.makeText(getApplicationContext(), "Angles changed to absolute", Toast.LENGTH_SHORT);
                    toast.show();
                    b1.setText("Change to Relative");
                }
            }else{
                Toast toast = Toast.makeText(getApplicationContext(), "Please Click on Start", Toast.LENGTH_SHORT);
                toast.show();
            }

        }else if(R.id.button == v.getId()) {
            if (on) {
                mSensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_NORMAL);
                mSensorManager.registerListener(this, magnet, SensorManager.SENSOR_DELAY_NORMAL);
                mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
                b.setText("Stop");
                on = false;
            } else {
                b.setText("Start");
                mSensorManager.unregisterListener(this);
                on = true;
            }
        }
    }

    public static float[] fuse(float[] acc,  float[] gyro) {
        if (null != acc && null != gyro){
            for(int i = 0; i< acc.length-1;i++){
                gyro[i] = Fuse_ALPHA*gyro[i] + ((1-Fuse_ALPHA)*acc[i]);
            }
        }
        return gyro;
    }

    public float [] CalculateAnglesFromAccelerationMagnetometer(float[] linear_acceleration, float[] magnetic_field){
        Float pitch_denominator = (float) Math.sqrt(linear_acceleration[0]*linear_acceleration[0]+linear_acceleration[2]*linear_acceleration[2]);
        Float roll_denominator = (float) Math.sqrt(linear_acceleration[1]*linear_acceleration[1]+linear_acceleration[2]*linear_acceleration[2]);
        Double pitch = 57.3*Math.atan2(linear_acceleration[1], pitch_denominator);
        Double roll = 57.3*Math.atan2(-linear_acceleration[0], roll_denominator);

        float[] returnValue =  {roll.floatValue(), pitch.floatValue(), 0.0f};

        returnValue[2] = calculateYawAnglesFromMagnet(magnetic_field);
        return returnValue;
    }

    public float calculateYawAnglesFromMagnet(float[] magnetic_field){
        float xDataLSB = magnetic_field[0];
        float yDataLSB = magnetic_field[1];
        float xGaussData = xDataLSB;
        float yGaussData = yDataLSB;
        float direction;
        if(xGaussData == 0){
            if(yGaussData < 0){
                direction = 90;
            }else{
                direction = 0;
            }
        }else{
            direction = (float) Math.atan2(yGaussData,xGaussData)*57.3f;
            if(direction > 359){
                direction = direction - 359;
            }
            else if(direction < 0){
                direction = direction + 359;
            }
        }

        return direction;

    }

    //Handler Class
    public class DisplayAngles implements Runnable {
        float[] displayAngles = {0,0,0};
        DisplayAngles(float[] displayAngles){
            this.displayAngles = displayAngles;
        }
        @Override
        public void run() {
            t1.setText(String.valueOf(formatNumber2(displayAngles[0])));
            t2.setText(String.valueOf(formatNumber2(displayAngles[1])));
            t3.setText(String.valueOf(formatNumber2(displayAngles[2])));
            renderer.angles = displayAngles;

        }
    }

    public static Integer formatNumber2(Float n){
        DecimalFormat df = new DecimalFormat("##");
        String x = df.format(n);
        Integer y  = Integer.parseInt(x);

        return y;
    }

}
