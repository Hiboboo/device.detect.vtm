package com.jinkeen.vtm.detect.action

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Rect
import android.os.*
import android.text.TextUtils
import android.view.View
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseDataBindingHolder
import com.jinkeen.base.action.BaseFragment
import com.jinkeen.base.util.dpToPx
import com.jinkeen.base.util.throttleFirst
import com.jinkeen.vtm.detect.R
import com.jinkeen.vtm.detect.databinding.FragmentCashboxCheckItemBinding
import com.jinkeen.vtm.detect.databinding.FragmentCashboxCheckLayoutBinding
import com.jinkeen.vtm.detect.serialport.CashSerialPortService
import java.lang.ref.SoftReference
import java.util.*
import kotlin.concurrent.fixedRateTimer

class CashboxCheckFragment : BaseFragment(R.layout.fragment_cashbox_check_layout) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext.bindService(
            Intent(mContext, CashSerialPortService::class.java),
            cashboxConnect,
            Context.BIND_AUTO_CREATE
        )
    }

    private lateinit var layoutBinding: FragmentCashboxCheckLayoutBinding

    private lateinit var adapter: CashboxMessageAdapter

    override fun setupViews(binding: ViewDataBinding?) {
        if (binding is FragmentCashboxCheckLayoutBinding) {
            layoutBinding = binding
            binding.cashboxReset.throttleFirst { mContext.sendBroadcast(Intent(CashSerialPortService.ACTION_RESET)) }
            binding.cashboxForceReset.throttleFirst { mContext.sendBroadcast(Intent(CashSerialPortService.ACTION_FORCE_RESET)) }
            binding.startCashin.throttleFirst { mContext.sendBroadcast(Intent(CashSerialPortService.ACTION_START_CASHIN)) }
            binding.finishCashin.throttleFirst { mContext.sendBroadcast(Intent(CashSerialPortService.ACTION_RESET)) }
            binding.cashboxMessageContainer.layoutManager = LinearLayoutManager(mContext, RecyclerView.VERTICAL, false)
            binding.cashboxMessageContainer.addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                    val position = parent.getChildAdapterPosition(view)
                    if (position > 0) outRect.top = 1.0f.dpToPx().toInt()
                }
            })
            binding.cashboxMessageContainer.itemAnimator = null
            adapter = CashboxMessageAdapter()
            binding.cashboxMessageContainer.adapter = adapter
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mContext.unbindService(cashboxConnect)
    }

    private fun setCashboxMessage(meesage: String) {
        if (TextUtils.isEmpty(meesage)) return
        adapter.addData(meesage)
        if (!layoutBinding.cashboxMessageContainer.canScrollVertically(1)) layoutBinding.cashboxMessageContainer.scrollToPosition(adapter.itemCount - 1)
    }

    private companion object {

        class CashReceiveHandler(fragment: CashboxCheckFragment) : Handler(Looper.getMainLooper()) {

            private val reference = SoftReference(fragment)

            override fun handleMessage(msg: Message) {
                if (msg.what == 200) reference.get()?.setCashboxMessage(msg.obj.toString())
            }
        }
    }

    private val cashHandler = CashReceiveHandler(this)

    private val cashboxConnect = object : ServiceConnection {

        private var timer: Timer? = null

        override fun onServiceConnected(name: ComponentName?, iBinder: IBinder?) {
            timer = fixedRateTimer("update-message", false, 0, 100) {
                (iBinder as? CashSerialPortService.CashSerialBinder)?.getService()?.getCashLastMessage()?.let {
                    cashHandler.sendMessage(cashHandler.obtainMessage(200, it))
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            timer?.cancel()
        }
    }

    private inner class CashboxMessageAdapter : BaseQuickAdapter<String, BaseDataBindingHolder<FragmentCashboxCheckItemBinding>>(R.layout.fragment_cashbox_check_item) {

        override fun convert(holder: BaseDataBindingHolder<FragmentCashboxCheckItemBinding>, item: String) {
            holder.dataBinding?.cashboxMessage?.text = item
        }
    }
}