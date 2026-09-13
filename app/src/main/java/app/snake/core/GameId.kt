package app.snake.core

enum class GameId(
    val title: String,
    val blurb: String,
    val boardKey: String,
    val lowerBetter: Boolean = false,
    val hotSeat: Boolean = false,
    val seatChoice: Boolean = false,
) {
    Snake("SNAKE", "Eat. Don't crash.", "snake", seatChoice = true),
    Mines("MINES", "Classic sweep.", "mines", lowerBetter = true),
    Breakout("BREAKOUT", "Keep the ball alive.", "breakout"),
    Stacks("STACKS", "Clear the lines.", "stacks"),
    Merge("2048", "Slide and combine.", "merge"),
    Pong("PONG", "Bounce it back.", "pong", seatChoice = true),
    Invaders("INVADERS", "Shoot the sky.", "invaders"),
    Flit("FLIT", "Tap to stay up.", "flit"),
    Memory("MEMORY", "Match the pairs.", "memory", lowerBetter = true, seatChoice = true),
    Dodge("DODGE", "Don't get hit.", "dodge"),
    Checkers("CHECKERS", "Hot seat. Jump them all.", "checkers", hotSeat = true),
    Chess("CHESS", "Hot seat. Pass the phone.", "chess", hotSeat = true),
    Four("FOUR", "Hot seat. Four in a row.", "four", hotSeat = true),
    Flip("FLIP", "Hot seat. Reversi.", "flip", hotSeat = true),
    Hop("HOP", "Cross the garden.", "hop"),
    Echo("ECHO", "Repeat the lights.", "echo"),
    Rocks("ROCKS", "Split the stones.", "rocks"),
    Peck("PECK", "Whack the sprouts.", "peck"),
    Catch("CATCH", "Catch the berries.", "catch"),
    Slide("SLIDE", "Slide into order.", "slide", lowerBetter = true),
    Boxes("BOXES", "Hot seat. Close the squares.", "boxes", hotSeat = true),
    ;

    fun scoreKeys(): List<String> = when (this) {
        Mines -> listOf("mines_beginner", "mines_intermediate", "mines_expert")
        else -> listOf(boardKey)
    }

    fun seatKey(): String = "${boardKey}_seat"
}
