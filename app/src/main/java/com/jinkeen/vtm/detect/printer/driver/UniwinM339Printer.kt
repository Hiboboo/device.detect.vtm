package com.jinkeen.vtm.detect.printer.driver

import android.content.Context
import android.hardware.usb.UsbManager
import com.jinkeen.base.device.SingleInstance
import com.jinkeen.base.device.data.AlignMode
import com.jinkeen.base.device.data.PrintMode
import com.jinkeen.base.device.data.PrintStyle
import com.jinkeen.base.device.printer.JinkeenPrinter
import com.jinkeen.base.util.d
import com.jinkeen.base.util.e
import com.jinkeen.vtm.detect.printer.uniwin.PrintCmd
import com.jinkeen.vtm.detect.printer.uniwin.UsbDriver
import com.jinkeen.vtm.detect.printer.uniwin.UsbUtil
import java.util.*

class UniwinM339Printer private constructor() : JinkeenPrinter {

    companion object {

        /** 打印机非正常状态 */
        const val ERR_PRINTER_UNKNOWN = 730001

        private const val TAG = "UniwinM339Printer"

        private val instance: UniwinM339Printer by lazy { UniwinM339Printer() }

        @SingleInstance
        operator fun invoke(): UniwinM339Printer = instance
    }

    private var usbDriver: UsbDriver? = null

    override fun connect(context: Context) {
        usbDriver = UsbDriver(context.getSystemService(Context.USB_SERVICE) as UsbManager)
        val driverCheck = UsbUtil.usbDriverCheck(context, usbDriver)
        if (driverCheck == 0) d("USB Driver 打印机已连接", tag = TAG) else d("USB Driver 打印机连接失败", tag = TAG)
    }

    override fun disconnect(context: Context) {
        usbDriver?.write(PrintCmd.SetClean())
        usbDriver?.closeUsbDevice()
        d("打印机已断开", tag = TAG)
    }

    override fun getStatus(): Int =
        if (usbDriver?.isConnected == true) JinkeenPrinter.STATE_OK else JinkeenPrinter.STATE_UNUNITED

    override suspend fun print(styles: LinkedList<PrintStyle>, block: suspend (code: Int) -> Unit) {
        d("准备开始打印, UsbDriver=", usbDriver ?: "null", tag = TAG)
        usbDriver?.let {
            val code = try {
                //设置进入汉字模式
                it.write(PrintCmd.SetReadZKmode(0))
                styles.forEach { style ->
                    when (style.mode) {
                        PrintMode.ALIGN_MODE -> when (style.value) {
                            AlignMode.LEFT -> it.write(PrintCmd.SetAlignment(0))
                            AlignMode.CENTER -> it.write(PrintCmd.SetAlignment(1))
                            AlignMode.RIGHT -> it.write(PrintCmd.SetAlignment(2))
                        }
                        PrintMode.TEXT -> it.write(PrintCmd.PrintString(style.value.toString(), 1))
                        PrintMode.LINE_WRAP -> it.write(PrintCmd.PrintFeedline(style.value.toString().toIntOrNull() ?: 0))
                        PrintMode.PIXEL_WRAP -> it.write(PrintCmd.PrintFeedDot(style.value.toString().toIntOrNull() ?: 0))
                        PrintMode.QR_CODE -> it.write(PrintCmd.PrintQrcode(style.value.toString(), 25, 6, 0))
                        PrintMode.CUT_PAPER -> it.write(PrintCmd.PrintCutpaper(0))
                        else -> {}
                    }
                }
                d("打印完成", tag = TAG)
                0
            } catch (e: java.lang.Exception) {
                e(e, "打印出错", tag = TAG)
                ERR_PRINTER_UNKNOWN
            }
            block(code)
        } ?: block(ERR_PRINTER_UNKNOWN)
    }
}