package com.handydev.financier.widget

import android.app.Dialog
import android.app.DialogFragment
import android.content.DialogInterface
import android.os.Bundle
import android.os.Vibrator
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.handydev.financier.R
import com.handydev.financier.utils.MyPreferences
import com.handydev.financier.utils.StringUtil
import com.handydev.financier.utils.Utils
import org.androidannotations.annotations.*
import java.math.BigDecimal
import java.util.*

@EFragment(R.layout.calculator)
open class CalculatorInput : DialogFragment() {
    @JvmField
    @ViewById(R.id.result)
    protected var tvResult: TextView? = null

    @JvmField
    @ViewById(R.id.op)
    protected var tvOp: TextView? = null

    @JvmField
    @ViewsById(R.id.b0, R.id.b1, R.id.b2, R.id.b3, R.id.b4, R.id.b5, R.id.b6, R.id.b7, R.id.b8, R.id.b9, R.id.bAdd, R.id.bSubtract, R.id.bDivide, R.id.bMultiply, R.id.bPercent, R.id.bPlusMinus, R.id.bDot, R.id.bResult, R.id.bClear, R.id.bDelete)
    protected var buttons: java.util.List<Button>? = null

    @JvmField
    @SystemService
    protected var vibrator: Vibrator? = null

    @JvmField
    @FragmentArg
    protected var amount: String? = null
    private val stack = Stack<String>()
    private var result = "0"
    private var isRestart = true
    private var isInEquals = false
    private var lastOp = '\u0000'
    private var listener: AmountListener? = null
    fun setListener(listener: AmountListener?) {
        this.listener = listener
    }

    @AfterInject
    fun init() {
    }

    @AfterViews
    fun initUi() {
        setDisplay(amount)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    @Click(R.id.b0, R.id.b1, R.id.b2, R.id.b3, R.id.b4, R.id.b5, R.id.b6, R.id.b7, R.id.b8, R.id.b9, R.id.bAdd, R.id.bSubtract, R.id.bDivide, R.id.bMultiply, R.id.bPercent, R.id.bPlusMinus, R.id.bDot, R.id.bResult, R.id.bClear, R.id.bDelete)
    fun onButtonClick(v: View) {
        val b = v as Button
        val c = b.text[0]
        onButtonClick(c)
    }

    @Click(R.id.bOK)
    fun onOk() {
        if (!isInEquals) {
            doEqualsChar()
        }
        listener!!.onAmountChanged(result)
        dismiss()
    }

    @Click(R.id.bCancel)
    fun onCancel() {
        dismiss()
    }

    private fun setDisplay(s: String?) {
        var s = s
        if (Utils.isNotEmpty(s)) {
            s = s!!.replace(",".toRegex(), ".")
            result = s
            tvResult!!.text = s
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog.setOnKeyListener(DialogInterface.OnKeyListener { dialog, keyCode, event -> /* Your logic, you get the KeyEvent*/
            if (event.action == KeyEvent.ACTION_UP) {
                if (event.keyCode == KeyEvent.KEYCODE_DEL) {
                    doBackspace()
                    return@OnKeyListener true
                }
                if (event.keyCode == KeyEvent.KEYCODE_C) {
                    resetAll()
                    return@OnKeyListener true
                }
                when (event.unicodeChar.toChar()) {
                    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '-', '*', '/', '%', '.', '=' -> {
                        doButton(event.unicodeChar.toChar())
                        return@OnKeyListener true
                    }
                    '\n' -> {
                        onOk()
                        return@OnKeyListener true
                    }
                    ' ' -> {
                        doButton('=')
                        return@OnKeyListener true
                    }
                }
            }
            false
        })
    }

    private fun onButtonClick(c: Char) {
        if (vibrator != null && MyPreferences.isPinHapticFeedbackEnabled(activity)) {
            vibrator!!.vibrate(20)
        }
        when (c) {
            'C' -> resetAll()
            '<' -> doBackspace()
            else -> doButton(c)
        }
    }

    private fun resetAll() {
        setDisplay("0")
        tvOp!!.text = ""
        lastOp = '\u0000'
        isRestart = true
        stack.clear()
    }

    private fun doBackspace() {
        val s = tvResult!!.text.toString()
        if ("0" == s || isRestart) {
            return
        }
        var newDisplay = if (s.length > 1) s.substring(0, s.length - 1) else "0"
        if ("-" == newDisplay) {
            newDisplay = "0"
        }
        setDisplay(newDisplay)
    }

    private fun doButton(c: Char) {
        if (Character.isDigit(c) || c == '.') {
            addChar(c)
        } else {
            when (c) {
                '+', '-', '/', '*' -> doOpChar(c)
                '%' -> doPercentChar()
                '=', '\r' -> doEqualsChar()
                '\u00B1' -> setDisplay(BigDecimal(result).negate().toPlainString())
            }
        }
    }

    private fun addChar(c: Char) {
        var s = tvResult!!.text.toString()
        if (c == '.' && s.indexOf('.') != -1 && !isRestart) {
            return
        }
        if ("0" == s) {
            s = c.toString()
        } else {
            s += c
        }
        setDisplay(s)
        if (isRestart) {
            setDisplay(c.toString())
            isRestart = false
        }
    }

    private fun doOpChar(op: Char) {
        if (isInEquals) {
            stack.clear()
            isInEquals = false
        }
        stack.push(result)
        doLastOp()
        lastOp = op
        tvOp!!.text = lastOp.toString()
    }

    private fun doLastOp() {
        isRestart = true
        if (lastOp == '\u0000' || stack.size == 1) {
            return
        }
        val valTwo = stack.pop()
        val valOne = stack.pop()
        when (lastOp) {
            '+' -> stack.push(asNumber(valOne).add(asNumber(valTwo)).toPlainString())
            '-' -> stack.push(asNumber(valOne).subtract(asNumber(valTwo)).toPlainString())
            '*' -> stack.push(asNumber(valOne).multiply(asNumber(valTwo)).toPlainString())
            '/' -> {
                val d2 = asNumber(valTwo)
                if (d2.toInt() == 0) {
                    stack.push("0.0")
                } else {
                    stack.push(asNumber(valOne).divide(d2, 2, BigDecimal.ROUND_HALF_UP).toPlainString())
                }
            }
            else -> {
            }
        }
        setDisplay(stack.peek())
        if (isInEquals) {
            stack.push(valTwo)
        }
    }

    private fun asNumber(s: String): BigDecimal {
        return if (StringUtil.isEmpty(s)) {
            BigDecimal.ZERO
        } else BigDecimal(s)
    }

    private fun doPercentChar() {
        if (stack.size == 0) return
        setDisplay(BigDecimal(result).divide(Utils.HUNDRED, 2, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal(stack.peek())).toPlainString())
        tvOp!!.text = ""
    }

    private fun doEqualsChar() {
        if (lastOp == '\u0000') {
            return
        }
        if (!isInEquals) {
            isInEquals = true
            stack.push(result)
        }
        doLastOp()
        tvOp!!.text = ""
    }
}