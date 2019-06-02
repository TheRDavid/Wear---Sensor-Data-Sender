Wear - Sensor Data Sender

Affective Computing Project - Setup Steps

- Download Open Face
  - Windows: https://github.com/TadasBaltrusaitis/OpenFace/wiki/Windows-Installation
  - Linux/Mac: http://cmusatyalab.github.io/openface/setup/
- Checkout the Watch sensor reader: https://github.com/TheRDavid/Wear---Sensor-Data-Sender (open in Android Studio)
- Connect an Android-phone and watch to your computer (make sure usb-debugging is enabled for both)
- Run the _mobile_-configuration on the phone, the _wear_-config on the watch
- Disconnect the watch (keep phone connected)
- Run OpenFace on your computer
- Set an output-location for the video- and data-file under _Recording settings_
- Select _File -> Open webcam_ and choose the highest resolution, then hit _select camera_
- Once the recording started, hit _Pause_ and wait a few seconds (this gap in time will show in the .csv file so we can use it for timing)
- Hit _Start_ on the watch to record data at the same as _Resume_ in Open Face
- Hit _Finish_ on the watch and _Stop_ in Open Face after the recording

View sensor data files:

In Android studio, go

_View -> Tool Windows -> Device File Explorer_

and navigate to

_/data/data/com.example.myapplication/files_
