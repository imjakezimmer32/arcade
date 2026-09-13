package app.snake.games.chess

enum class Side {
    White, Black;

    val other: Side get() = if (this == White) Black else White
}

enum class Kind(val value: Int) {
    Pawn(1), Knight(3), Bishop(3), Rook(5), Queen(9), King(0)
}

data class Piece(val side: Side, val kind: Kind)

data class ChessMove(
    val from: Int,
    val to: Int,
    val promo: Kind? = null,
    val castle: Boolean = false,
    val ep: Boolean = false,
)

enum class ChessPhase { Setup, Playing, Promote, Over }

enum class ChessEnd { Mate, Stale, Draw }

data class ChessSnap(
    val squares: List<Piece?>,
    val turn: Side,
    val wk: Boolean,
    val wq: Boolean,
    val bk: Boolean,
    val bq: Boolean,
    val ep: Int,
    val half: Int,
    val full: Int,
)

data class ChessState(
    val squares: List<Piece?>,
    val turn: Side,
    val wk: Boolean,
    val wq: Boolean,
    val bk: Boolean,
    val bq: Boolean,
    val ep: Int,
    val half: Int,
    val full: Int,
    val selected: Int? = null,
    val targets: List<ChessMove> = emptyList(),
    val last: ChessMove? = null,
    val phase: ChessPhase = ChessPhase.Setup,
    val end: ChessEnd? = null,
    val promoFrom: Int = -1,
    val promoTo: Int = -1,
    val whiteName: String = "WHITE",
    val blackName: String = "BLACK",
    val history: List<ChessSnap> = emptyList(),
    val passPending: Boolean = false,
    val recorded: Boolean = false,
) {
    val inCheck: Boolean get() = kingAttacked(squares, turn)

    fun name(side: Side) = if (side == Side.White) whiteName else blackName

    fun material(side: Side): Int = squares.fold(0) { acc, p ->
        acc + if (p?.side == side) p.kind.value else 0
    }

    companion object {
        fun lobby() = start("WHITE", "BLACK").copy(phase = ChessPhase.Setup)

        fun start(white: String, black: String): ChessState {
            val sq = MutableList<Piece?>(64) { null }
            fun back(rank: Int, side: Side) {
                val kinds = listOf(Kind.Rook, Kind.Knight, Kind.Bishop, Kind.Queen, Kind.King, Kind.Bishop, Kind.Knight, Kind.Rook)
                kinds.forEachIndexed { file, kind -> sq[idx(file, rank)] = Piece(side, kind) }
            }
            back(0, Side.White)
            back(7, Side.Black)
            for (file in 0..7) {
                sq[idx(file, 1)] = Piece(Side.White, Kind.Pawn)
                sq[idx(file, 6)] = Piece(Side.Black, Kind.Pawn)
            }
            return ChessState(
                squares = sq,
                turn = Side.White,
                wk = true, wq = true, bk = true, bq = true,
                ep = -1, half = 0, full = 1,
                phase = ChessPhase.Playing,
                whiteName = white, blackName = black,
            )
        }
    }
}

fun idx(file: Int, rank: Int) = rank * 8 + file
fun fileOf(i: Int) = i % 8
fun rankOf(i: Int) = i / 8

fun ChessState.sit(white: String, black: String) = ChessState.start(white, black)

fun ChessState.ackPass() = copy(passPending = false)

fun ChessState.undo(): ChessState {
    val snap = history.lastOrNull() ?: return this
    return copy(
        squares = snap.squares,
        turn = snap.turn,
        wk = snap.wk, wq = snap.wq, bk = snap.bk, bq = snap.bq,
        ep = snap.ep, half = snap.half, full = snap.full,
        selected = null, targets = emptyList(), last = null,
        phase = ChessPhase.Playing, end = null,
        promoFrom = -1, promoTo = -1,
        history = history.dropLast(1),
        passPending = false,
        recorded = false,
    )
}

