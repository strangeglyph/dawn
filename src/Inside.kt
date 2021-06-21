import kotlinx.html.*
import kotlinx.html.dom.create
import kotlinx.html.js.div
import kotlinx.html.js.style
import org.w3c.dom.HTMLProgressElement
import kotlin.browser.document
import kotlin.math.abs
import kotlin.math.floor

object Inside : Tab("inside", "Inside", 10) {

    enum class HeatLevel(val threshold: Double, val color: Color) {
        FREEZING(0.0, Color(0x00, 0x13, 0xbf)),
        COLD(10.0, Color(0x0e, 0x87, 0xc4)),
        COOL(100.0, Color(0x22, 0xdd, 0xe3)),
        WARM(1.0e3, Color(0xe8, 0xe4, 0x09)),
        COZY(1.0e4, Color(0xe8, 0xa5, 0x09)),
        HOT(1.0e5, Color(0xd1, 0x2d, 0x1b)),
        LIMIT(1.0e6, Color(0xd1, 0x2d, 0x1b)),;

        fun next(): HeatLevel {
            return if (this.ordinal < LIMIT.ordinal) {
                values()[this.ordinal + 1]
            } else {
                LIMIT;
            }
        }
    }

    // Heat level a discrete stage
    private var heat: HeatLevel = HeatLevel.FREEZING
    // ... and including partial progress to the next stage
    private var heatAsNumber = 0.0
    // As above, but what we are currently animating towards
    private var targetHeatLevel = HeatLevel.FREEZING
    // ... and again including partial progress to the next stage
    private var targetHeatAsNumber = 0.0

    // The base of the current color interpolation
    private var currentHeatRootColor = HeatLevel.FREEZING.color
    // The target of the current color interpolation
    private var targetHeatColor = HeatLevel.FREEZING.color
    // And the current color
    private var currentHeatColor = HeatLevel.FREEZING.color

    private val TICK_ID = "inside.heat.fade"

    private val heatIndicator = document.create.progress {
        id = "inside-heat-indicator"
        classes = setOf("heat-indicator")
        value = "0"
    } as HTMLProgressElement

    private val heatDiv = document.create.div {
        classes = setOf("heat-div")
        span {
            +"Heat"
            classes = setOf("heat-label")
        }
    }

    private val heatStyle = document.create.style {
        unsafe {
            raw(heatRawStyle(HeatLevel.FREEZING.color))
        }
    }

    private fun heatRawStyle(color: Color): String {
        return """
                #inside-heat-indicator {
                    color: ${color.toHtmlString()};
                }
                #inside-heat-indicator::-moz-progress-bar {
                    background-color: ${color.toHtmlString()};
                }
                #inside-heat-indicator::-webkit-progress-bar {
                    color: ${color.toHtmlString()};
                }
                """.trimIndent()
    }

    fun onHeatChange() {
        val descr = when (heat) {
            HeatLevel.FREEZING -> {
                "The walls are iron plate and frost gives them a faint shimmer. A storage room " +
                "keeps your meager supplies. The barracks contain enough space for everyone, though " +
                "the current temperatures make sleep a dangerous proposition."
            }
            HeatLevel.COLD -> {
                "The walls are iron plate and frost gives them a faint shimmer. A storage room " +
                "keeps your meager supplies. The barracks contain enough space for everyone. It is " +
                "uncomfortably cold."
            }
            HeatLevel.COOL -> {
                "The walls are iron plate and frost gives them a faint shimmer. A storage room " +
                "keeps your meager supplies. The barracks contain enough space for everyone. A small " +
                "fire in the middle of the room does little to keep away the chill."
            }
            HeatLevel.WARM -> {
                "The walls are iron plate and frost gives them a faint shimmer. A storage room " +
                "keeps your meager supplies. The barracks contain enough space for everyone. A fire is " +
                "blazing in the middle of the room, and people crowd around it."
            }
            HeatLevel.COZY -> {
                "The walls are iron plate and frost gives them a faint shimmer. A storage room " +
                "keeps your meager supplies. The barracks contain enough space for everyone. A roaring " +
                "fire fills the room with a cozy warmth."
            }
            HeatLevel.HOT -> {
                "The walls are iron plate and frost gives them a faint shimmer. A storage room " +
                "keeps your meager supplies. The barracks contain enough space for everyone. The room is " +
                "almost unbearably hot for anyone wearing more than a t-shirt."
            }
            HeatLevel.LIMIT -> {
                "The room spontaneously combusts. Everyone dies in the fire. (You shouldn't be here)"
            }
        }
    }

    fun enableHeat() {
        heatDiv.append(heatIndicator)
        MAIN_DIV.append(heatDiv)
        MAIN_DIV.append(heatStyle)
    }

    fun fadeHeatTo(newHeatLevel: HeatLevel, partialProgress: Double) {
        val newTargetHeatNum = newHeatLevel.ordinal + partialProgress
        if (newTargetHeatNum == targetHeatAsNumber) {
            // No difference from our current (potentially finished) animation goal
            return
        }

        targetHeatLevel = newHeatLevel
        targetHeatAsNumber = newTargetHeatNum

        val interpolation = SmootherStep(1000.0, heatAsNumber, targetHeatAsNumber)

        // To ensure a smooth color gradient, take a snapshot of the current color in case
        // we are in the middle of an animation already, and use that as a base to interpolate
        // from.
        currentHeatRootColor = currentHeatColor
        // The color we are animating towards is the color between the targetHeatLevel and the next higher
        // heat level depending on the partial progress
        targetHeatColor = targetHeatLevel.color.gradientTo(targetHeatLevel.next().color, partialProgress)

        TickThread.unregister(TICK_ID)
        TickThread.register(TICK_ID) { deltaMs ->
            interpolation.increment(deltaMs.toDouble())
            heatAsNumber = interpolation.current()

            // Keep the indicator capped at 1 unless we exceed it significantly
            // because a filled bar looks better than an empty bar
            if (heatAsNumber.fractionalPart() > 0.001) {
                heatIndicator.value = heatAsNumber.fractionalPart()
            } else if (heatAsNumber <= 0.001) {
                // Unless we are basically 0, in which case we do want the bar to be empty
                heatIndicator.value = 0.0
            } else {
                heatIndicator.value = 1.0
            }

            val newHeat = HeatLevel.values()[floor(heatAsNumber).toInt()]
            if (newHeat != heat) {
                heat = newHeat
                onHeatChange()
            }
            currentHeatColor = currentHeatRootColor.gradientTo(targetHeatColor, interpolation.percentCompleted())
            heatStyle.innerHTML = heatRawStyle(currentHeatColor)

            if (interpolation.isFinished()) {
                TickThread.unregister(TICK_ID)
            }
        }
    }
}
