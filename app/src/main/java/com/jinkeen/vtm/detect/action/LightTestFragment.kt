package com.jinkeen.vtm.detect.action

import androidx.databinding.ViewDataBinding
import com.jinkeen.base.action.BaseFragment
import com.jinkeen.base.util.throttleFirst
import com.jinkeen.vtm.detect.R
import com.jinkeen.vtm.detect.databinding.FragmentLightTestLayoutBinding
import com.jinkeen.vtm.detect.serialport.LightBusiness

class LightTestFragment : BaseFragment(R.layout.fragment_light_test_layout) {

    private val lightBusiness = LightBusiness()
    private var isOpenedLight = false

    override fun setupViews(binding: ViewDataBinding?) {
        if (binding is FragmentLightTestLayoutBinding) {
            binding.isOpenPort = false
            binding.openPort.throttleFirst {
                lightBusiness.openLightPort()
                binding.isOpenPort = true
            }
            binding.closePort.throttleFirst {
                lightBusiness.closeLightPort()
                binding.isOpenPort = false
            }
            binding.openTicket.throttleFirst { LightBusiness.openTicket() }
            binding.closeTicket.throttleFirst { LightBusiness.closeTicket() }
            binding.openCash.throttleFirst { LightBusiness.openCashbox() }
            binding.closeCash.throttleFirst { LightBusiness.closeCashbox() }
            binding.openIc.throttleFirst { LightBusiness.openIcReader() }
            binding.closeIc.throttleFirst { LightBusiness.closeIcReader() }
            binding.openIdcard.throttleFirst { LightBusiness.openIdentityReader() }
            binding.closeIdcard.throttleFirst { LightBusiness.closeIdentityReader() }
            binding.openPrinter.throttleFirst { LightBusiness.openPrinter() }
            binding.closePrinter.throttleFirst { LightBusiness.closePrinter() }
            binding.openUnion.throttleFirst { LightBusiness.openUnionReader() }
            binding.closeUnion.throttleFirst { LightBusiness.closeUnionReader() }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 因为是测试，所以在页面关掉后，自动恢复到测试前状态
        if (isOpenedLight) return
        lightBusiness.closeLightPort()
    }
}