fun ChessState.tap(square: Int): ChessState {
    if (phase != ChessPhase.Playing || passPending) return this
    val piece = squares.getOrNull(square)
    if (selected != null) {
        val hits = targets.filter { it.to == square }
        if (hits.isNotEmpty()) {
            val promos = hits.filter { it.promo != null }
            return if (promos.size > 1) {
                copy(phase = ChessPhase.Promote, promoFrom = hits.first().from, promoTo = square, selected = null, targets = emptyList())
            } else {
                commit(hits.first())
            }
        }
        if (piece?.side == turn) return select(square)
        return copy(selected = null, targets = emptyList())
    }
    return if (piece?.side == turn) select(square) else this
}

fun ChessState.pickPromo(kind: Kind): ChessState {
    if (phase != ChessPhase.Promote) return this
    return commit(ChessMove(promoFrom, promoTo, promo = kind))
}

fun ChessState.markRecorded() = copy(recorded = true)

private fun ChessState.select(square: Int): ChessState {
    val moves = legalFrom(square)
    return copy(selected = square, targets = moves)
}

private fun ChessState.snapshot() = ChessSnap(squares, turn, wk, wq, bk, bq, ep, half, full)

private fun ChessState.commit(move: ChessMove): ChessState {
    val applied = applyUnchecked(move)
    val nextTurn = turn.other
    val legal = applied.copy(turn = nextTurn).legalAll()
    val check = kingAttacked(applied.squares, nextTurn)
    val end = when {
        legal.isEmpty() && check -> ChessEnd.Mate
        legal.isEmpty() -> ChessEnd.Stale
        insufficient(applied.squares) -> ChessEnd.Draw
        applied.half >= 100 -> ChessEnd.Draw
        else -> null
    }
    return applied.copy(
        turn = nextTurn,
        selected = null,
        targets = emptyList(),
        last = move,
        phase = if (end != null) ChessPhase.Over else ChessPhase.Playing,
        end = end,
        promoFrom = -1,
        promoTo = -1,
        history = history + snapshot(),
        passPending = end == null,
        full = if (turn == Side.Black) full + 1 else full,
    )
}

private fun insufficient(squares: List<Piece?>): Boolean {
    val pieces = squares.filterNotNull()
    if (pieces.size == 2) return true
    if (pieces.size == 3) {
        val extra = pieces.firstOrNull { it.kind != Kind.King } ?: return true
        return extra.kind == Kind.Knight || extra.kind == Kind.Bishop
    }
    return false
}

private fun ChessState.applyUnchecked(move: ChessMove): ChessState {
    val next = squares.toMutableList()
    val moving = next[move.from] ?: return this
    var nextWk = wk
    var nextWq = wq
    var nextBk = bk
    var nextBq = bq
    var nextEp = -1
    val captured = when {
        move.ep -> {
            val cap = idx(fileOf(move.to), rankOf(move.from))
            next[cap] = null
            true
        }
        next[move.to] != null -> true
        else -> false
    }
    next[move.to] = if (move.promo != null) Piece(moving.side, move.promo) else moving
    next[move.from] = null
    if (move.castle) {
        when (move.to) {
            6 -> { next[5] = next[7]; next[7] = null }
            2 -> { next[3] = next[0]; next[0] = null }
            62 -> { next[61] = next[63]; next[63] = null }
            58 -> { next[59] = next[56]; next[56] = null }
        }
    }
    if (moving.kind == Kind.King) {
        if (moving.side == Side.White) { nextWk = false; nextWq = false } else { nextBk = false; nextBq = false }
    }
    if (moving.kind == Kind.Rook) {
        when (move.from) {
            0 -> nextWq = false
            7 -> nextWk = false
            56 -> nextBq = false
            63 -> nextBk = false
        }
    }
    when (move.to) {
        0 -> nextWq = false
        7 -> nextWk = false
        56 -> nextBq = false
        63 -> nextBk = false
    }
    if (moving.kind == Kind.Pawn && kotlin.math.abs(rankOf(move.to) - rankOf(move.from)) == 2) {
        nextEp = idx(fileOf(move.from), (rankOf(move.from) + rankOf(move.to)) / 2)
    }
    val resetHalf = moving.kind == Kind.Pawn || captured
    return copy(
        squares = next,
        wk = nextWk, wq = nextWq, bk = nextBk, bq = nextBq,
        ep = nextEp,
        half = if (resetHalf) 0 else half + 1,
    )
}

