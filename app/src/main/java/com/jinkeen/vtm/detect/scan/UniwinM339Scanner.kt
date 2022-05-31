package com.jinkeen.vtm.detect.scan

import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import com.jinkeen.base.util.d
import java.util.concurrent.atomic.AtomicBoolean

class UniwinM339Scanner {

    companion object {

        private const val TAG = "UniwinM339Scanner"
    }

    private val isPressEnter = AtomicBoolean(false)
    private val mStringBuilderResult = StringBuilder()
    private var mCaps = false
    private val mHandler = Handler(Looper.getMainLooper())
    private val mScanningFinishedRunnable = Runnable {
        val ret = mStringBuilderResult.toString()
        d("原始二维码数据：", ret, tag = TAG)
        block(ret)
        mStringBuilderResult.setLength(0)
        isPressEnter.set(false)
    }

    private var block: (data: String) -> Unit = {}

    fun executeScan(block: (data: String) -> Unit) {
        this.block = block
    }

    fun stopScan() {
        mHandler.removeCallbacks(mScanningFinishedRunnable)
    }

    fun analysisKeyEvent(event: KeyEvent) {
        if (isPressEnter.get()) return
        this.checkLetterStatus(event)
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (event.keyCode == KeyEvent.KEYCODE_ENTER) {
                isPressEnter.set(true)
                mHandler.removeCallbacks(mScanningFinishedRunnable)
                mHandler.post(mScanningFinishedRunnable)
            } else {
                val aChar = this.getInputCode(event)
                if (aChar.toString().trim().isNotEmpty()) mStringBuilderResult.append(aChar)
            }
        }
    }

    private fun checkLetterStatus(event: KeyEvent) {
        if (event.keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || event.keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT) {
            mCaps = (event.action == KeyEvent.ACTION_DOWN)
        }
    }

    private fun getInputCode(event: KeyEvent): Char {
        val aChar: Char
        val keyCode: Int = event.keyCode
        d("模拟按下的按键：", keyCode, tag = TAG)
        aChar = if (keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z) //29< keycode <54
        {
            //字母
            (if (mCaps) 'A' else 'a') + keyCode - KeyEvent.KEYCODE_A //
        } else if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
            //数字
            if (mCaps) //是否按住了shift键
            {
                //按住了 需要将数字转换为对应的字符
                when (keyCode) {
                    KeyEvent.KEYCODE_0 -> ')'
                    KeyEvent.KEYCODE_1 -> '!'
                    KeyEvent.KEYCODE_2 -> '@'
                    KeyEvent.KEYCODE_3 -> '#'
                    KeyEvent.KEYCODE_4 -> '$'
                    KeyEvent.KEYCODE_5 -> '%'
                    KeyEvent.KEYCODE_6 -> '^'
                    KeyEvent.KEYCODE_7 -> '&'
                    KeyEvent.KEYCODE_8 -> '*'
                    KeyEvent.KEYCODE_9 -> '('
                    else -> ' '
                }
            } else '0' + keyCode - KeyEvent.KEYCODE_0
        } else {
            //其他符号
            when (keyCode) {
                KeyEvent.KEYCODE_PERIOD -> '.'
                KeyEvent.KEYCODE_MINUS -> if (mCaps) '_' else '-'
                KeyEvent.KEYCODE_SLASH -> '/'
                KeyEvent.KEYCODE_STAR -> '*'
                KeyEvent.KEYCODE_POUND -> '#'
                KeyEvent.KEYCODE_SEMICOLON -> if (mCaps) ':' else ';'
                KeyEvent.KEYCODE_AT -> '@'
                KeyEvent.KEYCODE_BACKSLASH -> if (mCaps) '|' else '\\'
                else -> ' '
            }
        }
        return aChar
    }
}