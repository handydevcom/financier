package com.handydev.financier.view

import android.Manifest
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.core.content.FileProvider
import com.handydev.financier.BuildConfig
import com.handydev.financier.R
import com.handydev.financier.activity.RequestPermission
import com.handydev.financier.utils.PicturesUtil
import com.handydev.financier.utils.getColorHelper

class NodeInflater(var inflater: LayoutInflater) {

    fun addDivider(layout: LinearLayout): View {
        val divider = inflater.inflate(R.layout.edit_divider, layout, false)
        layout.addView(divider)
        return divider
    }

    open inner class Builder {
        protected val layout: LinearLayout
        protected val v: View
        private var divider = true

        constructor(layout: LinearLayout, layoutId: Int) {
            this.layout = layout
            v = inflater.inflate(layoutId, layout, false)
        }

        constructor(layout: LinearLayout, v: View) {
            this.layout = layout
            this.v = v
        }

        fun withId(id: Int, listener: View.OnClickListener?): Builder {
            v.id = id
            v.setOnClickListener(listener)
            return this
        }

        fun withLabel(labelId: Int, darkUI: Boolean = false): Builder {
            val labelView = v.findViewById<TextView>(R.id.label)
            labelView.setText(labelId)
            if (darkUI) {
                labelView.setTextColor(getColorHelper(inflater.context, R.color.main_text_color))
            }
            return this
        }

        fun withLabel(labelId: Int): Builder {
            val labelView = v.findViewById<TextView>(R.id.label)
            labelView.setText(labelId)
            return this
        }

        fun withLabel(label: String?): Builder {
            val labelView = v.findViewById<TextView>(R.id.label)
            labelView.text = label
            return this
        }

        fun withData(labelId: Int): Builder {
            val labelView = v.findViewById<TextView>(R.id.data)
            labelView.setText(labelId)
            return this
        }

        fun withData(label: String?): Builder {
            val labelView = v.findViewById<TextView>(R.id.data)
            labelView.text = label
            return this
        }

        fun withIcon(iconId: Int): Builder {
            val iconView = v.findViewById<ImageView>(R.id.icon)
            iconView.setImageResource(iconId)
            return this
        }

        fun withNoDivider(): Builder {
            divider = false
            return this
        }

        fun create(): View {
            layout.addView(v)
            if (divider) {
                val dividerView = addDivider(layout)
                v.tag = dividerView
            }
            return v
        }
    }

    inner class EditBuilder(layout: LinearLayout, view: View?) : Builder(layout, R.layout.select_entry_edit) {
        init {
            val relativeLayout = v.findViewById<RelativeLayout>(R.id.layout)
            val layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
            layoutParams.addRule(RelativeLayout.ALIGN_LEFT, R.id.label)
            layoutParams.addRule(RelativeLayout.BELOW, R.id.label)
            relativeLayout.addView(view, layoutParams)
        }
    }

    open inner class ListBuilder(layout: LinearLayout, layoutId: Int) : Builder(layout, layoutId) {
        open fun withButtonId(buttonId: Int, listener: View.OnClickListener?): ListBuilder {
            val plusImageView = v.findViewById<ImageView>(R.id.plus_minus)
            if (buttonId > 0) {
                plusImageView.visibility = View.VISIBLE
                plusImageView.id = buttonId
                plusImageView.setOnClickListener(listener)
            } else {
                plusImageView.visibility = View.GONE
            }
            return this
        }

        fun withClearButtonId(buttonId: Int, listener: View.OnClickListener?): ListBuilder {
            val plusImageView = v.findViewById<ImageView>(R.id.bMinus)
            plusImageView.id = buttonId
            plusImageView.setOnClickListener(listener)
            return this
        }

        fun withAutoCompleteFilter(listener: View.OnClickListener?, listId: Int): ListBuilder {
            val autoCompleteTxt = v.findViewById<AutoCompleteTextView>(R.id.autocomplete_filter)
            autoCompleteTxt.isFocusableInTouchMode = true
            val view = v.findViewById<View>(R.id.show_list)
            view.id = listId
            view.setOnClickListener(listener)
            return this
        }

        fun withoutMoreButton(): ListBuilder {
            v.findViewById<View>(R.id.more).visibility = View.GONE
            return this
        }
    }

    inner class CheckBoxBuilder(layout: LinearLayout) : Builder(layout, R.layout.select_entry_checkbox) {
        fun withCheckbox(checked: Boolean): CheckBoxBuilder {
            val checkBox = v.findViewById<CheckBox>(R.id.checkbox)
            checkBox.isChecked = checked
            return this
        }
    }

    inner class PictureBuilder(layout: LinearLayout) : ListBuilder(layout, R.layout.select_entry_picture) {
        override fun withButtonId(buttonId: Int, listener: View.OnClickListener?): ListBuilder {
            val plusImageView = v.findViewById<ImageView>(R.id.plus_minus)
            plusImageView.visibility = View.VISIBLE
            return super.withButtonId(buttonId, listener)
        }

        fun withPicture(context: Context, pictureFileName: String?): PictureBuilder {
            val imageView = v.findViewById<ImageView>(R.id.picture)
            imageView.setOnClickListener { arg0: View? ->
                if (RequestPermission.isRequestingPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    return@setOnClickListener
                }
                val fileName = imageView.getTag(R.id.attached_picture) as String
                if (fileName != null) {
                    val target = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID, PicturesUtil.pictureFile(fileName, true))
                    val intent = Intent()
                    intent.action = Intent.ACTION_VIEW
                    intent.setDataAndType(target, "image/jpeg")
                    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    context.startActivity(intent)
                }
            }
            PicturesUtil.showImage(context, imageView, pictureFileName)
            return this
        }
    }
}