fun ChessState.legalAll(): List<ChessMove> =
    squares.indices.filter { squares[it]?.side == turn }.flatMap { legalFrom(it) }

fun ChessState.legalFrom(from: Int): List<ChessMove> =
    pseudo(from).filter { move ->
        val next = applyUnchecked(move)
        !kingAttacked(next.squares, turn)
    }

private fun ChessState.pseudo(from: Int): List<ChessMove> {
    val piece = squares[from] ?: return emptyList()
    if (piece.side != turn) return emptyList()
    return when (piece.kind) {
        Kind.Pawn -> pawnMoves(from, piece.side)
        Kind.Knight -> leap(from, piece.side, KNIGHT)
        Kind.Bishop -> rays(from, piece.side, BISHOP)
        Kind.Rook -> rays(from, piece.side, ROOK)
        Kind.Queen -> rays(from, piece.side, BISHOP) + rays(from, piece.side, ROOK)
        Kind.King -> kingMoves(from, piece.side)
    }
}

private fun ChessState.pawnMoves(from: Int, side: Side): List<ChessMove> {
    val dir = if (side == Side.White) 1 else -1
    val start = if (side == Side.White) 1 else 6
    val last = if (side == Side.White) 7 else 0
    val f = fileOf(from)
    val r = rankOf(from)
    val out = ArrayList<ChessMove>(6)
    fun push(to: Int) {
        if (rankOf(to) == last) {
            PROMOS.forEach { out += ChessMove(from, to, promo = it) }
        } else {
            out += ChessMove(from, to)
        }
    }
    val one = idx(f, r + dir)
    if (r + dir in 0..7 && squares[one] == null) {
        push(one)
        val two = idx(f, r + dir * 2)
        if (r == start && squares[two] == null) out += ChessMove(from, two)
    }
    for (df in intArrayOf(-1, 1)) {
        val nf = f + df
        val nr = r + dir
        if (nf !in 0..7 || nr !in 0..7) continue
        val to = idx(nf, nr)
        val hit = squares[to]
        if (hit != null && hit.side != side) push(to)
        else if (to == ep) out += ChessMove(from, to, ep = true)
    }
    return out
}

private fun ChessState.leap(from: Int, side: Side, deltas: Array<IntArray>): List<ChessMove> {
    val f = fileOf(from)
    val r = rankOf(from)
    val out = ArrayList<ChessMove>(8)
    for (d in deltas) {
        val nf = f + d[0]
        val nr = r + d[1]
        if (nf !in 0..7 || nr !in 0..7) continue
        val to = idx(nf, nr)
        val hit = squares[to]
        if (hit == null || hit.side != side) out += ChessMove(from, to)
    }
    return out
}

private fun ChessState.rays(from: Int, side: Side, deltas: Array<IntArray>): List<ChessMove> {
    val f0 = fileOf(from)
    val r0 = rankOf(from)
    val out = ArrayList<ChessMove>(16)
    for (d in deltas) {
        var f = f0 + d[0]
        var r = r0 + d[1]
        while (f in 0..7 && r in 0..7) {
            val to = idx(f, r)
            val hit = squares[to]
            if (hit == null) out += ChessMove(from, to)
            else {
                if (hit.side != side) out += ChessMove(from, to)
                break
            }
            f += d[0]
            r += d[1]
        }
    }
    return out
}

