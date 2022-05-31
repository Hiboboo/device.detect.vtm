package com.jinkeen.vtm.detect.action

import androidx.databinding.ViewDataBinding
import com.jinkeen.base.action.BaseFragment
import com.jinkeen.base.device.data.AlignMode
import com.jinkeen.base.device.data.PrintMode
import com.jinkeen.base.device.data.PrintStyle
import com.jinkeen.base.device.printer.JinkeenPrinter
import com.jinkeen.base.util.d
import com.jinkeen.base.util.throttleFirst
import com.jinkeen.vtm.detect.R
import com.jinkeen.vtm.detect.databinding.FragmentPrinterTestLayoutBinding
import com.jinkeen.vtm.detect.printer.driver.UniwinM339Printer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class PrinterCheckFragment : BaseFragment(R.layout.fragment_printer_test_layout) {

    private val uniwinPinter = UniwinM339Printer()

    override fun setupViews(binding: ViewDataBinding?) {
        if (binding is FragmentPrinterTestLayoutBinding) {
            binding.printerConnect.throttleFirst {
                CoroutineScope(Dispatchers.IO).launch {
                    uniwinPinter.connect(mContext)
                    val state = uniwinPinter.getStatus()
                    withContext(Dispatchers.Main) {
                        binding.printerConnectRet.text = if (state == JinkeenPrinter.STATE_OK) "连接成功" else "连接失败"
                    }
                }
            }
            binding.printerState.throttleFirst {
                binding.printerStateRet.text = when (uniwinPinter.getStatus()) {
                    JinkeenPrinter.STATE_OK -> "正常"
                    JinkeenPrinter.STATE_UNUNITED -> "未连接/脱机"
                    JinkeenPrinter.STATE_OPEN_LID -> "开盖"
                    JinkeenPrinter.STATE_LACK -> "缺纸"
                    JinkeenPrinter.STATE_BE_LACK -> "即将缺纸"
                    JinkeenPrinter.STATE_OVERHEAT -> "过热"
                    else -> ""
                }
            }
            binding.printerCheck.throttleFirst {
                CoroutineScope(Dispatchers.IO).launch {
                    uniwinPinter.print(LinkedList<PrintStyle>().apply {
                        add(PrintStyle(PrintMode.ALIGN_MODE, AlignMode.CENTER)) // 中间对齐
                        add(PrintStyle(PrintMode.TEXT, "郑州小票打印股份有限公司"))
                        add(PrintStyle(PrintMode.TEXT, "\n"))
                        add(PrintStyle(PrintMode.TEXT, "服务签购单"))
                        add(PrintStyle(PrintMode.TEXT, "\n"))
                        add(PrintStyle(PrintMode.PIXEL_WRAP, 50)) // 按50px高度走纸

                        add(PrintStyle(PrintMode.ALIGN_MODE, AlignMode.LEFT)) // 居左对齐
                        add(PrintStyle(PrintMode.TEXT, "用户姓名：张三"))
                        add(PrintStyle(PrintMode.TEXT, "\n"))
                        add(PrintStyle(PrintMode.PIXEL_WRAP, 8))

                        add(PrintStyle(PrintMode.TEXT, "用户编号：3300881199"))
                        add(PrintStyle(PrintMode.TEXT, "\n"))
                        add(PrintStyle(PrintMode.PIXEL_WRAP, 8))

                        add(PrintStyle(PrintMode.TEXT, "圈存金额：299.88元"))
                        add(PrintStyle(PrintMode.TEXT, "\n"))
                        add(PrintStyle(PrintMode.PIXEL_WRAP, 8))

                        add(PrintStyle(PrintMode.TEXT, "终端编号：2021060200002"))
                        add(PrintStyle(PrintMode.TEXT, "\n"))
                        add(PrintStyle(PrintMode.PIXEL_WRAP, 8))

                        add(PrintStyle(PrintMode.TEXT, "订单编号：515911928938233856}"))
                        add(PrintStyle(PrintMode.TEXT, "\n"))
                        add(PrintStyle(PrintMode.PIXEL_WRAP, 8))

                        add(PrintStyle(PrintMode.TEXT, "交易日期：2022年06月01日 12:02:38"))
                        add(PrintStyle(PrintMode.TEXT, "\n"))
                        add(PrintStyle(PrintMode.PIXEL_WRAP, 35))

                        add(PrintStyle(PrintMode.ALIGN_MODE, AlignMode.CENTER))
                        add(PrintStyle(PrintMode.TEXT, "扫码获取更多服务"))
                        add(PrintStyle(PrintMode.TEXT, "\n"))
                        add(PrintStyle(PrintMode.PIXEL_WRAP, 20))

                        add(PrintStyle(PrintMode.ALIGN_MODE, AlignMode.LEFT))
                        add(PrintStyle(PrintMode.QR_CODE, "http://weixin.qq.com/r/1XVaQkrEGRdmrQ4h9yDH"))
                        add(PrintStyle(PrintMode.PIXEL_WRAP, 20))

                        add(PrintStyle(PrintMode.ALIGN_MODE, AlignMode.CENTER))
                        add(PrintStyle(PrintMode.TEXT, "客服电话：4006636773"))
                        add(PrintStyle(PrintMode.TEXT, "\n"))
                        add(PrintStyle(PrintMode.TEXT, "本服务由金擎科技提供"))
                        add(PrintStyle(PrintMode.TEXT, "\n"))
                        add(PrintStyle(PrintMode.PIXEL_WRAP, 150))

                        add(PrintStyle(PrintMode.TEXT, ""))
                        add(PrintStyle(PrintMode.CUT_PAPER, ""))
                    }) { code -> d("打印结果 Code=", code)}
                }
            }
        }
    }
}