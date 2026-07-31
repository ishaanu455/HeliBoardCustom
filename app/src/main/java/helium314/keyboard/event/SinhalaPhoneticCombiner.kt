package helium314.keyboard.event

import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode
import helium314.keyboard.latin.common.Constants
import java.util.ArrayList

class SinhalaPhoneticCombiner : Combiner {
    private val composingText = StringBuilder()
    
    private fun convert(englishText: String): String {
        var result = englishText
        
        val multiConsonants = mapOf(
            "kh" to "ඛ", "gh" to "ඝ", "dh" to "ද", "Dh" to "ඪ", 
            "th" to "ත", "Th" to "ඨ", "ph" to "ඵ", "bh" to "භ",
            "ch" to "ච", "jh" to "ඣ", "sh" to "ෂ", "Sh" to "ශ", "gn" to "ඥ",
            "nd" to "ඳ", "mb" to "ඹ", "ng" to "ඟ", "ny" to "ඤ"
        )
        for ((k, v) in multiConsonants) {
            result = result.replace(k, v)
        }
        
        val singleConsonants = mapOf(
            "k" to "ක", "g" to "ග", "t" to "ට", "T" to "ට",
            "d" to "ඩ", "D" to "ඩ", "n" to "න", "N" to "ණ",
            "p" to "ප", "b" to "බ", "m" to "ම", "y" to "ය",
            "r" to "ර", "l" to "ල", "L" to "ළ", "w" to "ව", 
            "v" to "ව", "s" to "ස", "S" to "ෂ", "h" to "හ", 
            "f" to "ෆ", "c" to "ච", "j" to "ජ", "x" to "ං",
            "q" to "ද", "X" to "ඃ", "z" to "‍"
        )
        for ((k, v) in singleConsonants) {
            result = result.replace(k, v)
        }
        
        val output = java.lang.StringBuilder()
        var i = 0
        while (i < result.length) {
            val c = result[i]
            
            if (c in '\u0D9A'..'\u0DC6' || c == '\u0D82' || c == '\u0D83') {
                output.append(c)
                var nextVowel = ""
                if (i + 1 < result.length && isEnglishVowel(result[i+1])) {
                    nextVowel += result[i+1]
                    if (i + 2 < result.length && isEnglishVowel(result[i+2])) {
                        val twoLetter = nextVowel + result[i+2]
                        if (twoLetter == "aa" || twoLetter == "ee" || twoLetter == "uu" || twoLetter == "ii" || twoLetter == "oo" || twoLetter == "ai" || twoLetter == "au" || twoLetter == "ou" || twoLetter == "Aa" || twoLetter == "AA" || twoLetter == "Ru") {
                            nextVowel = twoLetter
                        }
                    }
                }
                
                if (nextVowel.isNotEmpty()) {
                    output.append(getVowelSign(nextVowel))
                    i += nextVowel.length
                } else {
                    if (c != '\u0D82' && c != '\u0D83') {
                        output.append('\u0DCA')
                    }
                }
            } else if (isEnglishVowel(c)) {
                var nextVowel = c.toString()
                if (i + 1 < result.length && isEnglishVowel(result[i+1])) {
                    val twoLetter = nextVowel + result[i+1]
                    if (twoLetter == "aa" || twoLetter == "ee" || twoLetter == "uu" || twoLetter == "ii" || twoLetter == "oo" || twoLetter == "ai" || twoLetter == "au" || twoLetter == "ou" || twoLetter == "Aa" || twoLetter == "AA" || twoLetter == "Ru") {
                        nextVowel = twoLetter
                    }
                }
                output.append(getIndependentVowel(nextVowel))
                i += nextVowel.length - 1
            } else {
                output.append(c)
            }
            i++
        }
        return output.toString()
    }
    
    private fun isEnglishVowel(c: Char): Boolean {
        return c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u' ||
               c == 'A' || c == 'E' || c == 'I' || c == 'O' || c == 'U' || c == 'R'
    }
    
    private fun getVowelSign(v: String): String {
        return when (v) {
            "a" -> ""
            "aa" -> "ා"
            "A" -> "ැ"
            "Aa", "AA" -> "ෑ"
            "i" -> "ි"
            "ii", "I" -> "ී"
            "u" -> "ු"
            "uu", "U" -> "ූ"
            "e" -> "ෙ"
            "ee", "E" -> "ේ"
            "ai" -> "ෛ"
            "o" -> "ො"
            "oo", "O" -> "ෝ"
            "au", "ou" -> "ෞ"
            "R" -> "ෘ"
            "Ru" -> "ෟ"
            else -> ""
        }
    }
    
    private fun getIndependentVowel(v: String): String {
        return when (v) {
            "a" -> "අ"
            "aa" -> "ආ"
            "A" -> "ඇ"
            "Aa", "AA" -> "ඈ"
            "i" -> "ඉ"
            "ii", "I" -> "ඊ"
            "u" -> "උ"
            "uu", "U" -> "ඌ"
            "e" -> "එ"
            "ee", "E" -> "ඒ"
            "ai" -> "ඓ"
            "o" -> "ඔ"
            "oo", "O" -> "ඕ"
            "au", "ou" -> "ඖ"
            "R" -> "ඍ"
            "Ru" -> "ඎ"
            else -> v
        }
    }

    override fun processEvent(previousEvents: ArrayList<Event>?, event: Event): Event {
        val codePoint = event.codePoint

        if (event.keyCode == KeyCode.SHIFT) return event

        if (event.keyCode == KeyCode.DELETE) {
            if (composingText.isNotEmpty()) {
                composingText.delete(composingText.length - 1, composingText.length)
                if (composingText.isEmpty()) {
                    reset()
                    return Event.createHardwareKeypressEvent(0x20, Constants.CODE_SPACE, 0, event, event.isKeyRepeat)
                }
                return Event.createConsumedEvent(event)
            }
            return event
        }

        val isValidCodePoint = codePoint != Integer.MAX_VALUE && Character.isValidCodePoint(codePoint)
        val isWhitespace = isValidCodePoint && Character.isWhitespace(codePoint)

        if (event.isFunctionalKeyEvent || isWhitespace) {
            if (composingText.isNotEmpty()) {
                return commitAndReset(event)
            }
            return event
        }

        if (!isValidCodePoint) return Event.createConsumedEvent(event)

        val c = codePoint.toChar()
        if ((c in 'a'..'z') || (c in 'A'..'Z')) {
            composingText.append(c)
            return Event.createConsumedEvent(event)
        } else {
            if (composingText.isNotEmpty()) {
                return commitAndReset(event)
            }
        }

        return event
    }

    override val combiningStateFeedback: CharSequence
        get() = convert(composingText.toString())

    override fun reset() {
        composingText.setLength(0)
    }

    private fun commitAndReset(event: Event): Event {
        val converted = combiningStateFeedback
        reset()
        return Event.createSoftwareTextEvent(converted, KeyCode.MULTIPLE_CODE_POINTS, event)
    }
}
