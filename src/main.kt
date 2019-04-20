fun main() {
    val attractor = AttractorSim()
    attractor.draw()

    Messages.append("You arrive at the iron gate: Heavy, forbidding, inset into the mountain face that fades into the " +
            "darkness of the Forever Night. The other members of your party gather around you, shivering in the cold as " +
            "you debate how to proceed.")
    Story.beginStory()
}

