package com.jinkeen.vtm.detect.serialport

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.IBinder
import android.os.SystemClock
import android_serialport_api.SerialPort
import com.jinkeen.base.util.d
import com.jinkeen.base.util.e
import com.jinkeen.base.util.toHexBytes
import com.jinkeen.vtm.detect.BuildConfig
import com.jinkeen.vtm.detect.util.toHexString
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.lang.ref.SoftReference
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.fixedRateTimer

class CashSerialPortService : Service() {

    inner class CashSerialBinder : Binder() {

        fun getService(): CashSerialPortService = this@CashSerialPortService
    }

    private val binder = CashSerialBinder()

    private val receiver = CashSerialPortReceiver(this)

    override fun onCreate() {
        super.onCreate()
        this.openCashDevice()
        this.registerReceiver(receiver, IntentFilter().apply {
            addAction(ACTION_RESET)
            addAction(ACTION_FORCE_RESET)
            addAction(ACTION_START_CASHIN)
            addAction(ACTION_TOTAL_MONEY_RESET)
        })
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        this.closeCashDevice()
        this.unregisterReceiver(receiver)
        super.onDestroy()
    }

    companion object {

        private const val TAG = "CashSerialPortService"

        /** 钱箱复位 */
        private const val COM_RESET = "0203063041B3"

        /** 进钞开始到结束的过程，注意200ms轮询一次来获取钱箱不同状态 */
        private const val COM_POLL = "02030633DA81"

        /** 启用账单，开始入钞 */
        private const val COM_START_CASHIN = "02030C34FFFFFFFFFFFFFEF7"

        /** 告诉钱箱已正确收到指令 */
        private const val COM_ACK = "02030600C282"

        /** 复位 */
        const val ACTION_RESET = "${BuildConfig.APPLICATION_ID}.RESET"

        /** 强制复位，参见[forceReset] */
        const val ACTION_FORCE_RESET = "${BuildConfig.APPLICATION_ID}.FORCE_RESET"

        /** 开始进钞 */
        const val ACTION_START_CASHIN = "${BuildConfig.APPLICATION_ID}.START_CASHIN"

        /** 总金额归零 */
        const val ACTION_TOTAL_MONEY_RESET = "${BuildConfig.APPLICATION_ID}.TOTAL_MONEY_RESET"

        class CashSerialPortReceiver(service: CashSerialPortService) : BroadcastReceiver() {

            private val reference = SoftReference(service)

            override fun onReceive(context: Context, intent: Intent?) {
                intent?.action?.let {
                    d("接收到钱箱操作广播：", it, tag = TAG)
                    when (it) {
                        ACTION_RESET -> reference.get()?.reset()
                        ACTION_FORCE_RESET -> reference.get()?.forceReset()
                        ACTION_START_CASHIN -> reference.get()?.startCashin()
                        ACTION_TOTAL_MONEY_RESET -> reference.get()?.resetTotalMoney()
                        else -> d("Nothing", tag = TAG)
                    }
                }
            }
        }
    }

    private val logRecordQueue = ConcurrentLinkedDeque<String>()

    fun getCashLastMessage(): String? = logRecordQueue.poll()

    private var serialPort: SerialPort? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private val executorService = ThreadPoolExecutor(0, 22, 0, TimeUnit.SECONDS, SynchronousQueue())

    // 确保打开和关闭有先后顺序，无论谁先谁后。
    private val deviceLock = Any()

    private var loopMessageTimer: Timer? = null
    private var receivedTimer: Timer? = null

