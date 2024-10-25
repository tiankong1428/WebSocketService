package studio.attect.websocketservice.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat

fun AppCompatTextView.enableClick(){
    setTextColor(ContextCompat.getColor(context, R.color.colorPrimary))
    isFocusable = true
    isClickable = true
}

fun AppCompatTextView.disableClick(){
    setTextColor(ContextCompat.getColor(context, R.color.colorDisable))
    isFocusable = false
    isClickable = false
}

fun AppCompatTextView.enableClickCopy() {
    this.setOnClickListener {
        // 复制文本到剪贴板
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Copied Text", this.text)
        clipboard.setPrimaryClip(clip)

        // 提示用户已复制
        Toast.makeText(context, "文本已复制", Toast.LENGTH_SHORT).show()
    }
}
