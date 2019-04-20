object Story {

    fun beginStory() {
        Outside.addInteraction("force_open", "Force the door open") {
            Messages.append("You gather the strongest members of your group around you and try to find an angle to push the door aside. " +
                    "Unsucessfully, until someone remembers that your brought a small amount of plastic explosives with you. " +
                    "A loud bang and the happy shrieking of the children in your group and the locking mechanism of the gate is no more.")
            Resources.add(Resource.APPROACH_DIRECT, 1)
            Resources.remove(Resource.EXPLOSIVES, 1)
            gainEntry()
        }
        Outside.addInteraction("go_around", "Find a different way in") {
            Messages.append(
                    "After searching for a while, a small girl in your entourage manages to find a ventilation shaft hidden by ice and snow. " +
                    "She crawls inside and after a tense few minutes, an audible \"click\" signifies that the gate has been unlocked.")
            Resources.add(Resource.APPROACH_INDIRECT, 1)
            gainEntry()
        }
    }

    private fun gainEntry() {
        Outside.removeInteraction("force_open")
        Outside.removeInteraction("go_around")

        AttractorTab.enable()
        Outside.enable()
        Outside.setDescription("Your torches shed light on a small area outside the base, maybe fifty strides across. In it, you recognize the all-too-familiar monotonicity of the icy wasteland.")
        AttractorTab.setDescription("The accelerator lies before you: The silent remnant of a long-gone past.")

        LocationIndicator.show()
        LocationIndicator.set(Outside.displayName)
    }
}