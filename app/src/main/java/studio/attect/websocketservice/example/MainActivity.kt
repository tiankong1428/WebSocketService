package studio.attect.websocketservice.example

import java.util.zip.Inflater
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
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
                        textView.disableClick()
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
                        actionButton.enableClick()
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
                        recyclerViewAdapter.addContent(
                            BubbleData(
                                SENDER_SYSTEM,
                                "重新连接中",
                                null,
                                false,
                                System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
        })

        webSocketViewModel.receiveStringData.observe(
            this,
            object : StringDataObserver(webSocketViewModel) {
                override fun onReceive(t: String) {
                    Log.d("Test", "first string observe receive:$t")
                    recyclerViewAdapter.addContent(
                        BubbleData(
                            SENDER_SERVER,
                            t,
                            null,
                            false,
                            System.currentTimeMillis()
                        )
                    )
                }
            })
        webSocketViewModel.receiveStringData.observe(
            this,
            object : StringDataObserver(webSocketViewModel) {
                override fun onReceive(t: String) {
                    Log.d("Test", "second string observe receive:$t")
                }
            })

        webSocketViewModel.receiveBytesData.observe(this,
            object : BytesDataObserver(webSocketViewModel) {
                override fun onReceive(b: ByteString) {
                    recyclerViewAdapter.addContent(
                        BubbleData(
                            SENDER_SERVER,
                            null,
                            b.toByteArray(),
                            true,
                            System.currentTimeMillis()
                        )
                    )
                }
            })
    }

    /**
     * 创建通知前，需要向系统中创建一个通知频道
     * 仅在安卓O(8.0/26)及其更高版本才必须
     * @param channelId 频道ID,自定义字符串
     * @param channelName 频道名称，用户可以在系统设置中看到相关名称
     * @param channelDescription 频道描述，用户可以在系统设置中看到相关描述，用于向用户解释为何需要一个这样的通知
     * @return channelName
     */
    private fun createNotificationChannel(
        channelId: String = NOTIFICATION_CHANNEL_ID,
        channelName: String = getString(R.string.description_keep_websocket_active_in_background),
        channelDescription: String = getString(R.string.description_use_notification_keep_websocket)
    ): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = channelDescription
            //向系统注册频道，你不能在之后再进行对其进行重要性修改
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)

            channelId
        } else {
            ""
        }
    }
fun decompressAndParseHex(hexData: String): String {
    val byteData = hexStringToByteArray(hexData)

    for (headerLength in 2..15) {
        val dataStartPos = headerLength
        val dataToDecompress = byteData.copyOfRange(dataStartPos, byteData.size)

        try {
            val decompressedData = decompress(dataToDecompress)
            val jsonData = String(decompressedData, StandardCharsets.UTF_8)

            val objectMapper = jacksonObjectMapper()
            return try {
                val parsedData: Any = objectMapper.readValue(jsonData)
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(parsedData)
            } catch (e: Exception) {
                "JSON 解码错误: ${e.message}\n原始数据: $jsonData"
            }

        } catch (e: Exception) {
            // 继续尝试下一个头部长度
        }
    }

    return "没有找到有效的解压缩数据。"
}

fun hexStringToByteArray(s: String): ByteArray {
    val len = s.length
    val data = ByteArray(len / 2)
    for (i in 0 until len step 2) {
        data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
    }
    return data
}

