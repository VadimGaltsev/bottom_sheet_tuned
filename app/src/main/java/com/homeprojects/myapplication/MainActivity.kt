package com.homeprojects.myapplication

import android.Manifest
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.TypedArray
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.BottomSheetDialogFragment
import android.support.design.widget.CoordinatorLayout
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet
import android.view.*
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

class CustomBehavior
@JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : CoordinatorLayout.Behavior<View>(context, attrs) {

    override fun onDependentViewChanged(parent: CoordinatorLayout, child: View, dependency: View): Boolean {
        val bgView = parent.findViewById<View>(R.id.touch_outside)
//        val multip = (parent.height - dependency.bottom).toFloat() / dependency.height
//        val alpha = child.height.toFloat() / dependency.top + multip
        val multip = (parent.height - parent.getChildAt(1).bottom).toFloat() / parent.getChildAt(1).height
        val alpha = dependency.height.toFloat() / parent.getChildAt(1).top + multip
        println(multip)
//        child.alpha = alpha
        dependency.alpha = alpha
        bgView.alpha = alpha
        return true
    }
}

class DialogTest : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val parent = inflater.inflate(R.layout.bottom_sheet_scroll, container, false) as ViewGroup
        val view = inflater.inflate(R.layout.test, parent, false)
        view.setBackgroundDrawable(ContextCompat.getDrawable(context!!, R.drawable.bg_bottom_dialog_rounded))
        parent.findViewById<ViewGroup>(R.id.host_container).addView(view)
        return parent
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val manager = context?.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val windowHeifght = manager.defaultDisplay.height
        BottomSheetBehavior.from(view?.parent as View).peekHeight = windowHeifght / 2
        val view = view?.parent as ViewGroup
        view.background = null
        val toolbar = LayoutInflater.from(context).inflate(R.layout.toolbar, view.parent as ViewGroup, false)
//        val lp = toolbar.layoutParams as CoordinatorLayout.LayoutParams
        val bgView = (view.parent as ViewGroup).findViewById<View>(R.id.touch_outside)
        bgView.setBackgroundColor(ContextCompat.getColor(context!!, R.color.white))
        bgView.alpha = 0f
        val lp = bgView.layoutParams as CoordinatorLayout.LayoutParams
        lp.behavior = CustomBehavior(context!!)
        lp.anchorId = R.id.toolbar
//        lp.anchorId = R.id.design_bottom_sheet
        lp.gravity = Gravity.TOP
//        view.layoutParams.height = windowHeifght - toolbar.layoutParams.height - getStatusBar()
        (this.view as ViewGroup).addView(toolbar, 0)
//        view.addView(toolbar)
        toolbar.findViewById<View>(R.id.end).setOnClickListener {
            dialog.cancel()
        }
    }
}

class MainActivity : AppCompatActivity() {

    object Rec : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
//            this.setResultExtras(Bundle().apply { putInt("ok", 1) })
            intent?.putExtra("ok", 1)
            resultCode = 10
        }
    }

    object Rec2 : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            println(intent?.getIntExtra("ok", -1))
            println(resultCode)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        registerReceiver(Rec, IntentFilter("OK").apply { priority = 1 }, Manifest.permission.READ_SMS, null)
        registerReceiver(Rec2, IntentFilter("OK").apply { priority = -100 })


        val dialog = DialogTest()

//        val disposable = fab.onClickListener(1000L) { view ->
        dialog.show(supportFragmentManager, "")
////        }
//        print("")
    }

    override fun onResume() {
        super.onResume()
        sendOrderedBroadcast(Intent("OK").apply { putExtra("ok", 0) }, null)
    }

    fun View.onClickListener(timeoutInMillis: Long = 250L, onClick: (View) -> Unit): Disposable {
        return onClicks()
            .throttleFirst(timeoutInMillis, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(onClick)
    }

    fun View.onClicks(): Observable<View> {
        var emitterRef: ObservableEmitter<View>? = null
        val observable = Observable.create<View> { emitter -> emitterRef = emitter }

        observable.doOnDispose { setOnClickListener(null) }
        setOnClickListener {
            emitterRef?.onNext(it)
        }
        return observable
    }
}
