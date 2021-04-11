
import kotlinx.html.*
import kotlinx.html.dom.create
import kotlinx.html.js.div
import kotlinx.html.js.style
import org.w3c.dom.HTMLProgressElement
import kotlin.browser.document
import kotlin.math.pow

object Inside: Tab("inside", "Inside", 10) {

    enum class HeatLevel(val threshold: Double, val color: Color) {
        FREEZING(0.0, Color(0x00, 0x13, 0xbf)),
        COLD(10.0, Color(0x0e, 0x87, 0xc4)),
        COOL(100.0, Color(0x22, 0xdd, 0xe3)),
        WARM(1.0e3, Color(0xe8, 0xe4, 0x09)),
        COZY(1.0e4, Color(0xe8, 0xa5, 0x09)),
        HOT(1.0e5, Color(0xd1, 0x2d, 0x1b)),
        LIMIT(1.0e6, Color(0xd1, 0x2d, 0x1b)),
    }

    private var heat: HeatLevel = HeatLevel.FREEZING

    private var nextTickId = 0

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

    fun enableHeat() {
        heatDiv.append(heatIndicator)
        MAIN_DIV.append(heatDiv)
        MAIN_DIV.append(heatStyle)
    }

    private fun heatLevel(): HeatLevel {
        return heat;
    }


    private fun heatTick(deltaMillis: Int) {
        val deltaSeconds = deltaMillis / 1000.0

        heat *= HEAT_RETAIN_PCT_PER_SEC.pow(deltaSeconds)
        println("Heat decay tick")

        val currentStage = heatLevel()
        val nextStage = Inside.HeatLevel.values()[currentStage.ordinal + 1]
        val color = currentStage.color.gradientTo(nextStage.color, heatStageProgress())

        heatIndicator.value = heatStageProgress()
        heatStyle.innerHTML = heatRawStyle(color)
    }

    fun increaseHeat(amount: Double) {
        val interpolation = SmootherStep(250.0, amount)
        val id = "inside.heat.increase.$nextTickId"
        nextTickId += 1

        TickThread.register(id) { deltaMs ->
            val increment = interpolation.increment(deltaMs.toDouble())
            heat += increment

            if (interpolation.isFinished()) {
                TickThread.unregister(id)
            }
        }
    }
}