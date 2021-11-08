#!/usr/bin/env bash

echo "Killing all emulators"
$ANDROID_SDK/platform-tools/adb devices | grep emulator | cut -f1 | while read line; do $ANDROID_SDK/platform-tools/adb -s $line emu kill; done
sleep 10

echo "Starting the emulator: $1"
$ANDROID_SDK/emulator/emulator -avd $1 -wipe-data &
EMULATOR_PID=$!

echo "Waiting for the emulator"
WAIT_CMD="$ANDROID_SDK/platform-tools/adb wait-for-device shell getprop init.svc.bootanim"
until $WAIT_CMD | grep -m 1 stopped; do
  sleep 1
done
sleep 5
echo "$1 is ready."

echo "Unlocking home screen"
$ANDROID_SDK/platform-tools/adb shell input keyevent 82

# Clear and capture logcat
# $ANDROID_SDK/platform-tools/adb logcat -c
# $ANDROID_SDK/platform-tools/adb logcat > build/logcat.log &
# LOGCAT_PID=$!

echo "Running tests"
./gradlew clean connectedAndroidTest -i

# Stop the background processes
#kill $LOGCAT_PID
#echo "Killing the emulator with PID: $EMULATOR_PID"
#kill $EMULATOR_PID
