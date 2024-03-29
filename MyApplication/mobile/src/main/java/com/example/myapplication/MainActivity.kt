package com.example.myapplication

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.support.v4.content.LocalBroadcastManager
import android.content.Intent
import android.content.IntentFilter
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import java.io.*
import java.util.*
import java.util.concurrent.ExecutionException
import android.R.attr.path
import android.net.Uri
import android.os.*
import java.net.URI


class MainActivity : AppCompatActivity(), MessageClient.OnMessageReceivedListener {

    private val TAG = "MainActivity"
    internal var datapath = "/message_path"
    protected var handler: Handler? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
    }

    private fun currentTimeStr(): String {
        val c = Calendar.getInstance()
        val df = SimpleDateFormat("HH:mm:ss")
        return df.format(c.time)
    }

    fun logthis(msg:String)
    {
        val filename = SimpleDateFormat("hh_mm_ss").format(Calendar.getInstance().time) + ".txt"
        val filePath = filesDir.absolutePath+"/"+filename
        openFileOutput(filename, Context.MODE_PRIVATE).use {
            it.write(msg.toByteArray())
        }
        Toast.makeText(this, "Saved to file: " + File(filePath).exists(), Toast.LENGTH_LONG).show()

        // Sending per email does not work from a certain size on

        /*
        val emailIntent = Intent(android.content.Intent.ACTION_SEND)
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        emailIntent.setType("text/plain")
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf("d.p.rosenbusch@student.utwente.nl", "j.m.krooneman@student.utwente.nl", "a.jung@student.utwente.nl" ))
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,"SensorData @ ${currentTimeStr()}")
        emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(File(filePath)))
        emailIntent.setType("message/rfc822");

        try {
            startActivity(Intent.createChooser(emailIntent,
                "Send email using..."))
        } catch (e:Exception) {
            Toast.makeText(this,
                "#oops",
                Toast.LENGTH_SHORT).show()
            Log.i(TAG, msg)
            Log.e(TAG, "#oops:", e)
        }
        */
    }

    public override fun onResume() {
        super.onResume()
        Wearable.getMessageClient(this).addListener(this)
    }

    public override fun onPause() {
        super.onPause()
        Wearable.getMessageClient(this).removeListener(this)
    }

    private val sdf:SimpleDateFormat = SimpleDateFormat("hh:mm:ss")

    /**
     * This is a simple receiver add/removed in onResume/onPause
     * It receives the message from the wear device and displays to the screen.
     *
     */
    override fun onMessageReceived(messageEvent: MessageEvent) {
        val bais = ByteArrayInputStream(messageEvent.data)
        try {
            val data:Array<MutableMap<Date, FloatArray>> = ObjectInputStream(bais).readObject() as Array<MutableMap<Date, FloatArray>>
            Log.i(TAG, "elements found: " + data.size)
            var message:String = "AC DATA\n"

            val ac_responses = data[0]
            val hr_responses = data[1]

            for(mm:MutableMap.MutableEntry<Date, FloatArray> in ac_responses.entries)
            {
                message = message.plus(sdf.format(mm.key)+":\t"+mm.value.contentToString()+"\n")
            }
            message = message.plus("\nHR DATA\n")

            for(mm:MutableMap.MutableEntry<Date, FloatArray> in hr_responses.entries)
            {
                message = message.plus(sdf.format(mm.key)+":\t"+mm.value.contentToString()+"\n")
            }
            Log.i(TAG, "\n\n\nmessage: " + message)

            logthis(message)
        } finally {
            try {
                if (bais != null) {
                    bais.close()
                }
            } catch (exce: IOException) {
                Log.i("Ohshit", exce.stackTrace.toString())
            }
        }
    }


}