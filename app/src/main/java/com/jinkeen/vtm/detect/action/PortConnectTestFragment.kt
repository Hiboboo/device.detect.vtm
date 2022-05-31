package com.jinkeen.vtm.detect.action

import android.view.View
import android_serialport_api.SerialPort
import androidx.databinding.ViewDataBinding
import com.example.ztdemo.ZtPinpad
import com.hc.reader.AndroidSerialPort
import com.jinkeen.base.action.BaseFragment
import com.jinkeen.base.util.d
import com.jinkeen.base.util.e
import com.jinkeen.base.util.throttleFirst
import com.jinkeen.vtm.detect.R
import com.jinkeen.vtm.detect.databinding.FragmentPortConnectLayoutBinding
import com.zmsoft.hntermail.DCCardReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PortConnectTestFragment : BaseFragment(R.layout.fragment_port_connect_layout) {

    private lateinit var layoutBinding: FragmentPortConnectLayoutBinding

    override fun setupViews(binding: ViewDataBinding?) {
        if (binding is FragmentPortConnectLayoutBinding) {
            layoutBinding = binding
            binding.icEnabled = true
            binding.icReader.throttleFirst { this.checkIcreaderConnect() }
            binding.bankEnabled = true
            binding.bankReader.throttleFirst { this.checkBankreaderConnect() }
            binding.bankKeyboardEnabled = true
            binding.bankKeyboard.throttleFirst { this.checkBankcardPasswordConnect() }
            binding.cashboxEnabled = true
            binding.cashbox.throttleFirst { this.checkCashboxConnect() }
            binding.lightEnabled = true
            binding.lightControl.throttleFirst { this.checkLightControlConnect() }
        }
    }

    private fun checkIcreaderConnect() {
        layoutBinding.icEnabled = false
        CoroutineScope(Dispatchers.IO).launch {
            val reader = AndroidSerialPort(mContext)
            val connectRet = try {
                val code = reader.OpenReader("/dev/ttyS3", 115200)
                if (code.toInt() >= 0) {
                    d("ZD115读卡器端口打开成功")
                    true
                } else {
                    d("ZD115读卡器端口打开失败")
                    false
                }
            } catch (e: Exception) {
                e(e, "ZD115读卡器端口打开异常")
                false
            }
            var version = ""
            var snr = ""
            if (connectRet) {
                version = try {
                    val buffer = ByteArray(32)
                    val code = reader.srd_ver(buffer)
                    if (code >= 0) String(buffer).replace("", "") else reader.GetErrMessage(0, code)
                } catch (e: Exception) {
                    e.printStackTrace()
                    "空"
                }
                snr = try {
                    val buffer = ByteArray(24)
                    val code = reader.srd_ver(buffer)
                    if (code >= 0) String(buffer, 0, 16).replace(" ", "") else reader.GetErrMessage(0, code)
                } catch (e: Exception) {
                    e.printStackTrace()
                    "空"
                }
            }
            withContext(Dispatchers.Main) {
                layoutBinding.icEnabled = true
                layoutBinding.icRet = connectRet
                layoutBinding.icInfo = String.format("硬件版本号：%s\n产品序列号：%s", version, snr)
            }
        }
    }

    private fun checkBankreaderConnect() {
        layoutBinding.bankEnabled = false
        CoroutineScope(Dispatchers.IO).launch {
            val ret = try {
                DCCardReader.checkCard("4", 10, -1)
            } catch (e: Exception) {
                null
            }
            withContext(Dispatchers.Main) {
                layoutBinding.bankEnabled = true
                layoutBinding.bankRetText = ret?.get("failreson") ?: "连接出现异常"
                layoutBinding.bankRet = (ret?.get("status") ?: "-1") != "-1"
            }
        }
    }

    private fun checkBankcardPasswordConnect() {
        layoutBinding.bankKeyboardEnabled = false
        CoroutineScope(Dispatchers.IO).launch {
            val retText = try {
                val ztPinpad = ZtPinpad()
                val ret = ztPinpad.Open("/dev/ttyS5:9600,N,8,1")
                if (ret != 0) "密码键盘初始化失败[1]" else {
                    val soundType = 1
                    val maxlen = 6
                    val minlen = 0
                    val timeout = 30
                    val inputRet = ztPinpad.StartPinInput(soundType, maxlen, minlen, true, timeout)
                    if (inputRet != 0) "密码键盘初始化失败[2]" else ""
                }
            } catch (e: Exception) {
                null
            }
            withContext(Dispatchers.Main) {
                layoutBinding.bankKeyboardEnabled = true
                layoutBinding.bankKeyboardRet = null != retText
                layoutBinding.bankKeyboardRetText = retText ?: "密码键盘没有签到"
            }
        }
    }

    private fun checkCashboxConnect() {
        layoutBinding.cashboxEnabled = false
        CoroutineScope(Dispatchers.IO).launch {
            val openState = try {
                SerialPort(File("/dev/ttyS6"), 9600, 0)
                d("钱箱串口已打开")
                true
            } catch (e: Exception) {
                e(e, "钱箱串口打开异常")
                false
            }
            withContext(Dispatchers.Main) {
                layoutBinding.cashboxRetPanel.visibility = View.VISIBLE
                layoutBinding.cashboxEnabled = true
                layoutBinding.cashboxRet = openState
            }
        }
    }

    private fun checkLightControlConnect() {
        layoutBinding.lightEnabled = false
        CoroutineScope(Dispatchers.IO).launch {
            val openState = try {
                SerialPort(File("/dev/ttyS7"), 115200, 0)
                d("灯光串口已打开")
                true
            } catch (e: Exception) {
                e(e, "灯光串口打开异常")
                false
            }
            withContext(Dispatchers.Main) {
                layoutBinding.lightControlPanel.visibility = View.VISIBLE
                layoutBinding.lightEnabled = true
                layoutBinding.lightRet = openState
            }
        }
    }
}