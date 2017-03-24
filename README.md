# PiControl
Simple Android App, REST client for Raspberry Pi robot. It connects via WiFi hotspot
or IP address to a Raspberry Pi robot running PiControlServer (see 
https://github.com/adamconnors/PiControlServer). It uses a Webview to show a
video stream from the robot (if available) and sends HTTP requests in response
to user input. Used as a remote controller for my robot project.

git clone https://github.com/adamconnors/PiControl.git
cd PiControl
./gradlew build
adb install -r app/build/outputs/apk/app-debug.apk

