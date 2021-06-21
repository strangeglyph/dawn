import kotlin.math.floor

fun Double.fractionalPart(): Double {
    return this - floor(this)
}