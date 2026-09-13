package app.snake

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.snake.core.GameId
import app.snake.games.boxes.BoxesGame
import app.snake.games.breakout.BreakoutGame
import app.snake.games.catcher.CatchGame
import app.snake.games.checkers.CheckersGame
import app.snake.games.chess.ChessGame
import app.snake.games.dodge.DodgeGame
import app.snake.games.echo.EchoGame
import app.snake.games.flit.FlitGame
import app.snake.games.flip.FlipGame
import app.snake.games.four.FourGame
import app.snake.games.hop.HopGame
import app.snake.games.invaders.InvadersGame
import app.snake.games.memory.MemoryGame
import app.snake.games.merge.MergeGame
import app.snake.games.mines.MinesGame
import app.snake.games.peck.PeckGame
import app.snake.games.pong.PongGame
import app.snake.games.rocks.RocksGame
import app.snake.games.slide.SlideGame
import app.snake.games.snake.SnakeGame
import app.snake.games.stacks.StacksGame
import app.snake.menu.ArcadeMenu
import app.snake.scores.LeaderboardScreen

@Composable
fun ArcadeApp(arcade: ArcadeViewModel) {
    val route by arcade.route.collectAsStateWithLifecycle()
    BackHandler(enabled = route != Route.Menu) { arcade.back() }
    SnakeTheme {
        when (val current = route) {
            Route.Menu -> ArcadeMenu(arcade)
            Route.Scores -> LeaderboardScreen(arcade)
            is Route.Play -> when (current.game) {
                GameId.Snake -> SnakeGame(arcade)
                GameId.Mines -> MinesGame(arcade)
                GameId.Breakout -> BreakoutGame(arcade)
                GameId.Stacks -> StacksGame(arcade)
                GameId.Merge -> MergeGame(arcade)
                GameId.Pong -> PongGame(arcade)
                GameId.Invaders -> InvadersGame(arcade)
                GameId.Flit -> FlitGame(arcade)
                GameId.Memory -> MemoryGame(arcade)
                GameId.Dodge -> DodgeGame(arcade)
                GameId.Checkers -> CheckersGame(arcade)
                GameId.Chess -> ChessGame(arcade)
                GameId.Four -> FourGame(arcade)
                GameId.Flip -> FlipGame(arcade)
                GameId.Hop -> HopGame(arcade)
                GameId.Echo -> EchoGame(arcade)
                GameId.Rocks -> RocksGame(arcade)
                GameId.Peck -> PeckGame(arcade)
                GameId.Catch -> CatchGame(arcade)
                GameId.Slide -> SlideGame(arcade)
                GameId.Boxes -> BoxesGame(arcade)
            }
        }
    }
}
