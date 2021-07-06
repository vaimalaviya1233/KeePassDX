/*
 * Copyright 2021 Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePassDX.
 *
 *  KeePassDX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.DateInstant
import java.util.*

class DateTimeView @JvmOverloads constructor(context: Context,
                                             attrs: AttributeSet? = null,
                                             defStyle: Int = 0)
    : FrameLayout(context, attrs, defStyle), GenericDateTimeView {

    private var dateTimeLabelView: TextView
    private var dateTimeValueView: TextView
    private var expiresImage: ImageView

    private var mActivated: Boolean = false
    private var mDateTime: DateInstant = DateInstant.IN_ONE_MONTH_DATE_TIME

    var setOnDateClickListener: ((DateInstant) -> Unit)? = null

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        inflater?.inflate(R.layout.view_date_time, this)

        dateTimeLabelView = findViewById(R.id.date_time_label)
        dateTimeValueView = findViewById(R.id.date_time_value)
        expiresImage = findViewById(R.id.expires_image)
    }

    private fun assignExpiresDateText() {
        expiresImage.isVisible = if (mActivated) {
            mDateTime.date.before(Date())
        } else {
            false
        }

        dateTimeValueView.text = if (mActivated) {
            mDateTime.getDateTimeString(resources)
        } else {
            resources.getString(R.string.never)
        }
    }

    var label: String
        get() {
            return dateTimeLabelView.text.toString()
        }
        set(value) {
            dateTimeLabelView.text = value
        }

    var type: DateInstant.Type
        get() {
            return mDateTime.type
        }
        set(value) {
            mDateTime.type = value
        }

    override var activation: Boolean
        get() {
            return mActivated
        }
        set(value) {
            mActivated = value
            dateTime = if (value) {
                when (mDateTime.type) {
                    DateInstant.Type.DATE_TIME -> DateInstant.IN_ONE_MONTH_DATE_TIME
                    DateInstant.Type.DATE -> DateInstant.IN_ONE_MONTH_DATE
                    DateInstant.Type.TIME -> DateInstant.IN_ONE_HOUR_TIME
                }
            } else {
                DateInstant.NEVER_EXPIRES
            }
        }

    /**
     * Warning dateTime.type is ignore, use type instead
     */
    override var dateTime: DateInstant
        get() {
            return if (activation)
                mDateTime
            else
                DateInstant.NEVER_EXPIRES
        }
        set(value) {
            mDateTime = DateInstant(value.date, mDateTime.type)
            assignExpiresDateText()
        }

    override var isFieldVisible: Boolean
        get() {
            return isVisible
        }
        set(value) {
            isVisible = value
        }
}
