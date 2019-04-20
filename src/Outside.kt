object Outside: Tab("outside", "Outside", 0) {

    fun beginStory() {
        addInteraction("force_open", "Force the door open") {
            Messages.append("You forced the door open.")
            Resources.add(Resource.APPROACH_DIRECT, 1)
            gainEntry()
        }
        addInteraction("go_around", "Find a different way in") {
            Messages.append("You found a hidden way inside.")
            Resources.add(Resource.APPROACH_INDIRECT, 1)
            gainEntry()
        }
    }

    private fun gainEntry() {
        removeInteraction("force_open")
        removeInteraction("go_around")
        AttractorTab.enable()
        Outside.enable()
        LocationIndicator.show()
        Resources.add(Resource.ENERGY, 6)
        LocationIndicator.set(Outside.displayName)
    }

}