    private fun openCashDevice() {
        if (executorService.isShutdown) return
        executorService.execute {
            synchronized(deviceLock) {
                var isOpened = false
                logRecordQueue.add("准备连接钱箱")
                try {
                    serialPort?.close()

                    serialPort = SerialPort(File("/dev/ttyS6"), 9600, 0)

                    inputStream = serialPort!!.inputStream
                    outputStream = serialPort!!.outputStream

                    val received = ByteArray(1024)
                    receivedTimer = fixedRateTimer("cash_received_task", false, 0, 10) {
                        try {
                            inputStream?.let {
                                val available = it.available()
                                if (available > 0) {
                                    val size = it.read(received)
                                    if (size > 0) onDataReceive(received, size)
                                }
                            }
                        } catch (e: Exception) {
                            e(e, "读取数据出现异常", tag = TAG)
                            logRecordQueue.add("读取数据出现异常")
                        }
                    }

                    this.send(COM_RESET)
                    loopMessageTimer = fixedRateTimer("cash_loop_poll_task", false, 0, 200) { send(COM_POLL) }

                    isOpened = true
                    d("钱箱串口已打开", tag = TAG)
                    logRecordQueue.add("钱箱串口已打开")
                } catch (e: Exception) {
                    isOpened = false
                    e(e, "串口打开出现异常", tag = TAG)
                    logRecordQueue.add("串口打开出现异常")
                } finally {
                    if (!isOpened) {
                        loopMessageTimer?.cancel()
                        receivedTimer?.cancel()
                        try {
                            outputStream?.close()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        serialPort?.close()
                        d("钱箱已关闭。", tag = TAG)
                        logRecordQueue.add("钱箱已关闭[1]")
                    }
                }
            }
        }
    }

    /**
     * 关闭钱箱
     */
    private fun closeCashDevice() {
        synchronized(deviceLock) {
            loopMessageTimer?.cancel()
            receivedTimer?.cancel()
            d("POLL消息和RECEIVED消息轮询已停止。", tag = TAG)
        }
        if (executorService.isShutdown) return
        executorService.execute {
            synchronized(deviceLock) {
                try {
                    outputStream?.close()
                } catch (e: Exception) {
                    e(e, "钱箱串口关闭异常", tag = TAG)
                    logRecordQueue.add("钱箱串口关闭异常")
                }
                serialPort?.close()
                d("钱箱已关闭。", tag = TAG)
                logRecordQueue.add("钱箱已关闭[2]")
            }
        }
        executorService.shutdown()
    }

    /**
     * 开始进钞，发送进钞指令
     */
    private fun startCashin() {
        if (executorService.isShutdown) return
        executorService.execute { this.send(COM_START_CASHIN) }
    }

    /**
     * 总金额归零
     */
    private fun resetTotalMoney() {
        receivedCashTotalMoney.set(0)
        d("接收金额已复位=", receivedCashTotalMoney.get(), tag = "CashPayActivity")
    }

    /**
     * 正常的复位，指令将被添加到发送队列，并保证一定会在之后的某个时间被执行。
     * 具体执行时间取决于队列中之前的指令集数量
     */
    private fun reset() {
        if (executorService.isShutdown) return
        executorService.execute { this.send(COM_RESET) }
    }

    /**
     * 强制复位。但并不会每次都能强制执行，这取决于内部指令接收标识是否正常，只有在非正常的情况下才会强制执行。
     */
    private fun forceReset() {
        // 偶尔会发生指令发送后得不到钱箱的回应，此时应采取强制复位，并将指令队列清空。
        synchronized(receivedLock) {
            // 只有当没有正常收到指令的情况下，才会真正的强制执行。
            if (!isReceivedCommand) {
                synchronized(sendLock) {
                    outputStream?.write(COM_RESET.toHexBytes())
                    outputStream?.flush()
                }
                commandQueue.clear()
            }
            isReceivedCommand = true
        }
    }

    // 接收现金状态
    private val receiveCashProgress = AtomicInteger(0)

    // 接收到的有效现金总额
    private val receivedCashTotalMoney = AtomicInteger(0)

    // 是否已接收到指令
    private var isReceivedCommand = true
    private var receivedLock = Any()

    // 处理获取到的数据
    private fun onDataReceive(received: ByteArray, size: Int) {
        val ret = received.toHexString(0, size)
        this.parseCashStateMessage(ret)
        d("接收到的指令：", ret, tag = TAG)
        logRecordQueue.add("接收到的指令：$ret")
        // 若收到指令!=ACK，则向钱箱发送ACK指令以表示正确收到了回复。
        if (ret != COM_ACK) executorService.execute { this.s(COM_ACK) }
        when (ret) {
            // 纸币器上电完成
            "020306104392" -> {
                d("纸币器上电完成", tag = TAG)
                logRecordQueue.add("纸币器上电完成")
            }
            // 纸币器idling状态，等待用户塞入纸币
            "0203061467D4" -> {
                d("等待用户塞入纸币", tag = TAG)
                logRecordQueue.add("等待用户塞入纸币")
            }
            // 纸币器进钱状态
            "02030615EEC5" -> {
                d("纸币器处于入钞状态", tag = TAG)
                logRecordQueue.add("纸币器处于入钞状态")
                receiveCashProgress.set(0)
            }
            // 暂存区判断，发送纸币压入钱箱指令
            // 一元、五元、十元、二十元、五十元、一百元
            "02030780008C33", "02030780029E10", "02030780031701",
            "0203078004A875", "02030780052164", "0203078006BA56" -> {
                d("暂存区验钞", tag = TAG)
                logRecordQueue.add("暂存区验钞")
                receiveCashProgress.set(0)
                executorService.execute {
                    SystemClock.sleep(5) // ACK指令和确认压栈指令有个微小间隔以确保钱箱能正确收到指令。
                    this.s("02030635ECE4")
                }
            }
            "02030617FCE6" -> {
                d("压栈状态", tag = TAG)
                logRecordQueue.add("压栈状态")
            }
            // 已收入纸币
            // 一元、五元、十元、二十元、五十元、一百元
            "0203078100542A", "02030781024609", "0203078103CF18",
            "0203078104706C", "0203078105F97D", "0203078106624F" -> {
                if (receiveCashProgress.addAndGet(1) == 1) {
                    receivedCashTotalMoney.addAndGet(
                        when (ret) {
                            "0203078100542A" -> {
                                d("已收入1元纸币", tag = TAG)
                                logRecordQueue.add("已收入1元纸币")
                                1
                            }
                            "02030781024609" -> {
                                d("已收入5元纸币", tag = TAG)
                                logRecordQueue.add("已收入5元纸币")
                                5
                            }
                            "0203078103CF18" -> {
                                d("已收入10元纸币", tag = TAG)
                                logRecordQueue.add("已收入10元纸币")
                                10
                            }
                            "0203078104706C" -> {
                                d("已收入20元纸币", tag = TAG)
                                logRecordQueue.add("已收入20元纸币")
                                20
                            }
                            "0203078105F97D" -> {
                                d("已收入50元纸币", tag = TAG)
                                logRecordQueue.add("已收入50元纸币")
                                50
                            }
                            "0203078106624F" -> {
                                d("已收入100元纸币", tag = TAG)
                                logRecordQueue.add("已收入100元纸币")
                                100
                            }
                            else -> 0
                        }
                    )
                }
            }
            "020306414FD1" -> {
                d("钱箱已满", tag = TAG)
                logRecordQueue.add("钱箱已满")
            }
            "02030642D4E3" -> {
                d("钱箱脱机状态", tag = TAG)
                logRecordQueue.add("钱箱脱机状态")
            }
            "02030644E286" -> {
                d("钱箱堵塞", tag = TAG)
                logRecordQueue.add("钱箱堵塞")
            }
            "020306435DF2" -> {
                d("入钞口堵塞", tag = TAG)
                logRecordQueue.add("入钞口堵塞")
            }
            "020306456B97" -> {
                d("有用户欺骗行为", tag = TAG)
                logRecordQueue.add("有用户欺骗行为")
            }
            "02030646F0A5" -> {
                d("暂停状态", tag = TAG)
                logRecordQueue.add("暂停状态")
            }
            "02030619820F" -> {
                d("钱箱正常", tag = TAG)
                logRecordQueue.add("钱箱正常")
            }
            "0203061A193D" -> {
                d("纸币器处于HOLDING状态", tag = TAG)
                logRecordQueue.add("纸币器处于HOLDING状态")
            }
            "0203061B902C" -> {
                d("忙碌状态", tag = TAG)
                logRecordQueue.add("忙碌状态")
            }
        }
        if (ret.startsWith("0203061C")) {
            d("纸币器返回了通用的拒绝码", tag = TAG)
            logRecordQueue.add("纸币器返回了通用的拒绝码")
        }
        if (ret.startsWith("02030647") || ret.startsWith("02030680") || ret.startsWith("02030681") || ret.startsWith("02030682")) {
            d("纸币器返回了通用的失败码", tag = TAG)
            logRecordQueue.add("纸币器返回了通用的失败码")
        }
        synchronized(receivedLock) { isReceivedCommand = true }
    }

    /**
     * 解析并保存钱箱的实时状态消息
     *
     * @param hex 钱箱的实时状态消息（原十六进制字符串）
     */
    private fun parseCashStateMessage(hex: String) {
        val message = when (hex) {
            "020306414FD1" -> "纸币器处于钱箱钱满状态"
            "02030642D4E3" -> "纸币器处于钱箱脱机状态"
            "02030644E286" -> "纸币器处于钱箱堵塞状态"
            "020306435DF2" -> "纸币器处于入钞口堵塞状态"
            "020306456B97" -> "纸币器处于有用户欺骗行为状态"
            "02030646F0A5" -> "纸币器处于暂停状态"
            "0203061A193D" -> "纸币器处于停止状态"
            "0203061B902C" -> "纸币器处于忙碌状态"
            else -> when {
                hex.startsWith("0203061C") -> "纸币器返回了拒绝通用码"
                hex.startsWith("02030647") -> "纸币器返回了失败通用码"
                hex.startsWith("02030680") ||
                        hex.startsWith("02030681") ||
                        hex.startsWith("02030682") -> ""
                else -> ""
            }
        }
//        d("Message=", message, tag = TAG)
        logRecordQueue.add(message)
    }

    // 指令队列
    private val commandQueue = ConcurrentLinkedDeque<String>()

    private fun send(command: String) {
        // 如果将要发送的是复位指令，应该立刻执行，并且自动放弃之前的指令队列。
        if (command == COM_RESET) commandQueue.clear()
        // 如果最后一个指令和将要被添加的指令不一样，则将新指令最佳到队列末尾。以保证将要执行的指令完整性。
        if (commandQueue.lastOrNull() != command) commandQueue.add(command)
        synchronized(receivedLock) {
            if (isReceivedCommand) {
                isReceivedCommand = false
                commandQueue.poll()?.let { s(it) } ?: kotlin.run { isReceivedCommand = true }
            }
        }
    }

    // 指令发送重试最大次数
    private val retryCount = AtomicInteger(3)
    private val sendLock = Any()

    /**
     * 向串口端写入将要执行的指令。
     * --
     * 指令执行失败时，会默认自动重试3次，若之后全部失败，则放弃该指令，所有状态恢复后，继续执行下一条指令。
     *
     * @param command 将要执行的指令字符串（十六进制）
     */
    private fun s(command: String) {
        // 注意内部递归，因此不能在这里单独开线程，而必须在外部调用时开启。
        synchronized(sendLock) {
            try {
                outputStream?.write(command.toHexBytes())
                outputStream?.flush()
                retryCount.set(3)
                if (command == COM_ACK) isReceivedCommand = true // 发送ACK指令，不用得到设备响应
                d("钱箱命令发送完成：", command, tag = TAG)
                logRecordQueue.add("钱箱命令发送完成：$command")
            } catch (e: Exception) {
                e(e, "钱箱命令发送失败", command, tag = TAG)
                logRecordQueue.add("钱箱命令发送失败：$command")
                if (retryCount.getAndDecrement() > 0) s(command) else {
                    isReceivedCommand = true
                    retryCount.set(3)
                }
            }
        }
    }
}