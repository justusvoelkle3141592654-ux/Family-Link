package com.familylink.slf

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    // Farben (Markenfarben der App)
    private val colorPrimary = Color.parseColor("#2E7D5B")
    private val colorPrimaryDark = Color.parseColor("#1B5E42")
    private val colorAccent = Color.parseColor("#F2A413")
    private val colorGood = Color.parseColor("#2E7D5B")
    private val colorBad = Color.parseColor("#C0392B")
    private val colorMuted = Color.parseColor("#607D8B")

    private lateinit var root: LinearLayout

    // Spielzustand
    private val selected = WordBank.categories.toMutableList()
    private var timerEnabled = true
    private var totalPlayer = 0
    private var totalComputer = 0
    private var round = 0
    private var currentLetter = 'A'
    private var countDown: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#F4F6F5"))
            isFillViewport = true
        }
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(28))
        }
        scroll.addView(root)
        setContentView(scroll)
        showSetup()
    }

    override fun onDestroy() {
        countDown?.cancel()
        super.onDestroy()
    }

    // ---------------------------------------------------------------- Setup

    private fun showSetup() {
        countDown?.cancel()
        root.removeAllViews()

        root.addView(title("Stadt-Land-Fluss"))
        root.addView(subtitle("Gegen den Computer"))
        root.addView(spacer(8))
        root.addView(
            body(
                "Für jeden Buchstaben findest du zu jeder Kategorie ein passendes " +
                    "Wort. Der Computer spielt mit!\n\n" +
                    "20 Punkte für ein Wort, das nur du hast · 10 Punkte, wenn ihr " +
                    "beide dasselbe habt · 0 Punkte für kein Wort."
            )
        )
        root.addView(spacer(16))
        root.addView(sectionLabel("Kategorien"))

        val chosen = selected.toMutableSet()
        for (cat in WordBank.categories) {
            val cb = CheckBox(this).apply {
                text = cat.name
                textSize = 17f
                isChecked = chosen.contains(cat)
                setTextColor(Color.parseColor("#1F2A26"))
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) chosen.add(cat) else chosen.remove(cat)
                }
            }
            root.addView(cb)
        }

        root.addView(spacer(8))
        val timerBox = CheckBox(this).apply {
            text = "Zeitlimit (60 Sekunden pro Runde)"
            textSize = 17f
            isChecked = timerEnabled
            setTextColor(Color.parseColor("#1F2A26"))
            setOnCheckedChangeListener { _, isChecked -> timerEnabled = isChecked }
        }
        root.addView(timerBox)

        root.addView(spacer(20))
        root.addView(primaryButton("Spiel starten") {
            if (chosen.isEmpty()) {
                toast("Bitte mindestens eine Kategorie wählen.")
                return@primaryButton
            }
            selected.clear()
            // Reihenfolge wie im WordBank beibehalten
            selected.addAll(WordBank.categories.filter { chosen.contains(it) })
            totalPlayer = 0
            totalComputer = 0
            round = 0
            startRound()
        })
    }

    // ---------------------------------------------------------------- Spielrunde

    private val inputs = mutableListOf<EditText>()

    private fun startRound() {
        countDown?.cancel()
        round++
        currentLetter = GameLogic.randomLetter()
        inputs.clear()
        root.removeAllViews()

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(TextView(this).apply {
            text = "Runde $round"
            textSize = 16f
            setTextColor(colorMuted)
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        })
        header.addView(TextView(this).apply {
            text = "Du $totalPlayer : $totalComputer PC"
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(colorPrimaryDark)
        })
        root.addView(header)
        root.addView(spacer(10))

        // Großer Buchstabe
        root.addView(letterBadge(currentLetter))
        root.addView(spacer(6))

        val timerView = TextView(this).apply {
            gravity = Gravity.CENTER
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(colorAccent)
        }
        if (timerEnabled) {
            root.addView(timerView)
            root.addView(spacer(10))
        } else {
            root.addView(spacer(6))
        }

        // Eingabefelder je Kategorie
        for (cat in selected) {
            root.addView(sectionLabel(cat.name))
            val et = EditText(this).apply {
                hint = "${cat.name} mit $currentLetter …"
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
                setSingleLine(true)
                textSize = 18f
                background = fieldBackground()
                setPadding(dp(14), dp(14), dp(14), dp(14))
            }
            inputs.add(et)
            root.addView(et)
            root.addView(spacer(12))
        }

        root.addView(spacer(8))
        root.addView(primaryButton("Fertig – auswerten") { finishRound() })

        if (timerEnabled) {
            countDown = object : CountDownTimer(60_000, 1_000) {
                override fun onTick(ms: Long) {
                    val s = ms / 1000
                    timerView.text = "⏱  $s Sekunden"
                    timerView.setTextColor(if (s <= 10) colorBad else colorAccent)
                }

                override fun onFinish() {
                    timerView.text = "⏱  Zeit abgelaufen!"
                    finishRound()
                }
            }.also { it.start() }
        }
    }

    private fun finishRound() {
        countDown?.cancel()

        val results = mutableListOf<Pair<Category, CategoryResult>>()
        for (i in selected.indices) {
            val cat = selected[i]
            val playerText = inputs[i].text.toString()
            val computerWord = WordBank.pick(cat, currentLetter)
            results.add(cat to GameLogic.scoreCategory(playerText, computerWord, currentLetter))
        }

        val roundPlayer = results.sumOf { it.second.playerPoints }
        val roundComputer = results.sumOf { it.second.computerPoints }
        totalPlayer += roundPlayer
        totalComputer += roundComputer

        showResults(results, roundPlayer, roundComputer)
    }

    // ---------------------------------------------------------------- Ergebnis

    private fun showResults(
        results: List<Pair<Category, CategoryResult>>,
        roundPlayer: Int,
        roundComputer: Int
    ) {
        root.removeAllViews()
        root.addView(title("Auswertung"))
        root.addView(subtitle("Buchstabe: $currentLetter"))
        root.addView(spacer(14))

        for ((cat, res) in results) {
            root.addView(resultCard(cat, res))
            root.addView(spacer(12))
        }

        root.addView(spacer(4))
        root.addView(
            scoreLine("Diese Runde", roundPlayer, roundComputer, big = false)
        )
        root.addView(spacer(4))
        root.addView(
            scoreLine("Gesamt", totalPlayer, totalComputer, big = true)
        )
        root.addView(spacer(20))

        root.addView(primaryButton("Nächste Runde") { startRound() })
        root.addView(spacer(10))
        root.addView(secondaryButton("Spiel beenden") { showGameOver() })
    }

    private fun showGameOver() {
        root.removeAllViews()
        root.addView(title("Spielende"))
        root.addView(spacer(10))

        val verdict = when {
            totalPlayer > totalComputer -> "🏆  Du gewinnst!"
            totalPlayer < totalComputer -> "🤖  Der Computer gewinnt!"
            else -> "🤝  Unentschieden!"
        }
        root.addView(TextView(this).apply {
            text = verdict
            textSize = 26f
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(colorPrimaryDark)
        })
        root.addView(spacer(16))
        root.addView(scoreLine("Endstand", totalPlayer, totalComputer, big = true))
        root.addView(spacer(6))
        root.addView(body("Gespielte Runden: $round"))
        root.addView(spacer(24))
        root.addView(primaryButton("Neues Spiel") { showSetup() })
    }

    // ---------------------------------------------------------------- View-Bausteine

    private fun resultCard(cat: Category, res: CategoryResult): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground()
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        val head = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        head.addView(TextView(this).apply {
            text = cat.name
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(colorPrimaryDark)
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        })
        val badgeText = when {
            !res.playerValid -> "0 Punkte"
            res.duplicate -> "10 Punkte"
            else -> "20 Punkte"
        }
        head.addView(pointsBadge(badgeText, res.playerPoints))
        card.addView(head)
        card.addView(spacer(8))

        val playerLine = if (res.playerAnswer.isBlank()) {
            "Du: —  (kein Wort)"
        } else if (!res.playerValid) {
            "Du: ${res.playerAnswer}  (ungültig)"
        } else {
            "Du: ${res.playerAnswer}"
        }
        card.addView(TextView(this).apply {
            text = playerLine
            textSize = 16f
            setTextColor(if (res.playerValid) Color.parseColor("#1F2A26") else colorBad)
        })
        card.addView(TextView(this).apply {
            text = "Computer: " + (res.computerAnswer ?: "—")
            textSize = 16f
            setTextColor(colorMuted)
        })
        return card
    }

    private fun scoreLine(label: String, p: Int, c: Int, big: Boolean): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = cardBackground()
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        row.addView(TextView(this).apply {
            text = label
            textSize = if (big) 19f else 16f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(colorPrimaryDark)
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        })
        row.addView(TextView(this).apply {
            text = "$p : $c"
            textSize = if (big) 22f else 18f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(if (p >= c) colorGood else colorBad)
        })
        return row
    }

    private fun pointsBadge(text: String, points: Int): View {
        return TextView(this).apply {
            this.text = text
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(dp(10), dp(4), dp(10), dp(4))
            background = pillBackground(
                when {
                    points >= 20 -> colorGood
                    points >= 10 -> colorAccent
                    else -> colorMuted
                }
            )
        }
    }

    private fun letterBadge(letter: Char): View {
        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        val tv = TextView(this).apply {
            text = letter.toString()
            textSize = 56f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            val size = dp(96)
            layoutParams = LinearLayout.LayoutParams(size, size)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(colorPrimary)
            }
        }
        wrap.addView(tv)
        return wrap
    }

    private fun title(text: String) = TextView(this).apply {
        this.text = text
        textSize = 30f
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(colorPrimaryDark)
    }

    private fun subtitle(text: String) = TextView(this).apply {
        this.text = text
        textSize = 17f
        setTextColor(colorAccent)
        setTypeface(typeface, Typeface.BOLD)
    }

    private fun sectionLabel(text: String) = TextView(this).apply {
        this.text = text
        textSize = 14f
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(colorMuted)
        setPadding(0, dp(6), 0, dp(4))
    }

    private fun body(text: String) = TextView(this).apply {
        this.text = text
        textSize = 15f
        setTextColor(Color.parseColor("#3A4640"))
    }

    private fun primaryButton(text: String, onClick: () -> Unit) = Button(this).apply {
        this.text = text
        textSize = 18f
        isAllCaps = false
        setTextColor(Color.WHITE)
        setTypeface(typeface, Typeface.BOLD)
        background = pillBackground(colorPrimary)
        setPadding(dp(16), dp(16), dp(16), dp(16))
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        setOnClickListener { onClick() }
    }

    private fun secondaryButton(text: String, onClick: () -> Unit) = Button(this).apply {
        this.text = text
        textSize = 17f
        isAllCaps = false
        setTextColor(colorPrimaryDark)
        background = outlineBackground(colorPrimary)
        setPadding(dp(16), dp(14), dp(16), dp(14))
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        setOnClickListener { onClick() }
    }

    // ---------------------------------------------------------------- Hilfsfunktionen

    private fun spacer(heightDp: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(heightDp))
    }

    private fun cardBackground() = GradientDrawable().apply {
        setColor(Color.WHITE)
        cornerRadius = dp(14).toFloat()
        setStroke(dp(1), Color.parseColor("#E1E7E4"))
    }

    private fun fieldBackground() = GradientDrawable().apply {
        setColor(Color.WHITE)
        cornerRadius = dp(12).toFloat()
        setStroke(dp(2), Color.parseColor("#CBD5D0"))
    }

    private fun pillBackground(color: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(28).toFloat()
    }

    private fun outlineBackground(color: Int) = GradientDrawable().apply {
        setColor(Color.TRANSPARENT)
        cornerRadius = dp(28).toFloat()
        setStroke(dp(2), color)
    }

    private fun toast(msg: String) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun dp(value: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics
    ).toInt()
}
