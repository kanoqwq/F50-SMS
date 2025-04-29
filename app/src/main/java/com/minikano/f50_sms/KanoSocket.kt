package com.minikano.f50_sms

import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets

object KanoSocket {
    private const val AT_BUFFER_SIZE = 2048
    const val FAIL = "FAIL"
    const val OK = "OK"
    private const val SOCKET_NAME = "miscserver"
    const val TAG = "kano_ZTE_LOG"

    class Watchdog(private val socketName: String, private val cmd: String) {
        private var timeoutJob: Job? = null
        private var timeoutCallback: (() -> Unit)? = null

        fun setTimeoutCallback(callback: () -> Unit) {
            this.timeoutCallback = callback
        }

        fun wantEat(timeoutMillis: Long = 3000L) {
            timeoutJob?.cancel()
            timeoutJob = CoroutineScope(Dispatchers.IO).launch {
                delay(timeoutMillis)
                timeoutCallback?.invoke()
            }
        }

        fun feedFood() {
            timeoutJob?.cancel()
        }
    }

    private fun timeout(obj: Any) {
        Log.w(TAG, "socket timeout")
        val socket = obj as? LocalSocket
        try {
            socket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun sendCmd(socketName: String, cmd: String): String? {
        var result: String? = null
        val thread = Thread.currentThread()
        val caller = thread.stackTrace[2].className
        val strCmd = "[$caller][${thread.id}]<${cmd.replace('\n', '\\')}>"
        Log.d(TAG, "$socketName send cmd: $strCmd")
        val socketClient = LocalSocket()
        val socketAddress = LocalSocketAddress(socketName, LocalSocketAddress.Namespace.ABSTRACT)

        try {
            socketClient.connect(socketAddress)
            var ops: OutputStream? = null
            var ins: InputStream? = null
            val buf = ByteArray(AT_BUFFER_SIZE)
            var wd: Watchdog? = null

            try {
                Log.i(TAG, "$strCmd connect $socketName success")
                ops = socketClient.outputStream
                ins = socketClient.inputStream
                ops.write(cmd.toByteArray(StandardCharsets.UTF_8))
                ops.flush()
                Log.d(TAG, "$strCmd write cmd and flush done")

                wd = Watchdog(socketName, cmd)
                wd.setTimeoutCallback { timeout(socketClient) }
                wd.wantEat()

                val count = ins.read(buf, 0, AT_BUFFER_SIZE)
                wd.feedFood()

                result = if (count != -1) {
                    String(buf.copyOf(count), StandardCharsets.UTF_8)
                } else {
                    Log.e(TAG, "$strCmd read failed")
                    ""
                }
                Log.d(TAG, "$strCmd count = $count, result is: $result")

            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                wd?.feedFood()
                try {
                    ops?.close()
                    ins?.close()
                    socketClient.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "connect to $socketName failed", e)
            try {
                socketClient.close()
            } catch (_: IOException) {
            }
        }
        Log.d(TAG, "$strCmd handle over and result is: $result")
        return result
    }

    @JvmStatic
    fun sendCmd(socketName: String, cmd: ByteArray, result: ByteArray): Boolean {
        Log.d(TAG, "$socketName send byte cmd")
        val socketClient = LocalSocket()
        val socketAddress = LocalSocketAddress(socketName, LocalSocketAddress.Namespace.ABSTRACT)

        try {
            socketClient.connect(socketAddress)
            var wd: Watchdog? = null
            try {
                val ops = socketClient.outputStream
                val ins = socketClient.inputStream
                Log.i(TAG, "connect $socketName success")
                ops.write(cmd)
                ops.flush()
                Log.d(TAG, "write cmd and flush done")

                wd = Watchdog(socketName, "byte command")
                wd.setTimeoutCallback { timeout(socketClient) }
                wd.wantEat()

                val count = ins.read(result)
                wd.feedFood()
                Log.d(TAG, "result read done, count=$count")
                return true
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                wd?.feedFood()
                try {
                    socketClient.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "connect to $socketName failed", e)
            try {
                socketClient.close()
            } catch (_: IOException) {
            }
        }
        return false
    }

    @Deprecated("Use sendCmd instead")
    @JvmStatic
    @Synchronized
    fun sendCmdAndRecResult(socketName: String, namespace: LocalSocketAddress.Namespace, strcmd: String): String? {
        Log.d(TAG, "$socketName send cmd: $strcmd")
        val buf = ByteArray(255)
        var socketClient: LocalSocket? = null
        var outputStream: OutputStream? = null
        var inputStream: InputStream? = null
        var result: String? = null

        try {
            socketClient = LocalSocket()
            val socketAddress = LocalSocketAddress(socketName, namespace)
            if (!socketClient.isConnected) {
                socketClient.connect(socketAddress)
            }
            outputStream = socketClient.outputStream
            outputStream?.apply {
                val cmd = "$strcmd\u0000"
                write(cmd.toByteArray(StandardCharsets.UTF_8))
                flush()
            }
            inputStream = socketClient.inputStream
            val count = inputStream.read(buf, 0, 255)
            result = String(buf, 0, count, Charsets.UTF_8)

        } catch (e: IOException) {
            Log.e(TAG, "Failed get output stream: $e")
        } finally {
            try {
                outputStream?.close()
                inputStream?.close()
                socketClient?.close()
            } catch (e: IOException) {
                Log.d(TAG, "catch exception is $e")
            }
        }
        return result
    }

    @JvmStatic
    @Synchronized
    fun sendCmdNoCloseSocket(socketName: String, namespace: LocalSocketAddress.Namespace, strcmd: String): String? {
        val buf = ByteArray(255)
        var result: String? = null

        try {
            val socketClient = LocalSocket()
            val socketAddress = LocalSocketAddress(socketName, namespace)
            socketClient.connect(socketAddress)

            val outputStream = socketClient.outputStream
            val inputStream = socketClient.inputStream

            val cmd = "$strcmd\u0000"
            outputStream.write(cmd.toByteArray(StandardCharsets.UTF_8))
            outputStream.flush()

            val count = inputStream.read(buf, 0, 255)
            result = String(buf, 0, count, Charsets.UTF_8)

            socketClient.close()
        } catch (e: IOException) {
            Log.e(TAG, "Failed get output stream: $e")
        }
        return result
    }

    @JvmStatic
    fun sendSlogModemAt(cmd: String): String? {
        Log.d(TAG, "SendSlogModemAt $cmd")
        return sendCmdAndRecResult(SOCKET_NAME, LocalSocketAddress.Namespace.ABSTRACT, "$cmd")
    }

//    @Synchronized
//    fun sendCmdByAidl(socketName: String?, strcmd: String): String? {
//        var result: String?
//        synchronized(KanoSocket::class.java) {
//            result = null
//            val cmd = strcmd + "\u0000"
//            val logControlAidl: LogControlAidl = mLogctl
//            if (logControlAidl != null) {
//                result = logControlAidl.sendCmd(socketName, cmd)
//            }
//            Log.d(TAG, "aidl cmd = $strcmd, res = $result")
//        }
//        return result
//    }
}