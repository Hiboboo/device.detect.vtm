package com.jinkeen.vtm.detect.action

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.listener.OnItemClickListener
import com.chad.library.adapter.base.viewholder.BaseDataBindingHolder
import com.jinkeen.base.action.BaseAppCompatActivity
import com.jinkeen.base.util.dpToPx
import com.jinkeen.vtm.detect.R
import com.jinkeen.vtm.detect.databinding.ActivityHomeItemBinding
import com.jinkeen.vtm.detect.databinding.ActivityMainBinding

class MainActivity : BaseAppCompatActivity() {

    override fun getScreenWidth(): Float = 1920.0f

    @SuppressLint("MissingSuperCall")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState, R.layout.activity_main)
    }

    override fun setupThemeStyle(s: ThemeStyle) {
        s.isNeedBaseActionbar = false
    }

    override fun initCustomToolbar(): Toolbar? {
        super.initCustomToolbar()
        return getLayoutBinding<ActivityMainBinding>()?.included?.toolbar
    }

    private lateinit var adapter: ItemAdapter

    override fun setupViews(binding: ViewDataBinding?) {
        if (binding is ActivityMainBinding) {
            binding.recyclerview.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
            binding.recyclerview.addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                    val position = parent.getChildAdapterPosition(view)
                    if (position > 0) outRect.top = 1.0f.dpToPx().toInt()
                }
            })
            adapter = ItemAdapter().apply { setOnItemClickListener(itemClickListener) }
            binding.recyclerview.adapter = adapter
        }
    }

    override fun onResume() {
        super.onResume()
        adapter.setNewInstance(mutableListOf<ItemData>().apply {
            add(ItemData(1, "端口连接检测", true))
            add(ItemData(2, "钱箱检测", false))
            add(ItemData(3, "灯光检测", false))
            add(ItemData(4, "扫码测试", false))
            add(ItemData(5, "小票打印机", false))
        })
        this.switchChildPage(1)
    }

    private val itemClickListener = OnItemClickListener { _, _, position ->
        adapter.selectedPosition(position)
        this.switchChildPage(adapter.getItem(position).code)
    }

    private fun switchChildPage(code: Int) {
        when (code) {
            1 -> this.replaceFragment(PortConnectTestFragment::class.java)
            2 -> this.replaceFragment(CashboxCheckFragment::class.java)
            3 -> this.replaceFragment(LightTestFragment::class.java)
            4 -> this.replaceFragment(ScanFragment::class.java)
            5 -> this.replaceFragment(PrinterCheckFragment::class.java)
        }
    }

    private var fragment: Fragment? = null

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return (fragment as? ScanFragment)?.dispatchKeyEvent(event) ?: super.dispatchKeyEvent(event)
    }

    private fun replaceFragment(cls: Class<*>, data: Bundle? = null) {
        fragment = supportFragmentManager.fragmentFactory.instantiate(classLoader, cls.name)
        fragment?.arguments = data
        supportFragmentManager.beginTransaction().replace(R.id.container, fragment!!, cls.simpleName).commit()
    }

    inner class ItemAdapter : BaseQuickAdapter<ItemData, BaseDataBindingHolder<ActivityHomeItemBinding>>(R.layout.activity_home_item) {

        override fun convert(holder: BaseDataBindingHolder<ActivityHomeItemBinding>, item: ItemData) {
            holder.dataBinding?.name?.text = item.name
            holder.dataBinding?.rItemView?.setBackgroundColor(if (item.isChecked) Color.WHITE else Color.TRANSPARENT)
        }

        fun selectedPosition(position: Int) {
            data.forEachIndexed { index, appInfoEntity -> appInfoEntity.isChecked = (index == position) }
            notifyItemRangeChanged(0, itemCount)
        }
    }

    data class ItemData(
        val code: Int,
        val name: String,
        var isChecked: Boolean
    )
}