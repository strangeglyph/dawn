import AttractorSim.Companion.RADIUS
import AttractorSim.Companion.VIEWPORT_RADIUS
import org.w3c.dom.CanvasRenderingContext2D
import kotlin.math.*

const val GRAVITATION = 6.674e-11

class PathSegment(val x: Double, val y: Double, val age: Double)

class Particle(x: Double, y: Double, speedX: Double, speedY: Double, val state: AttractorSim) {
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

    fun update(fullDeltaT: Double) {
        accumulatedTime += fullDeltaT

        while (accumulatedTime >= 0) {
            // Simulate one step into the future. We will interpolate during rendering
            previousPhysicsStepX = x
            previousPhysicsStepY = y

            fixedUpdate(PHYSICS_STEP)
            accumulatedTime -= PHYSICS_STEP
        }
    }

    fun fixedUpdate(deltaT: Double) {
        paths.removeAll { segment -> segment.age < age - PATH_MAX_AGE }

        if (age != lastAge && !decaying) {
            paths.add(PathSegment(x, y, age))

            val distAccel = dist * angleROC * angleROC - (GRAVITATION * state.weight) / (dist * dist)
            val angleAccel = -2.0 * distROC * angleROC / dist

            val nextDist = 2 * dist - lastDist + distAccel * deltaT * deltaT
            val nextAngle = 2 * angle - lastAngle + angleAccel * deltaT * deltaT

            lastDist = dist
            lastAngle = angle

            dist = nextDist
            angle = nextAngle
        }

        lastAge = age
        age += deltaT

        if (sqrt(x * x + y * y) > 3 * RADIUS) {
            decaying = true
        }

        if (sqrt(x * x + y * y) <= state.size) {
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
            drawSecantLine(ctx, state, fromX, fromY, to.x, to.y)
            fromX = to.x
            fromY = to.y

            i++
        }

        // Interpolate final position between last physics step and the step simulated into the future
        val alpha = (accumulatedTime + PHYSICS_STEP) / PHYSICS_STEP
        val actualX = x * alpha + previousPhysicsStepX * (1 - alpha)
        val actualY = y * alpha + previousPhysicsStepY * (1 - alpha)
        drawSecantLine(ctx, state, fromX, fromY, actualX, actualY)
        ctx.beginPath()
        // ctx.ellipse(x + AttractorSim.RADIUS, y + AttractorSim.RADIUS, 2.0, 2.0, 0.0, 0.0, 2 * PI)
        ctx.fill()
        ctx.stroke()
    }
}

class IntersectionResult(val x: Double, val y: Double, val relativePosition: Double)

sealed class LineCircleIntersection {
    object NoIntersection : LineCircleIntersection()
    class Tangent(val point: IntersectionResult) : LineCircleIntersection()
    class Secant(val first: IntersectionResult, val second: IntersectionResult) : LineCircleIntersection()
}

fun getLineCircleIntersection(fromX: Double, fromY: Double, toX: Double, toY: Double, radius: Double): LineCircleIntersection {
    val dx = toX - fromX
    val dy = toY - fromY
    val distSq = dx * dx + dy * dy
    if (distSq == 0.0) return LineCircleIntersection.NoIntersection

    val determinant = fromX * toY - toX * fromY
    val radiusSq = radius * radius
    val discriminant = radiusSq * distSq - determinant * determinant
    if (discriminant < 0) {
        // No intersection or tangent only
        return LineCircleIntersection.NoIntersection
    }

    val discriminantRoot = sqrt(discriminant)
    val signDy = if (dy < 0) -1.0 else 1.0

    var x1 = (determinant * dy + signDy * dx * discriminantRoot) / distSq
    var y1 = (-determinant * dx + abs(dy) * discriminantRoot) / distSq
    var posOnLineP1 = (x1 - fromX) / dx

    if (discriminant == 0.0) {
        return LineCircleIntersection.Tangent(IntersectionResult(x1, y1, posOnLineP1))
    }

    var x2 = (determinant * dy - signDy * dx * discriminantRoot) / distSq
    var y2 = (-determinant * dx - abs(dy) * discriminantRoot) / distSq
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

    return LineCircleIntersection.Secant(IntersectionResult(x1, y1, posOnLineP1), IntersectionResult(x2, y2, posOnLineP2))
}

fun drawSecantLine(ctx: CanvasRenderingContext2D, state: AttractorSim, fromX: Double, fromY: Double, toX: Double, toY: Double) {
    // First check for intersection with the viewport
    // Then check for intersection with central attracktor

    val viewportIntersect = getLineCircleIntersection(fromX, fromY, toX, toY, VIEWPORT_RADIUS)
            as? LineCircleIntersection.Secant
            ?: return

    if (viewportIntersect.second.relativePosition < 0) {
        // Line segment outside of circle, pointing away
        return
    }

    if (viewportIntersect.first.relativePosition > 1) {
        // Line segment outside of circle, pointing towards
        return
    }

    val fromX = if (viewportIntersect.first.relativePosition >= 0) {
        viewportIntersect.first.x
    } else {
        fromX
    }

    val fromY = if (viewportIntersect.first.relativePosition >= 0) {
        viewportIntersect.first.y
    } else {
        fromY
    }

    val toX = if (viewportIntersect.second.relativePosition <= 1) {
        viewportIntersect.second.x
    } else {
        toX
    }

    val toY = if (viewportIntersect.second.relativePosition <= 1) {
        viewportIntersect.second.y
    } else {
        toY
    }


    val attractorIntersection = getLineCircleIntersection(fromX, fromY, toX, toY, state.size)

    // Assume that intersections with an attractor only happens with the towards the end of a line
    ctx.beginPath()
    ctx.moveTo(fromX + RADIUS, fromY + RADIUS)


    when (attractorIntersection) {
        is LineCircleIntersection.NoIntersection -> {
            ctx.lineTo(toX + RADIUS, toY + RADIUS)
        }

        is LineCircleIntersection.Tangent -> {
            if (attractorIntersection.point.relativePosition in 0.0..1.0) {
                ctx.lineTo(attractorIntersection.point.x + RADIUS, attractorIntersection.point.y + RADIUS)
            } else {
                ctx.lineTo(toX + RADIUS, toY + RADIUS)
            }
        }

        is LineCircleIntersection.Secant -> {
            if (attractorIntersection.first.relativePosition in 0.0..1.0) {
                ctx.lineTo(attractorIntersection.first.x + RADIUS, attractorIntersection.first.y + RADIUS)
            } else {
                ctx.lineTo(toX + RADIUS, toY + RADIUS)
            }
        }
    }

    ctx.stroke()
}