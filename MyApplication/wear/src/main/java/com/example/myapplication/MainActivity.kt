package com.example.myapplication

import android.Manifest
import android.app.PendingIntent.getActivity
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.Bundle
import android.support.wearable.activity.WearableActivity
import android.support.wearable.view.WatchViewStub
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.wearable.intent.RemoteIntent
import java.lang.Exception
import android.R.id.message
import android.R.attr.start
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.Node
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectOutput
import java.io.ObjectOutputStream
import java.util.*
import java.util.concurrent.ExecutionException


class MainActivity : WearableActivity(), SensorEventListener, View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback {

    private val TAG = "MainActivity"
    var record = false
    var datapath = "/message_path"

    private val Type_AC = 0
    private val TYPE_HR = 1

    private val ac_responses = mutableMapOf<Date, FloatArray>()
    private val hr_responses = mutableMapOf<Date, FloatArray>()

    override fun onCreate(savedInstanceState: Bundle?) {

        Log.i(TAG,"Starting")
        // Keep the Wear screen always on (for testing only!)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (checkSelfPermission(Manifest.permission.BODY_SENSORS)
            != PackageManager.PERMISSION_GRANTED) {

            Log.i(TAG,"Requesting Permission")
            requestPermissions(
                Array<String>(1) { Manifest.permission.BODY_SENSORS },
                152)
        }
        else{
            Log.i(TAG,"ALREADY GRANTED")
            registerSensorListeners()
            var button = findViewById<Button>(R.id.button)
            button.setOnClickListener(this)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            152 -> {
                Log.i(TAG,"Feedback for Permission")
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Log.i(TAG,"Granted Permission")

                    registerSensorListeners()
                    var button = findViewById<Button>(R.id.button)
                    button.setOnClickListener(this)
                } else {
                    Toast.makeText(this, "ugh, what?!", Toast.LENGTH_LONG)
                }
                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }

    override fun onClick(v: View?) {
        if(v?.id == R.id.button)
        {
            var button = findViewById<Button>(R.id.button)
            record = !record
            button.setText("Finish")
            if(!record) {
                button.setText("Start")
                SendThread(datapath, arrayOf(ac_responses, hr_responses)).start()
            }
        }
    }
    private fun registerSensorListeners() {
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        val motionSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        sensorManager.registerListener(this, heartRateSensor, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, motionSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override
    fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        Log.i(TAG, "onAccuracyChanged - accuracy: $accuracy")
    }

    override
    fun onSensorChanged(event: SensorEvent) {
        if (!record) return
        val c = Calendar.getInstance()
        Log.i(TAG, event.values.contentToString())
        when (event.sensor.type) {
            Sensor.TYPE_HEART_RATE -> hr_responses[c.time] = event.values.copyOf()
            Sensor.TYPE_ACCELEROMETER -> ac_responses[c.time] = event.values.copyOf()
            else -> Log.i(TAG, "Unknown sensor type")
        }
    }
    internal inner class SendThread//constructor
        (var path: String, var message: Array<MutableMap<Date, FloatArray>>) : Thread() {

        fun toByteArray(message: Array<MutableMap<Date, FloatArray>>):ByteArray
        {
            var bos:ByteArrayOutputStream = ByteArrayOutputStream()
            var out:ObjectOutput
            try {
                out = ObjectOutputStream(bos)
                out.writeObject(message)
                out.flush()
                return bos.toByteArray()
            } finally {
                try {
                    bos.close()
                } catch (ex: IOException) {
                    // ignore close exception
                }
            }
        }

        //sends the message via the thread.  this will send to all wearables connected, but
        //since there is (should only?) be one, so no problem.
        override fun run() {
            //first get all the nodes, ie connected wearable devices.
            val nodeListTask = Wearable.getNodeClient(applicationContext).connectedNodes
            try {
                // Block on a task and get the result synchronously (because this is on a background
                // thread).
                val nodes = Tasks.await<List<Node>>(nodeListTask)

                //Now send the message to each device.
                for (node in nodes) {
                    val sendMessageTask = Wearable.getMessageClient(this@MainActivity)
                        .sendMessage(node.getId(), path, toByteArray(message))

                    try {
                        // Block on a task and get the result synchronously (because this is on a background
                        // thread).
                        val result = Tasks.await(sendMessageTask)
                        Log.v("blub", "SendThread: message send to " + node.displayName)

                    } catch (exception: ExecutionException) {
                        Log.e("blub", "Task failed: $exception")

                    } catch (exception: InterruptedException) {
                        Log.e("blub", "Interrupt occurred: $exception")
                    }

                }

            } catch (exception: ExecutionException) {
                Log.e("blub", "Task failed: $exception")

            } catch (exception: InterruptedException) {
                Log.e("blub", "Interrupt occurred: $exception")
            }

        }
    }
}
