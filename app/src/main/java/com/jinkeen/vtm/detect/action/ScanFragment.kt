package com.jinkeen.vtm.detect.action

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.databinding.ViewDataBinding
import com.jinkeen.base.action.BaseFragment
import com.jinkeen.vtm.detect.R
import com.jinkeen.vtm.detect.databinding.FragmentScanLayoutBinding
import com.jinkeen.vtm.detect.scan.UniwinM339Scanner

class ScanFragment : BaseFragment(R.layout.fragment_scan_layout) {

    private val scanner = UniwinM339Scanner()
    private lateinit var layoutBinding: FragmentScanLayoutBinding

    override fun setupViews(binding: ViewDataBinding?) {
        if (binding is FragmentScanLayoutBinding) {
            layoutBinding = binding
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        scanner.executeScan {
            if (flag == 1) {
                flag = 0
                layoutBinding.scanRet.text = String.format("二维码数据：%s", it)
            }
        }
    }

    private var flag = 1

    fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (flag == 1) {
            scanner.analysisKeyEvent(event)
            return true
        }
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        scanner.stopScan()
    }
}