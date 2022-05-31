package com.jinkeen.vtm.detect.serialport

import android_serialport_api.SerialPort
import com.jinkeen.base.util.d
import com.jinkeen.base.util.e
import com.jinkeen.base.util.toHexBytes
import java.io.File
import java.io.OutputStream
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class LightSerialPortManager private constructor() {

    companion object {
        private val INSTANCE: LightSerialPortManager by lazy { LightSerialPortManager() }

        operator fun invoke(): LightSerialPortManager = INSTANCE

        private const val TAG = "LightSerialPortManager"
    }

    private var serialPort: SerialPort? = null
    private var outputStream: OutputStream? = null

    fun open(devicePath: String, baudrate: String) {
        thread(start = true) {
            try {
                serialPort?.close()

                val device = File(devicePath)
                val baurateValue = baudrate.toIntOrNull() ?: 0
                serialPort = SerialPort(device, baurateValue, 0)

                outputStream = serialPort!!.outputStream
            } catch (t: Throwable) {
                e(t, "打开串口失败", tag = TAG)
                close()
            }
        }
    }

    private val executorService = ThreadPoolExecutor(0, 5, 0, TimeUnit.SECONDS, SynchronousQueue())

    fun close() {
        if (!executorService.isShutdown)
            executorService.execute {
                try {
                    outputStream?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                serialPort?.close()
            }
        executorService.shutdown()
    }

    fun sendCommand(command: String) {
        if (executorService.isShutdown) return
        executorService.execute {
            try {
                outputStream?.write(command.toHexBytes())
                d("命令发送完成：", command)
            } catch (e: Exception) {
                e(e, "命令发送失败：", command)
            }
        }
    }
}