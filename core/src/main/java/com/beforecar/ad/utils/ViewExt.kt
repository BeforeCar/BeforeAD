package com.beforecar.ad.utils

import android.view.View

fun View.setVisible(visible: Boolean) {
    this.visibility = if (visible) View.VISIBLE else View.GONE
}

fun View.setInVisible(visible: Boolean) {
    this.visibility = if (visible) View.INVISIBLE else View.VISIBLE
}