private fun ChessState.kingMoves(from: Int, side: Side): List<ChessMove> {
    val out = leap(from, side, KING).toMutableList()
    if (side == Side.White && from == 4 && !kingAttacked(squares, Side.White)) {
        if (wk && squares[5] == null && squares[6] == null &&
            !attacked(squares, 5, Side.Black) && !attacked(squares, 6, Side.Black)
        ) out += ChessMove(4, 6, castle = true)
        if (wq && squares[3] == null && squares[2] == null && squares[1] == null &&
            !attacked(squares, 3, Side.Black) && !attacked(squares, 2, Side.Black)
        ) out += ChessMove(4, 2, castle = true)
    }
    if (side == Side.Black && from == 60 && !kingAttacked(squares, Side.Black)) {
        if (bk && squares[61] == null && squares[62] == null &&
            !attacked(squares, 61, Side.White) && !attacked(squares, 62, Side.White)
        ) out += ChessMove(60, 62, castle = true)
        if (bq && squares[59] == null && squares[58] == null && squares[57] == null &&
            !attacked(squares, 59, Side.White) && !attacked(squares, 58, Side.White)
        ) out += ChessMove(60, 58, castle = true)
    }
    return out
}

fun kingSq(squares: List<Piece?>, side: Side): Int =
    squares.indices.firstOrNull { squares[it]?.side == side && squares[it]?.kind == Kind.King } ?: -1

fun kingAttacked(squares: List<Piece?>, side: Side): Boolean {
    val k = kingSq(squares, side)
    return k >= 0 && attacked(squares, k, side.other)
}

fun attacked(squares: List<Piece?>, square: Int, by: Side): Boolean {
    val f = fileOf(square)
    val r = rankOf(square)
    for (d in KNIGHT) {
        val nf = f + d[0]
        val nr = r + d[1]
        if (nf !in 0..7 || nr !in 0..7) continue
        val p = squares[idx(nf, nr)]
        if (p?.side == by && p.kind == Kind.Knight) return true
    }
    for (d in KING) {
        val nf = f + d[0]
        val nr = r + d[1]
        if (nf !in 0..7 || nr !in 0..7) continue
        val p = squares[idx(nf, nr)]
        if (p?.side == by && p.kind == Kind.King) return true
    }
    val pawnDir = if (by == Side.White) -1 else 1
    for (df in intArrayOf(-1, 1)) {
        val nf = f + df
        val nr = r + pawnDir
        if (nf !in 0..7 || nr !in 0..7) continue
        val p = squares[idx(nf, nr)]
        if (p?.side == by && p.kind == Kind.Pawn) return true
    }
    fun slides(deltas: Array<IntArray>, vararg kinds: Kind): Boolean {
        for (d in deltas) {
            var nf = f + d[0]
            var nr = r + d[1]
            while (nf in 0..7 && nr in 0..7) {
                val p = squares[idx(nf, nr)]
                if (p != null) {
                    if (p.side == by && p.kind in kinds) return true
                    break
                }
                nf += d[0]
                nr += d[1]
            }
        }
        return false
    }
    if (slides(BISHOP, Kind.Bishop, Kind.Queen)) return true
    if (slides(ROOK, Kind.Rook, Kind.Queen)) return true
    return false
}

private val KNIGHT = arrayOf(
    intArrayOf(1, 2), intArrayOf(1, -2), intArrayOf(-1, 2), intArrayOf(-1, -2),
    intArrayOf(2, 1), intArrayOf(2, -1), intArrayOf(-2, 1), intArrayOf(-2, -1),
)
private val KING = arrayOf(
    intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1),
    intArrayOf(1, 1), intArrayOf(1, -1), intArrayOf(-1, 1), intArrayOf(-1, -1),
)
private val BISHOP = arrayOf(
    intArrayOf(1, 1), intArrayOf(1, -1), intArrayOf(-1, 1), intArrayOf(-1, -1),
)
private val ROOK = arrayOf(
    intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1),
)
private val PROMOS = listOf(Kind.Queen, Kind.Rook, Kind.Bishop, Kind.Knight)
