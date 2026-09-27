package com.example.eyewearagentapp.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build

class BluetoothScoManager(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    /**
     * Bluetooth SCO対応デバイス（Eyewear等）が接続されているか確認
     */
    fun isScoDeviceConnected(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
                for (device in devices) {
                    if (device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                        device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                    ) {
                        return true
                    }
                }
            }
            @Suppress("DEPRECATION")
            audioManager.isBluetoothScoAvailableOffCall
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Bluetooth SCOによるマイク音声入力を開始
     */
    fun startSco() {
        try {
            if (!audioManager.isBluetoothScoOn) {
                @Suppress("DEPRECATION")
                audioManager.startBluetoothSco()
                audioManager.isBluetoothScoOn = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Bluetooth SCO入力を停止
     */
    fun stopSco() {
        try {
            if (audioManager.isBluetoothScoOn) {
                @Suppress("DEPRECATION")
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
