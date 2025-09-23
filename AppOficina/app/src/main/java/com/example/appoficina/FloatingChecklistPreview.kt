package com.example.appoficina

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.appoficina.R
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class FloatingChecklistPreview(
    private val activity: AppCompatActivity,
    private val previewContainer: View,
    private val previewContent: LinearLayout,
    private val previewScroll: ScrollView,
    previewHeader: View,
    previewCloseButton: ImageButton,
    private val previewToggleButton: ImageButton? = null,
    private val sectionKey: String? = null,
) {
    private var previewLoaded = false
    private var previewVisible = false
    private var fetchInProgress = false
    private val renderer = ChecklistPreviewRenderer(activity, sectionKey)
    private val useToggleIcons = sectionKey?.equals("posto02", ignoreCase = true) != true

    init {
        previewCloseButton.setOnClickListener {
            hidePreview()
        }

        var dragOffsetX = 0f
        var dragOffsetY = 0f
        previewHeader.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dragOffsetX = previewContainer.x - event.rawX
                    dragOffsetY = previewContainer.y - event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val parent = previewContainer.parent
                    if (parent is View && parent.width > 0 && parent.height > 0) {
                        val maxX = (parent.width - previewContainer.width).coerceAtLeast(0)
                        val maxY = (parent.height - previewContainer.height).coerceAtLeast(0)
                        val newX = (event.rawX + dragOffsetX).coerceIn(0f, maxX.toFloat())
                        val newY = (event.rawY + dragOffsetY).coerceIn(0f, maxY.toFloat())
                        previewContainer.x = newX
                        previewContainer.y = newY
                        true
                    } else {
                        false
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> true
                else -> false
            }
        }

        previewToggleButton?.apply {
            visibility = View.GONE
            isEnabled = false
            if (useToggleIcons) {
                setImageResource(android.R.drawable.ic_menu_view)
            } else {
                setImageDrawable(null)
            }
            contentDescription = activity.getString(R.string.show_previous_checklist)
            setOnClickListener {
                if (previewVisible) {
                    hidePreview()
                } else if (previewLoaded) {
                    showPreview(animated = true)
                }
            }
        }
    }

    fun loadPreviousChecklist(obra: String?, ano: String?) {
        if (obra.isNullOrBlank() || previewLoaded || fetchInProgress) {
            return
        }

        fetchInProgress = true

        Thread {
            val ip = activity.getSharedPreferences("config", Context.MODE_PRIVATE)
                .getString("api_ip", "192.168.0.135")
            if (ip.isNullOrBlank()) {
                fetchInProgress = false
                return@Thread
            }

            val isPosto02 = sectionKey?.equals("posto02", ignoreCase = true) == true
            val endereco = try {
                val path = if (isPosto02) {
                    "/json_api/posto02/checklist"
                } else {
                    "/json_api/checklist"
                }
                val builder = StringBuilder("http://$ip:5000")
                builder.append(path)
                builder.append("?obra=")
                builder.append(URLEncoder.encode(obra, "UTF-8"))
                if (!ano.isNullOrBlank()) {
                    builder.append("&ano=")
                    builder.append(URLEncoder.encode(ano, "UTF-8"))
                }
                builder.toString()
            } catch (_: Exception) {
                fetchInProgress = false
                return@Thread
            }

            try {
                val url = URL(endereco)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                val codigo = conn.responseCode
                if (codigo in 200..299) {
                    val resposta = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(resposta)
                    val embeddedChecklist = if (isPosto02) {
                        json.optJSONObject("checklist") ?: json
                    } else {
                        json.optJSONObject("checklist")
                    }
                    val displayChecklist = when {
                        embeddedChecklist == null -> json
                        embeddedChecklist === json -> json
                        else -> mergeChecklistPayload(embeddedChecklist, json)
                    }
                    activity.runOnUiThread {
                        mostrarChecklist(displayChecklist)
                    }
                }
                conn.disconnect()
            } catch (_: Exception) {
            } finally {
                fetchInProgress = false
            }
        }.start()
    }

    private fun mergeChecklistPayload(primary: JSONObject, original: JSONObject): JSONObject {
        val merged = JSONObject(primary.toString())

        fun copyPrimitive(key: String) {
            if (!merged.has(key)) {
                val value = original.opt(key)
                if (value != null && value != JSONObject.NULL) {
                    merged.put(key, value)
                }
            }
        }

        copyPrimitive("obra")
        copyPrimitive("ano")

        if (!merged.has("respondentes")) {
            original.optJSONObject("respondentes")?.let { merged.put("respondentes", it) }
        }

        if (!merged.has("itens")) {
            original.optJSONArray("itens")?.let { merged.put("itens", it) }
        }

        sectionKey?.let { key ->
            if (!merged.has(key)) {
                val directMatch = original.optJSONObject(key)
                if (directMatch != null) {
                    merged.put(key, directMatch)
                } else {
                    val iterator = original.keys()
                    while (iterator.hasNext()) {
                        val candidate = iterator.next()
                        if (candidate.equals(key, ignoreCase = true)) {
                            val secao = original.optJSONObject(candidate)
                            if (secao != null) {
                                merged.put(key, secao)
                            }
                            break
                        }
                    }
                }
            }
        } ?: run {
            val iterator = original.keys()
            while (iterator.hasNext()) {
                val key = iterator.next()
                if (merged.has(key)) {
                    continue
                }
                val secao = original.optJSONObject(key) ?: continue
                if (secao.optJSONArray("itens") != null) {
                    merged.put(key, secao)
                }
            }
        }

        return merged
    }

    private fun mostrarChecklist(checklist: JSONObject) {
        if (activity.isFinishing || activity.isDestroyed) {
            return
        }

        val rendered = renderer.render(previewContent, checklist)
        if (!rendered) {
            exibirMensagemVazia()
            return
        }

        previewLoaded = true
        previewToggleButton?.apply {
            visibility = View.VISIBLE
            isEnabled = true
        }

        showPreview(animated = true)
    }

    private fun exibirMensagemVazia() {
        previewLoaded = true
        val density = activity.resources.displayMetrics.density
        val padding = (16 * density).toInt()
        previewContent.setPadding(padding, padding, padding, padding)
        previewContent.addView(
            TextView(activity).apply {
                text = activity.getString(R.string.no_previous_checklist)
            },
        )
        previewToggleButton?.apply {
            visibility = View.VISIBLE
            isEnabled = true
        }
        showPreview(animated = true)
    }

    private fun showPreview(animated: Boolean) {
        if (!previewLoaded || activity.isFinishing || activity.isDestroyed) {
            return
        }

        previewScroll.scrollTo(0, 0)
        previewToggleButton?.apply {
            if (useToggleIcons) {
                setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            } else {
                setImageDrawable(null)
            }
            contentDescription = activity.getString(R.string.hide_previous_checklist)
        }

        if (animated) {
            previewContainer.alpha = 0f
            previewContainer.visibility = View.VISIBLE
            previewContainer.animate().alpha(1f).setDuration(200L).start()
        } else {
            previewContainer.animate().cancel()
            previewContainer.alpha = 1f
            previewContainer.visibility = View.VISIBLE
        }
        previewVisible = true
    }

    private fun hidePreview() {
        previewContainer.animate().cancel()
        previewContainer.visibility = View.GONE
        previewVisible = false
        previewToggleButton?.apply {
            if (useToggleIcons) {
                setImageResource(android.R.drawable.ic_menu_view)
            } else {
                setImageDrawable(null)
            }
            contentDescription = activity.getString(R.string.show_previous_checklist)
        }
    }
}

