package app.snake.core

enum class Dir(val dx: Int, val dy: Int) {
    Up(0, -1),
    Down(0, 1),
    Left(-1, 0),
    Right(1, 0);

    fun isOpposite(other: Dir): Boolean =
        dx + other.dx == 0 && dy + other.dy == 0
}
