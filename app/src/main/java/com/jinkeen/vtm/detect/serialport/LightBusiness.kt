package com.jinkeen.vtm.detect.serialport

import com.jinkeen.base.util.SharedPreferenceHelper
import com.jinkeen.base.util.d
import com.jinkeen.vtm.detect.BuildConfig

class LightBusiness {

    companion object {
        private val instance: LightBusiness by lazy { LightBusiness() }

        operator fun invoke(): LightBusiness = instance

        fun openTicket() {
            val c = SharedPreferenceHelper.getString(LightExtra.KEY_TICKET, Command.CONST_TICKET).split(",")[0]
            d("打开小票打印机灯光=", c)
            sendCommand(c)
        }

        fun closeTicket() {
            val c = SharedPreferenceHelper.getString(LightExtra.KEY_TICKET, Command.CONST_TICKET).split(",")[1]
            d("关闭小票打印机灯光=", c)
            sendCommand(c)
        }

        fun openIcReader() {
            val c = SharedPreferenceHelper.getString(LightExtra.KEY_IC, Command.CONST_IC).split(",")[0]
            d("打开IC读卡器灯光=", c)
            sendCommand(c)
        }

        fun closeIcReader() {
            val c = SharedPreferenceHelper.getString(LightExtra.KEY_IC, Command.CONST_IC).split(",")[1]
            d("关闭IC读卡器灯光=", c)
            sendCommand(c)
        }

        fun openUnionReader() {
            val c = SharedPreferenceHelper.getString(LightExtra.KEY_BANKCARD, Command.CONST_UNION).split(",")[0]
            d("打开银联卡读卡器灯光=", c)
            sendCommand(c)
        }

        fun closeUnionReader() {
            val c = SharedPreferenceHelper.getString(LightExtra.KEY_BANKCARD, Command.CONST_UNION).split(",")[1]
            d("关闭银联卡读卡器灯光=", c)
            sendCommand(c)
        }

        fun openCashbox() {
            val c = SharedPreferenceHelper.getString(LightExtra.KEY_CASHBOX, Command.CONST_CASHBOX).split(",")[0]
            d("打开钱箱灯光=", c)
            sendCommand(c)
        }

        fun closeCashbox() {
            val c = SharedPreferenceHelper.getString(LightExtra.KEY_CASHBOX, Command.CONST_CASHBOX).split(",")[1]
            d("关闭钱箱灯光=", c)
            sendCommand(c)
        }

        fun openIdentityReader() {
            val c = SharedPreferenceHelper.getString(LightExtra.KEY_IDENTITY, Command.CONST_IDCARD).split(",")[0]
            d("打开身份证识别灯光=", c)
            sendCommand(c)
        }

        fun closeIdentityReader() {
            val c = SharedPreferenceHelper.getString(LightExtra.KEY_IDENTITY, Command.CONST_IDCARD).split(",")[1]
            d("关闭身份证识别灯光=", c)
            sendCommand(c)
        }

        fun openPrinter() {
            val c = SharedPreferenceHelper.getString(LightExtra.KEY_PRINTER, Command.CONST_PRINT).split(",")[0]
            d("打开发票打印机灯光=", c)
            sendCommand(c)
        }

        fun closePrinter() {
            val c = SharedPreferenceHelper.getString(LightExtra.KEY_PRINTER, Command.CONST_PRINT).split(",")[1]
            d("关闭发票打印机灯光=", c)
            sendCommand(c)
        }

        private fun sendCommand(command: String) {
            LightSerialPortManager().sendCommand(command)
        }
    }

    object LightExtra {
        const val KEY_IC = "${BuildConfig.APPLICATION_ID}.ic"
        const val KEY_BANKCARD = "${BuildConfig.APPLICATION_ID}.bankcard"
        const val KEY_CASHBOX = "${BuildConfig.APPLICATION_ID}.cashbox"
        const val KEY_IDENTITY = "${BuildConfig.APPLICATION_ID}.identity"
        const val KEY_TICKET = "${BuildConfig.APPLICATION_ID}.ticket"
        const val KEY_PRINTER = "${BuildConfig.APPLICATION_ID}.printer"
    }

    object Command {

        const val CONST_TICKET = "AA0106020155,AA0106020055"
        const val CONST_IC = "AA0106010155,AA0106010055"
        const val CONST_UNION = "AA0106030155,AA0106030055"
        const val CONST_CASHBOX = "AA0106040155,AA0106040055"
        const val CONST_IDCARD = "AA0106060155,AA0106060055"
        const val CONST_PRINT = "AA0106050155,AA0106050055"

        internal const val LIGHT_PATH = "/dev/ttyS7"
        internal const val BAUDRATE = "115200"
    }

    fun openLightPort() {
        LightSerialPortManager().open(Command.LIGHT_PATH, Command.BAUDRATE)
    }

    fun closeLightPort() {
        LightSerialPortManager().close()
    }
}