
import AttractorSim.Companion.RADIUS
import org.w3c.dom.CanvasRenderingContext2D
import kotlin.math.abs
import kotlin.math.sqrt

class Particle(var x: Double, var y: Double, var lastX: Double, var lastY: Double) {
    companion object {
        const val PATH_MAXAGE = 2.0
    }

    fun dirX() = (x - lastX) / sqrt((x - lastX) * (x - lastX) + (y - lastY) * (y - lastY))
    fun dirY() = (y - lastY) / sqrt((x - lastX) * (x - lastX) + (y - lastY) * (y - lastY))

    var age = 0.0
    var speed = 50 // px/second

    class PathSegment(val x: Double, val y: Double, val age: Double)

    val paths: MutableList<PathSegment> = ArrayList()

    init {
        paths.add(PathSegment(x, y, 0.0))
    }


    fun update(deltaT: Double) {
        paths.add(PathSegment(x, y, age))
        while (paths[0].age < age - PATH_MAXAGE) paths.removeAt(0)

        age += deltaT

        val newX = x + dirX() * speed * deltaT
        val newY = y + dirY() * speed * deltaT
        lastX = x
        lastY = y
        x = newX
        y = newY
    }

    fun render(ctx: CanvasRenderingContext2D) {
        ctx.strokeStyle = "rgb(0, 255, 255)"
        ctx.fillStyle = ctx.strokeStyle
        ctx.lineWidth = 1.0
        //ctx.beginPath()

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

        drawSecantLine(ctx, fromX, fromY, x, y)
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


    var posOnLineP1 = (x1 - fromX)/dx
    var posOnLineP2 = (x2 - fromX)/dx

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