
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.get
import kotlin.browser.document
import kotlin.browser.window
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class AttractorSim {
    companion object {
        const val DIAMETER = 600
        const val RADIUS = this.DIAMETER / 2.0
        const val VIEWPORT_RADIUS = RADIUS - 1

        fun initalizeCanvas(): HTMLCanvasElement {
            val canvas = document.getElementsByClassName("attractor")[0] as HTMLCanvasElement
            val context = canvas.getContext("2d") as CanvasRenderingContext2D
            context.canvas.width = this.DIAMETER
            context.canvas.height = this.DIAMETER

            return canvas
        }
    }

    init {
        val stepButton = document.getElementsByClassName("step")[0] as HTMLButtonElement
        stepButton.onclick = { if (pause) { update(deltaT = 1.0); draw() } }
        val spawnButton = document.getElementsByClassName("spawn")[0] as HTMLButtonElement
        spawnButton.onclick = { spawn(); draw() }
        val clearButton = document.getElementsByClassName("clear")[0] as HTMLButtonElement
        clearButton.onclick = { clear(); draw() }
        val playButton = document.getElementsByClassName("play")[0] as HTMLButtonElement
        playButton.onclick = { play() }
        val pauseButton = document.getElementsByClassName("pause")[0] as HTMLButtonElement
        pauseButton.onclick = { pause() }
    }

    private val canvas: HTMLCanvasElement = initalizeCanvas()
    private val context: CanvasRenderingContext2D = canvas.getContext("2d") as CanvasRenderingContext2D
    private val particles: MutableList<Particle> = ArrayList()
    private var time = 0.0
    private var lastUpdate: Double? = null

    private var pause = true

    fun play() {
        if (pause) {
            pause = false
            lastUpdate = null // Act like no time passed
            window.requestAnimationFrame { time -> loop(time) }
        }
    }

    fun pause() {
        pause = true
    }

    private fun renderAttractor() {
        context.save()

        context.strokeStyle = "rgb(0, 0, 0)"
        context.fillStyle = context.strokeStyle
        context.shadowColor = "rgb(255, 255, 255)"
        context.shadowBlur = 16.0
        context.beginPath()
        context.ellipse(RADIUS, RADIUS, 32.0, 32.0, 0.0, 0.0, 2 * PI)
        context.fill()
        context.stroke()

        context.restore()
    }

    private fun renderViewport() {
        context.save()

        context.strokeStyle = "rgb(170, 170, 170)"
        context.fillStyle = context.strokeStyle
        context.lineWidth = 0.0
        context.beginPath()
        context.ellipse(RADIUS, RADIUS, RADIUS, RADIUS, 0.0, 0.0, 2 * PI)
        context.fill()
        context.stroke()

        context.strokeStyle = "rgb(${0.3 * 255}, ${0.3 * 255}, ${0.3 * 255})"
        context.lineWidth = 2.0
        context.beginPath()
        context.ellipse(RADIUS, RADIUS, VIEWPORT_RADIUS, VIEWPORT_RADIUS, 0.0, 0.0, 2 * PI)
        context.stroke()

        context.restore()
    }

    fun draw() {
        context.strokeStyle = "rgb(0, 0, 0)"
        context.clearRect(0.0, 0.0, DIAMETER.toDouble(), DIAMETER.toDouble())

        renderViewport()
        renderAttractor()

        context.save()
        particles.forEach { p -> p.render(context) }
        context.restore()
    }

    fun update(deltaT: Double) {
        particles.forEach { p -> p.update(deltaT) }
        spawn()
    }

    fun clear() {
        particles.clear()
    }

    fun loop(now: Double) {
        if (lastUpdate == null) {
            lastUpdate = now
        }
        val deltaT = (now - lastUpdate!!) / 1000
        lastUpdate = now
        time += deltaT

        if (deltaT > 0.0) {
            update(deltaT)
            draw()
        }

        if (!pause) {
            window.requestAnimationFrame { time -> loop(time) }
        }
    }

    fun spawn() {
        val spawnAngle = Random.nextDouble(2 * PI)
        val spawnX = sin(spawnAngle) * VIEWPORT_RADIUS
        val spawnY = cos(spawnAngle) * VIEWPORT_RADIUS

        // Take a movement vector straight towards the center and angle it somewhat to the side (rotation around starting position)
        val initialMoveAngle = Random.nextDouble(0.05 * PI, 0.25 * PI)
        val moveX = (-spawnX) * cos(initialMoveAngle) - (-spawnY) * sin(initialMoveAngle)
        val moveY = (-spawnX) * sin(initialMoveAngle) + (-spawnY) * cos(initialMoveAngle)

        particles.add(Particle(spawnX, spawnY, spawnX - moveX * 0.1, spawnY - moveY * 0.1))
    }
}