import AttractorSim.Companion.RADIUS
import org.w3c.dom.CanvasRenderingContext2D
import kotlin.math.*

const val GRAVITATION = 6.674e-11

class PathSegment(val x: Double, val y: Double, val age: Double)

class Particle(x: Double, y: Double, speedX: Double, speedY: Double) {
    companion object {
        const val MAX_AGE = 50
        const val PATH_MAX_AGE = 5.0
        const val PHYSICS_STEP = 0.01
    }

    private var age = 0.0
    private var lastAge = -1.0

    private var accumulatedTime = 0.0


    private var dist = sqrt(x * x + y * y)
    private var angle = atan2(y, x)

    private var lastDist = 0.0
    private var lastAngle = 0.0

    private val distROC
        get() = (dist - lastDist) * (age - lastAge)
    private val angleROC
        get() = (angle - lastAngle) * (age - lastAge)

    private val x
        get() = dist * cos(angle)
    private val y
        get() = dist * sin(angle)

    private var previousPhysicsStepX = lastDist * cos(lastAngle)
    private var previousPhysicsStepY = lastDist * sin(lastAngle)


    var dead = false
        private set

    private var decaying = false


    private val paths: MutableList<PathSegment> = ArrayList()

    init {
        val lastX = x - speedX * PHYSICS_STEP
        val lastY = y - speedY * PHYSICS_STEP
        lastDist = sqrt(lastX * lastX + lastY * lastY)
        lastAngle = atan2(lastY, lastX)
    }

    fun update(fullDeltaT: Double, state: AttractorSim) {
        accumulatedTime += fullDeltaT

        while (accumulatedTime >= 0) {
            // Simulate one step into the future. We will interpolate during rendering
            // println("Physics step - ${accumulatedTime / PHYSICS_STEP + 1} steps remaining")
            previousPhysicsStepX = x
            previousPhysicsStepY = y

            fixedUpdate(PHYSICS_STEP, state)
            accumulatedTime -= PHYSICS_STEP
        }
    }

    fun fixedUpdate(deltaT: Double, state: AttractorSim) {
        paths.removeAll { segment -> segment.age < age - PATH_MAX_AGE }

        if (age != lastAge && !decaying) {
            paths.add(PathSegment(x, y, age))

            // println("r(n-1)=$lastDist, t(n-1)=$lastAngle")
            // println("r( n )=$dist, t( n )=$angle")
            // println("v_r(n)=$distROC, v_t(n)=$angleROC")

            val distAccel = dist * angleROC * angleROC - (GRAVITATION * state.weight) / (dist * dist)
            val angleAccel = -2.0 * distROC * angleROC / dist
            // println("a_r(n)=$distAccel, a_t(n)=$angleAccel")

            val nextDist = 2 * dist - lastDist + distAccel * deltaT * deltaT
            val nextAngle = 2 * angle - lastAngle + angleAccel * deltaT * deltaT
            // println("r(n+1)=$nextDist, t(n+1)=$nextAngle")

            lastDist = dist
            lastAngle = angle

            dist = nextDist
            angle = nextAngle
        }

        lastAge = age
        age += deltaT

        if (sqrt(x * x + y * y) > 2 * RADIUS) {
            decaying = true
        }

        if (sqrt(x * x + y * y) < state.size) {
            decaying = true
        }

        if (age > MAX_AGE) {
            decaying = true
        }

        if (decaying && paths.isEmpty()) {
            dead = true
        }
    }

    fun render(ctx: CanvasRenderingContext2D) {
        ctx.strokeStyle = "rgb(0, 255, 255)"
        ctx.fillStyle = ctx.strokeStyle
        ctx.lineWidth = 1.0
        //ctx.beginPath()

        if (paths.isEmpty()) {
            return
        }

        val first = paths[0]
        var fromX = first.x
        var fromY = first.y

        var i = 1
        while (i < paths.size) {
            val to = paths[i]
            drawSecantLine(ctx, fromX, fromY, to.x, to.y)
            fromX = to.x
            fromY = to.y

            i++
        }

        // Interpolate final position between last physics step and the step simulated into the future
        val alpha = (accumulatedTime + PHYSICS_STEP) / PHYSICS_STEP
        val actualX = x * alpha + previousPhysicsStepX * (1 - alpha)
        val actualY = y * alpha + previousPhysicsStepY * (1 - alpha)
        drawSecantLine(ctx, fromX, fromY, actualX, actualY)
        ctx.beginPath()
        // ctx.ellipse(x + AttractorSim.RADIUS, y + AttractorSim.RADIUS, 2.0, 2.0, 0.0, 0.0, 2 * PI)
        ctx.fill()
        ctx.stroke()
    }
}

fun drawSecantLine(ctx: CanvasRenderingContext2D, fromX: Double, fromY: Double, toX: Double, toY: Double) {
    val dx = toX - fromX
    val dy = toY - fromY
    val distSq = dx * dx + dy * dy
    if (distSq == 0.0) return

    val determinant = fromX * toY - toX * fromY

    val radius = AttractorSim.VIEWPORT_RADIUS
    val radiusSq = radius * radius
    val discriminant = radiusSq * distSq - determinant * determinant
    if (discriminant <= 0) {
        // No intersection or tangent only
        return
    }

    val discriminantRoot = sqrt(discriminant)
    val signDy = if (dy < 0) -1.0 else 1.0
    //                         v
    var x1 = (determinant * dy + signDy * dx * discriminantRoot) / distSq
    var x2 = (determinant * dy - signDy * dx * discriminantRoot) / distSq

    //                          v
    var y1 = (-determinant * dx + abs(dy) * discriminantRoot) / distSq
    var y2 = (-determinant * dx - abs(dy) * discriminantRoot) / distSq


    var posOnLineP1 = (x1 - fromX) / dx
    var posOnLineP2 = (x2 - fromX) / dx

    if (posOnLineP1 > posOnLineP2) {
        // Make sure that p1 is closer to from than p2
        var tmp = x1
        x1 = x2
        x2 = tmp

        tmp = y1
        y1 = y2
        y2 = tmp

        tmp = posOnLineP1
        posOnLineP1 = posOnLineP2
        posOnLineP2 = tmp
    }

    /*
    ctx.save()
    ctx.beginPath()

    ctx.strokeStyle = "rgb(255, 0, 0)"
    ctx.moveTo(RADIUS, RADIUS)
    ctx.lineTo(x1 + RADIUS, y1 + RADIUS)
    ctx.moveTo(RADIUS, RADIUS)
    ctx.lineTo(x2 + RADIUS, y2 + RADIUS)

    ctx.moveTo(fromX + RADIUS + 2, fromY + RADIUS + 2)
    ctx.lineTo(toX + RADIUS + 2, toY + RADIUS + 2)
    ctx.stroke()
    ctx.restore()
    */


    if (posOnLineP2 < 0) {
        // Line segment outside of circle, pointing away
        return
    }

    if (posOnLineP1 > 1) {
        // Line segment outside of circle, pointing towards
        return
    }

    ctx.beginPath()
    if (posOnLineP1 >= 0) {
        ctx.moveTo(x1 + RADIUS, y1 + RADIUS)
    } else {
        ctx.moveTo(fromX + RADIUS, fromY + RADIUS)
    }

    if (posOnLineP2 <= 1) {
        ctx.lineTo(x2 + RADIUS, y2 + RADIUS)
    } else {
        ctx.lineTo(toX + RADIUS, toY + RADIUS)
    }
    ctx.stroke()
}