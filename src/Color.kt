class Color(val red: Int, val green: Int, val blue: Int) {
    fun toHtmlString(): String {
        val redString = if (red < 16) "0${red.toString(16)}" else red.toString(16)
        val greenString = if (green < 16) "0${green.toString(16)}" else green.toString(16)
        val blueString = if (blue < 16) "0${blue.toString(16)}" else blue.toString(16)

        return "#$redString$greenString$blueString"
    }

    fun gradientTo(otherColor: Color, progress: Double): Color {
        if (progress > 1) {
            return otherColor
        }
        val interRed = red * (1 - progress) + otherColor.red * progress
        val interGreen = green * (1 - progress) + otherColor.green * progress
        val interBlue = blue * (1 - progress) + otherColor.blue * progress
        return Color(interRed.toInt(), interGreen.toInt(), interBlue.toInt())
    }
}