package com.oceaniat.sms2mail

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputEditText
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private val io = Executors.newSingleThreadExecutor()

    private lateinit var swEnabled: SwitchCompat
    private lateinit var etUser: TextInputEditText
    private lateinit var etPass: TextInputEditText
    private lateinit var etHost: TextInputEditText
    private lateinit var etPort: TextInputEditText
    private lateinit var swSsl: SwitchCompat
    private lateinit var etTo: TextInputEditText
    private lateinit var swForwardAll: SwitchCompat
    private lateinit var etKeywords: TextInputEditText
    private lateinit var tvStatus: TextView
    private lateinit var tvLog: TextView

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        updateStatus()
        refreshLog()
        toast(if (granted) "已获得短信读取权限" else "未授予权限，收到短信时无法转发")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = Prefs(this)
        bindViews()
        loadToUi()
        updateStatus()
        refreshLog()

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            saveFromUi()
            updateStatus()
            toast("已保存")
        }
        findViewById<Button>(R.id.btnTest).setOnClickListener { sendTestMail() }
        findViewById<Button>(R.id.btnPerm).setOnClickListener {
            permLauncher.launch(Manifest.permission.RECEIVE_SMS)
        }
        findViewById<Button>(R.id.btnClearLog).setOnClickListener {
            LogStore.clear(this)
            refreshLog()
            toast("记录已清空")
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
        refreshLog()
    }

    override fun onDestroy() {
        super.onDestroy()
        io.shutdown()
    }

    private fun bindViews() {
        swEnabled = findViewById(R.id.swEnabled)
        etUser = findViewById(R.id.etUser)
        etPass = findViewById(R.id.etPass)
        etHost = findViewById(R.id.etHost)
        etPort = findViewById(R.id.etPort)
        swSsl = findViewById(R.id.swSsl)
        etTo = findViewById(R.id.etTo)
        swForwardAll = findViewById(R.id.swForwardAll)
        etKeywords = findViewById(R.id.etKeywords)
        tvStatus = findViewById(R.id.tvStatus)
        tvLog = findViewById(R.id.tvLog)
    }

    private fun loadToUi() {
        val cfg = prefs.load()
        swEnabled.isChecked = cfg.enabled
        etUser.setText(cfg.username)
        etPass.setText(cfg.password)
        etHost.setText(cfg.smtpHost)
        etPort.setText(cfg.smtpPort.toString())
        swSsl.isChecked = cfg.ssl
        etTo.setText(cfg.to)
        swForwardAll.isChecked = cfg.forwardAll
        etKeywords.setText(cfg.keywords)
    }

    private fun saveFromUi() {
        val ssl = swSsl.isChecked
        val port = etPort.text?.toString()?.trim()?.toIntOrNull() ?: if (ssl) 465 else 587
        prefs.save(
            MailConfig(
                enabled = swEnabled.isChecked,
                smtpHost = etHost.text?.toString()?.trim().orEmpty().ifEmpty { "smtp.163.com" },
                smtpPort = port,
                ssl = ssl,
                username = etUser.text?.toString()?.trim().orEmpty(),
                password = etPass.text?.toString()?.trim().orEmpty(),
                fromName = prefs.load().fromName,
                to = etTo.text?.toString()?.trim().orEmpty(),
                forwardAll = swForwardAll.isChecked,
                keywords = etKeywords.text?.toString()?.trim().orEmpty()
            )
        )
    }

    private fun sendTestMail() {
        saveFromUi()
        updateStatus()
        val cfg = prefs.load()
        if (!cfg.isReady()) {
            toast("请先填好发件邮箱、授权码和收件邮箱")
            return
        }
        toast("正在发送测试邮件…")
        io.execute {
            var ok = false
            var msg = ""
            try {
                MailSender.send(
                    cfg,
                    "【测试】短信转邮箱已连通",
                    "这是一封测试邮件，收到就说明配置正确。\n\n" +
                        "发件账号：${cfg.username}\n" +
                        "收件人：${cfg.recipients().joinToString(", ")}\n" +
                        "SMTP：${cfg.smtpHost}:${cfg.smtpPort}（${if (cfg.ssl) "SSL" else "STARTTLS"}）\n"
                )
                ok = true
                msg = "测试邮件已发送，去收件箱看看"
            } catch (e: Exception) {
                ok = false
                msg = MailSender.friendlyError(e)
            }
            LogStore.add(this, "测试", "", ok, msg)
            runOnUiThread {
                refreshLog()
                toast(msg)
            }
        }
    }

    private fun updateStatus() {
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED
        val cfg = prefs.load()

        val parts = mutableListOf<String>()
        parts += if (granted) "短信权限 ✓" else "短信权限 ✗"
        parts += if (cfg.isReady()) "邮箱配置 ✓" else "邮箱配置待完善"
        parts += if (cfg.enabled) "转发中" else "已暂停"
        tvStatus.text = parts.joinToString("   ·   ")

        val btn = findViewById<Button>(R.id.btnPerm)
        btn.isEnabled = !granted
        btn.text = if (granted) "短信权限已授予" else "授予短信读取权限"
    }

    private fun refreshLog() {
        tvLog.text = LogStore.render(this)
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
