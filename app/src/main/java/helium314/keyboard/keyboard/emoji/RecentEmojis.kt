// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji

import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.edit
import androidx.core.view.isGone
import helium314.keyboard.event.HapticEvent
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode
import helium314.keyboard.latin.AudioAndHapticFeedbackManager
import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.common.StringUtils
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.Log
import helium314.keyboard.latin.utils.dpToPx
import helium314.keyboard.latin.utils.prefs
import kotlinx.serialization.json.Json

// manages recently used emojis
object RecentEmojis {
    private val prefs = Settings.getCurrentContext().prefs()

    fun set(emojis: List<String>) {
        Log.i("test", "set $emojis")
        prefs.edit { putString(Settings.PREF_RECENT_EMOJIS, Json.encodeToString(emojis)) }
    }

    @JvmStatic
    fun add(emoji: String) {
        Log.i("test", "add $emoji")
        if (emoji.isEmpty()) return
        val recents = get()
        recents.removeAll { it == emoji }
        recents.add(0, emoji)
        set(recents.take(MAX_COUNT))
    }

     @JvmStatic
     fun addCodepoint(emoji: Int) {
         if (emoji > 0) add(StringUtils.newSingleCodePointString(emoji))
     }

    fun get(): MutableList<String> {
        val pref = prefs.getString(Settings.PREF_RECENT_EMOJIS, Defaults.PREF_RECENT_EMOJIS)
        if (pref.isNullOrEmpty()) return mutableListOf()
        return runCatching { Json.decodeFromString<MutableList<String>>(pref) }
            .getOrNull() ?: mutableListOf()
    }

    private const val MAX_COUNT = 39 // max for config_emoji_keyboard_max_recents_key_count

    /**
     * Fills [rowScrollView]/[container] with the most recently used emojis, as a persistent row
     * (like the number row: always visible above the main keyboard, independent of suggestions).
     * Hides [rowScrollView] when the feature is disabled or there is no recent emoji, shows it
     * (and repopulates it) otherwise. Tapping an emoji inserts it and refreshes the row so the
     * order reflects the latest use.
     */
    fun updateRow(latinIME: LatinIME, rowScrollView: View?, container: LinearLayout?) {
        if (rowScrollView == null || container == null) return
        if (!Settings.getValues().mShowRecentEmojiRow) {
            rowScrollView.isGone = true
            return
        }
        val recents = get()
        if (recents.isEmpty()) {
            rowScrollView.isGone = true
            return
        }

        container.removeAllViews()
        val sidePadding = 10.dpToPx(latinIME.resources)
        val outValue = TypedValue()
        latinIME.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)

        recents.forEach { emoji ->
            val textView = TextView(latinIME)
            textView.text = emoji
            textView.textSize = 22f
            textView.gravity = Gravity.CENTER
            textView.setPadding(sidePadding, 0, sidePadding, 0)
            textView.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT
            )
            textView.isClickable = true
            textView.isFocusable = true
            if (outValue.resourceId != 0) textView.setBackgroundResource(outValue.resourceId)
            textView.setOnClickListener {
                AudioAndHapticFeedbackManager.getInstance()
                    .performHapticAndAudioFeedback(KeyCode.NOT_SPECIFIED, it, HapticEvent.KEY_PRESS)
                latinIME.onTextInput(emoji)
                add(emoji) // move to front, so order reflects most recent use
                updateRow(latinIME, rowScrollView, container) // refresh order immediately
            }
            container.addView(textView)
        }
        rowScrollView.isGone = false
    }
}
