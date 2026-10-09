package app.pictoclip

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var store: ClipStore
    private lateinit var adapter: ClipAdapter
    private lateinit var clock: TextView
    private lateinit var prompt: TextView
    private lateinit var count: TextView
    private lateinit var empty: View
    private lateinit var topBar: LinearLayout
    private lateinit var bottomBar: LinearLayout

    private val handler = Handler(Looper.getMainLooper())
    private var selectedId = -1L

    private val tick = object : Runnable {
        override fun run() {
            clock.text = SimpleDateFormat("HH:mm  MM/dd", Locale.getDefault()).format(Date())
            handler.postDelayed(this, 10_000)
        }
    }
    private val resetPrompt = Runnable { prompt.text = PROMPT }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Ds.init(this)
        store = ClipStore(this)
        selectedId = savedInstanceState?.getLong(STATE_SELECTED, -1L) ?: -1L
        setContentView(buildUi())
        refresh()
        handleIncoming(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncoming(intent)
    }

    override fun onResume() {
        super.onResume()
        handler.post(tick)
    }

    override fun onPause() {
        handler.removeCallbacks(tick)
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(STATE_SELECTED, selectedId)
    }

    // ---- UI ------------------------------------------------------------------------------------

    private fun buildUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = Ds.gridBackground(this@MainActivity)
        }

        // DS-style status bar: title on the left, clock + date on the right.
        topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Ds.TOP_LIGHT, Ds.TOP_DARK),
            )
            minimumHeight = dp(32)
            setPadding(dp(10), 0, dp(10), 0)
        }
        topBar.addView(
            Ds.label(this, 14f, Color.WHITE).apply { text = "PICTOCLIP" },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
        )
        clock = Ds.label(this, 14f, Color.WHITE)
        topBar.addView(clock)
        root.addView(topBar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        // Prompt panel, like "Choose a Chat Room to join."
        val promptPanel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = PanelDrawable(dp(2).toFloat())
            setPadding(dp(14), dp(10), dp(14), dp(10))
        }
        prompt = Ds.label(this, 15f).apply { text = PROMPT }
        promptPanel.addView(prompt, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        count = Ds.label(this, 13f, Ds.SUBTLE)
        promptPanel.addView(count)
        root.addView(
            promptPanel,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dp(10), dp(10), dp(10), dp(4))
            },
        )

        // Clip list, with a greyed "no cartridge" style panel when there is nothing saved.
        adapter = ClipAdapter()
        val list = ListView(this).apply {
            divider = null
            dividerHeight = 0
            setSelector(ColorDrawable(Color.TRANSPARENT))
            setBackgroundColor(Color.TRANSPARENT)
            clipToPadding = false
            overScrollMode = View.OVER_SCROLL_NEVER
            adapter = this@MainActivity.adapter
            setOnItemClickListener { _, _, position, _ ->
                val clip = this@MainActivity.adapter.getItem(position)
                selectedId = clip.id
                this@MainActivity.adapter.notifyDataSetChanged()
                copy(clip)
            }
        }
        empty = Ds.label(this, 15f, Ds.SUBTLE).apply {
            text = "There are no clips yet.\nCopy something, then press A to paste."
            gravity = Gravity.CENTER
            setLineSpacing(0f, 1.2f)
            background = PanelDrawable(dp(2).toFloat(), Ds.BG)
            setPadding(dp(16), dp(24), dp(16), dp(24))
        }
        val listArea = FrameLayout(this)
        listArea.addView(list, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        listArea.addView(
            empty,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP).apply {
                setMargins(dp(10), dp(10), dp(10), 0)
            },
        )
        root.addView(listArea, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        // Bottom bar, like "B Go back / A Join".
        bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Ds.BAR)
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }
        val delete = DsButton(this, "B", "Delete").apply {
            setOnClickListener { deleteSelected() }
            setOnLongClickListener { confirmClear(); true }
        }
        val paste = DsButton(this, "A", "Paste").apply {
            setOnClickListener { pasteFromClipboard() }
        }
        bottomBar.addView(delete, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(5) })
        bottomBar.addView(paste, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(5) })
        root.addView(bottomBar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        // Edge-to-edge (enforced on Android 15+): keep our bars clear of the system bars.
        root.setOnApplyWindowInsetsListener { _, insets ->
            val l: Int
            val t: Int
            val r: Int
            val b: Int
            if (Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                l = bars.left; t = bars.top; r = bars.right; b = bars.bottom
            } else {
                @Suppress("DEPRECATION")
                run {
                    l = insets.systemWindowInsetLeft; t = insets.systemWindowInsetTop
                    r = insets.systemWindowInsetRight; b = insets.systemWindowInsetBottom
                }
            }
            topBar.minimumHeight = dp(32) + t
            topBar.setPadding(dp(10) + l, t, dp(10) + r, 0)
            bottomBar.setPadding(dp(10) + l, dp(8), dp(10) + r, dp(8) + b)
            insets
        }
        return root
    }

    // ---- actions -------------------------------------------------------------------------------

    private fun refresh() {
        adapter.setClips(store.all())
        empty.visibility = if (adapter.count == 0) View.VISIBLE else View.GONE
        count.text = "${adapter.count}/${ClipStore.MAX}"
    }

    private fun flash(message: String) {
        handler.removeCallbacks(resetPrompt)
        prompt.text = message
        handler.postDelayed(resetPrompt, 1500)
    }

    private fun save(text: String) {
        val clip = store.add(text)
        if (clip == null) {
            flash("Nothing to paste!")
            return
        }
        selectedId = clip.id
        refresh()
        flash("Saved!")
    }

    private fun pasteFromClipboard() {
        val cm = getSystemService(ClipboardManager::class.java)
        val clip = cm.primaryClip
        val text = if (clip != null && clip.itemCount > 0) clip.getItemAt(0).coerceToText(this)?.toString() else null
        if (text.isNullOrBlank()) flash("Clipboard is empty!") else save(text)
    }

    private fun copy(clip: Clip) {
        val cm = getSystemService(ClipboardManager::class.java)
        cm.setPrimaryClip(ClipData.newPlainText("Pictoclip", clip.text))
        flash("Copied!")
    }

    private fun deleteSelected() {
        if (selectedId == -1L || adapter.indexOf(selectedId) < 0) {
            flash("Select a clip first!")
            return
        }
        store.remove(selectedId)
        selectedId = -1L
        refresh()
        flash("Deleted!")
    }

    private fun confirmClear() {
        if (adapter.count == 0) return
        AlertDialog.Builder(this)
            .setMessage("Delete all clips?")
            .setPositiveButton("Yes") { _, _ ->
                store.clear()
                selectedId = -1L
                refresh()
                flash("All clips deleted!")
            }
            .setNegativeButton("No", null)
            .show()
    }

    /** Text arriving from the Share sheet or the text-selection menu is saved straight away. */
    private fun handleIncoming(i: Intent?) {
        i ?: return
        val text = when (i.action) {
            Intent.ACTION_SEND -> i.getCharSequenceExtra(Intent.EXTRA_TEXT)
            Intent.ACTION_PROCESS_TEXT -> i.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
            else -> null
        }?.toString() ?: return
        i.action = null
        save(text)
    }

    // ---- list adapter --------------------------------------------------------------------------

    private inner class ClipAdapter : BaseAdapter() {
        private var items: List<Clip> = emptyList()

        fun setClips(clips: List<Clip>) {
            items = clips
            notifyDataSetChanged()
        }

        fun indexOf(id: Long) = items.indexOfFirst { it.id == id }

        override fun getCount() = items.size
        override fun getItem(position: Int) = items[position]
        override fun getItemId(position: Int) = items[position].id
        override fun hasStableIds() = true

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val row = (convertView as? ClipRow) ?: ClipRow(parent.context)
            val clip = items[position]
            row.bind(clip, clip.id == selectedId)
            return row
        }
    }

    private companion object {
        const val PROMPT = "Choose a clip to copy."
        const val STATE_SELECTED = "selected"
    }
}