fun decompress(data: ByteArray): ByteArray {
    val inflater = Inflater()
    inflater.setInput(data)
    val outputStream = ByteArrayOutputStream(data.size)

    return try {
        val buffer = ByteArray(1024)
        while (!inflater.finished()) {
            val size = inflater.inflate(buffer)
            outputStream.write(buffer, 0, size)
        }
        outputStream.toByteArray()
    } finally {
        inflater.end()
        outputStream.close()
    }
}
    /**
     * 获得系统通知服务管理者
     * 需要通过它来与系统通知功能进行操作
     * @return NotificationManager
     */
    private fun getNotificationManager(): NotificationManager {
        return getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    /**
     * 为WebSocket服务创建一个前台通知
     * @return Notification 通知本身
     */
    private fun createForeRunningNotification(title: String, content: String): Notification {
        val channel = createNotificationChannel()

        val backToMainActivityIntent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_MAIN
            addCategory(CATEGORY_LAUNCHER)
        }

        val backToMainActivityPendingIntent =
            PendingIntent.getActivity(this, 0, backToMainActivityIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, channel).apply {
            setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
            setSmallIcon(R.drawable.ic_notification_icon)
            setContentTitle(title)
            setContentText(content)
            setContentIntent(backToMainActivityPendingIntent)
            priority = NotificationCompat.PRIORITY_MIN //让通知不在状态栏显示图标，处于通知列表的最下方，避免打扰用户使用
        }.build()


        //直接向系统通知服务管理者请求发送通知
        getNotificationManager().notify(NOTIFICATION_ID, notification)

        return notification
    }


    inner class MyRecyclerViewAdapter : RecyclerView.Adapter<BubbleViewHolder>() {


        /**
         * 列表实时内容
         */
        private val contents: ArrayList<BubbleData> = arrayListOf()

        /**
         * 列表大小，避免实时求
         */
        private var size = 0

        fun addContent(data: BubbleData) {
            contents.add(data)
            size++
            notifyItemInserted(size - 1)
            recyclerView.smoothScrollToPosition(size - 1) //自动滚动到底部
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BubbleViewHolder {
            return when (viewType) {
                SENDER_SERVER -> BubbleViewHolder(
                    layoutInflater.inflate(
                        R.layout.bubble_server,
                        parent,
                        false
                    )
                )
                SENDER_CLIENT -> BubbleViewHolder(
                    layoutInflater.inflate(
                        R.layout.bubble_client,
                        parent,
                        false
                    )
                )
                else -> BubbleViewHolder(
                    layoutInflater.inflate(
                        R.layout.bubble_system,
                        parent,
                        false
                    )
                )
            }
        }

        override fun getItemCount(): Int = size

        override fun onBindViewHolder(holder: BubbleViewHolder, position: Int) {
            holder.applyData(contents[position])
        }

        override fun getItemViewType(position: Int): Int {
            return when {
                contents[position].sender == SENDER_SERVER -> SENDER_SERVER
                contents[position].sender == SENDER_CLIENT -> SENDER_CLIENT
                else -> SENDER_SYSTEM
            }
        }

    }

    inner class BubbleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val time: AppCompatTextView? = itemView.findViewById(R.id.time)
        private val content: AppCompatTextView? = itemView.findViewById(R.id.content)
        private val type: AppCompatTextView? = itemView.findViewById(R.id.type)

        fun applyData(data: BubbleData) {
            time?.text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(data.time))
            if (data.hex) {
                data.byteContent?.let {
                    val hexString = it.toHexString()
                    
                    // 调用解压缩和解析 JSON 函数
                    val result = decompressAndParseHex(hexString)

                    // 更新内容和类型文本
                    content?.text = hexString
                    type?.text = String.format(getString(R.string.type_hex_size), it.size)

                }

            } else {
                data.strContent?.let {
                    content?.text = it
                    type?.text = String.format(getString(R.string.type_string_size), it.length)
                }

            }
        }
    }

    companion object {
        /**
         * 通知频道的id
         * 字符串类型，自己随意定义
         */
        const val NOTIFICATION_CHANNEL_ID = "websocket"

        /**
         * 通知的id
         * 此ID用于给系统分辨是否为同一个通知（同一个即为更新）
         */
        const val NOTIFICATION_ID = 1

        /**
         * 标识：系统消息
         */
        const val SENDER_SYSTEM = 0

        /**
         * 标识：发送方：服务端（远端）
         */
        const val SENDER_SERVER = 1

        /**
         * 标识：发送方：客户端（本机）
         */
        const val SENDER_CLIENT = 2
    }


}