package app.pictoclip

import android.content.ClipData
import android.content.ClipboardManager
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Smoke test: the app starts, and the paste / copy / delete flow works. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainActivityTest {

    private fun View.find(match: (View) -> Boolean): View? {
        if (match(this)) return this
        if (this is ViewGroup) for (i in 0 until childCount) getChildAt(i).find(match)?.let { return it }
        return null
    }

    @Test
    fun startsPastesCopiesAndDeletes() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        val root = activity.window.decorView
        val list = root.find { it is ListView } as ListView
        assertEquals(0, list.adapter.count)

        val cm = activity.getSystemService(ClipboardManager::class.java)
        cm.setPrimaryClip(ClipData.newPlainText("test", "hello world"))
        val paste = root.find { it is DsButton && it.text == "Paste" }
        assertNotNull(paste)
        paste!!.performClick()
        assertEquals(1, list.adapter.count)

        // Build a row the way the list does, and tap it: that copies it back.
        val row = list.adapter.getView(0, null, list)
        row.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        cm.setPrimaryClip(ClipData.newPlainText("test", "something else"))
        list.performItemClick(row, 0, list.adapter.getItemId(0))
        assertEquals("hello world", cm.primaryClip!!.getItemAt(0).text.toString())

        val delete = root.find { it is DsButton && it.text == "Delete" }!!
        delete.performClick()
        assertEquals(0, list.adapter.count)
    }

    @Test
    fun storeDedupesAndCaps() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        val store = ClipStore(activity)
        store.clear()
        store.add("a"); store.add("b"); store.add("a")
        assertEquals(listOf("a", "b"), store.all().map { it.text })
        assertEquals(null, store.add("   "))
    }
}
