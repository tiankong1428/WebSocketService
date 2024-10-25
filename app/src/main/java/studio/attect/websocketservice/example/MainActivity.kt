package studio.attect.websocketservice.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_MAIN
import android.content.Intent.CATEGORY_LAUNCHER
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*
import okio.ByteString
import okio.ByteString.Companion.toByteString
import studio.attect.staticviewmodelstore.StaticViewModelLifecycleActivity
import studio.attect.websocketservice.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author attect
 * @date 2019-05-04
 */
class MainActivity : StaticViewModelLifecycleActivity() {

    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var recyclerViewAdapter: MyRecyclerViewAdapter

    private lateinit var webSocketViewModel: WebSocketServiceViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        WebSocketService.getViewModel(this)?.let {
            webSocketViewModel = it
        }


        layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        recyclerViewAdapter = MyRecyclerViewAdapter()

        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = recyclerViewAdapter


        actionButton.setOnClickListener { view ->
            (view as AppCompatTextView).let { textView ->
                if (webSocketViewModel.status.value != WebSocketStatus.CONNECTED) {
                    addressEditText.text?.toString()?.let { url ->
                        textview.enableClickCopy()
                        addressEditText.isEnabled = false
                        notificationCheckBox.isEnabled = false
                        val handshakeHeader = WebSocketHandshakeHeader("client", "WebSocketService")
                        if (notificationCheckBox.isChecked) {
                            WebSocketService.startService(
                                this,
                                url,
                                NOTIFICATION_ID,
                                createForeRunningNotification("WebSocket", "connect to $url"),
                                arrayListOf(handshakeHeader),
                                5000
                            )
                        } else {
                            WebSocketService.startService(this, url, arrayListOf(handshakeHeader),5000)
                        }
                    }
                } else {
                    webSocketViewModel.stopByUser.value = true
                }

            }
        }

        sendButton.setOnClickListener {
            editText.text?.toString()?.let {
                if (hexCheckBox.isChecked) {
                    val byteArray = it.hexStringToByteArray()
                    val byteString = byteArray.toByteString(0, byteArray.size)
                    webSocketViewModel.sendBytesData.value = byteString
                    recyclerViewAdapter.addContent(
                        BubbleData(
                            SENDER_CLIENT,
                            null,
                            byteArray,
                            true,
                            System.currentTimeMillis()
                        )
                    )
                } else {
                    webSocketViewModel.sendStringData.value = it
                    recyclerViewAdapter.addContent(
                        BubbleData(
                            SENDER_CLIENT,
                            it,
                            null,
                            false,
                            System.currentTimeMillis()
                        )
                    )
                }
            }
        }
        sendButton.disableClick()

        webSocketViewModel.status.observe(this, androidx.lifecycle.Observer { webSocketStatus ->
            webSocketStatus?.let {
                when (it) {
                    WebSocketStatus.DISCONNECTED -> {
                        actionButtonclickClick()
                        actionButton.setText(R.string.connect)
                        sendButton.disableClick()
                        addressEditText.isEnabled = true
                        notificationCheckBox.isEnabled = true
                        recyclerViewAdapter.addContent(
                            BubbleData(
                                SENDER_SYSTEM,
                                "连接已断开",
                                null,
                                false,
                                System.currentTimeMillis()
                            )
                        )
                    }
                    WebSocketStatus.CONNECTING -> {
                        actionButton.setText(R.string.connecting)
                        recyclerViewAdapter.addContent(
                            BubbleData(
                                SENDER_SYSTEM,
                                "连接中",
                                null,
                                false,
                                System.currentTimeMillis()
                            )
                        )
                    }
                    WebSocketStatus.CONNECTED -> {
                        actionButton.setText(R.string.disconnect)
                        sendButton.enableClick()
                        recyclerViewAdapter.addContent(
                            BubbleData(
                                SENDER_SYSTEM,
                                "已连接",
                                null,
                                false,
                                System.currentTimeMillis()
                            )
                        )
                        actionButton.enableClick()
                    }
                    WebSocketStatus.CLOSING -> {
                        actionButton.setText(R.string.ws_closing)
                        recyclerViewAdapter.addContent(
                            BubbleData(
                                SENDER_SYSTEM,
                                "正在断开连接",
                                null,
                                false,
                                System.currentTimeMillis()
                            )
                        )
                    }
                    WebSocketStatus.RECONNECTING -> {
                        actionButton.disableClick()
                        actionButton.setText(R.string.retrying)