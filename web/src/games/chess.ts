import { el, GameHandle, hud, leafButton } from "../core/ui";
import { scores } from "../core/scores";
import { clearOverlays, hotSeatLobby, passOverlay, seatResult } from "./types";

type Side = "white" | "black";
type Kind = "pawn" | "knight" | "bishop" | "rook" | "queen" | "king";
type Phase = "setup" | "playing" | "promote" | "over";
type ChessEnd = "mate" | "stale" | "draw";

type Piece = { side: Side; kind: Kind };
type ChessMove = {
  from: number;
  to: number;
  promo?: Kind;
  castle?: boolean;
  ep?: boolean;
};

type State = {
  squares: (Piece | null)[];
  turn: Side;
  wk: boolean;
  wq: boolean;
  bk: boolean;
  bq: boolean;
  ep: number;
  half: number;
  full: number;
  selected: number | null;
  targets: ChessMove[];
  last: ChessMove | null;
  phase: Phase;
  end: ChessEnd | null;
  promoFrom: number;
  promoTo: number;
  whiteName: string;
  blackName: string;
  passPending: boolean;
  recorded: boolean;
};

const KIND_VALUE: Record<Kind, number> = {
  pawn: 1,
  knight: 3,
  bishop: 3,
  rook: 5,
  queen: 9,
  king: 0,
};

const KNIGHT: [number, number][] = [
  [1, 2],
  [1, -2],
  [-1, 2],
  [-1, -2],
  [2, 1],
  [2, -1],
  [-2, 1],
  [-2, -1],
];
const KING: [number, number][] = [
  [1, 0],
  [-1, 0],
  [0, 1],
  [0, -1],
  [1, 1],
  [1, -1],
  [-1, 1],
  [-1, -1],
];
const BISHOP: [number, number][] = [
  [1, 1],
  [1, -1],
  [-1, 1],
  [-1, -1],
];
const ROOK: [number, number][] = [
  [1, 0],
  [-1, 0],
  [0, 1],
  [0, -1],
];
const PROMOS: Kind[] = ["queen", "rook", "bishop", "knight"];

const GLYPH: Record<Side, Record<Kind, string>> = {
  white: { king: "♔", queen: "♕", rook: "♖", bishop: "♗", knight: "♘", pawn: "♙" },
  black: { king: "♚", queen: "♛", rook: "♜", bishop: "♝", knight: "♞", pawn: "♟" },
};

function other(side: Side): Side {
  return side === "white" ? "black" : "white";
}
function idx(file: number, rank: number) {
  return rank * 8 + file;
}
function fileOf(i: number) {
  return i % 8;
}
function rankOf(i: number) {
  return (i / 8) | 0;
}

function startSquares(): (Piece | null)[] {
  const sq: (Piece | null)[] = Array(64).fill(null);
  const back: Kind[] = ["rook", "knight", "bishop", "queen", "king", "bishop", "knight", "rook"];
  for (let file = 0; file < 8; file++) {
    sq[idx(file, 0)] = { side: "white", kind: back[file]! };
    sq[idx(file, 7)] = { side: "black", kind: back[file]! };
    sq[idx(file, 1)] = { side: "white", kind: "pawn" };
    sq[idx(file, 6)] = { side: "black", kind: "pawn" };
  }
  return sq;
}

function lobby(): State {
  return {
    squares: startSquares(),
    turn: "white",
    wk: true,
    wq: true,
    bk: true,
    bq: true,
    ep: -1,
    half: 0,
    full: 1,
    selected: null,
    targets: [],
    last: null,
    phase: "setup",
    end: null,
    promoFrom: -1,
    promoTo: -1,
    whiteName: "WHITE",
    blackName: "BLACK",
    passPending: false,
    recorded: false,
  };
}

function start(whiteName: string, blackName: string): State {
  return { ...lobby(), phase: "playing", whiteName, blackName };
}

function nameOf(state: State, side: Side) {
  return side === "white" ? state.whiteName : state.blackName;
}

function material(squares: (Piece | null)[], side: Side): number {
  return squares.reduce((acc, p) => acc + (p?.side === side ? KIND_VALUE[p.kind] : 0), 0);
}

function kingSq(squares: (Piece | null)[], side: Side): number {
  return squares.findIndex((p) => p?.side === side && p.kind === "king");
}

function attacked(squares: (Piece | null)[], square: number, by: Side): boolean {
  const f = fileOf(square);
  const r = rankOf(square);
  for (const [df, dr] of KNIGHT) {
    const nf = f + df!;
    const nr = r + dr!;
    if (nf < 0 || nf > 7 || nr < 0 || nr > 7) continue;
    const p = squares[idx(nf, nr)];
    if (p?.side === by && p.kind === "knight") return true;
  }
  for (const [df, dr] of KING) {
    const nf = f + df!;
    const nr = r + dr!;
    if (nf < 0 || nf > 7 || nr < 0 || nr > 7) continue;
    const p = squares[idx(nf, nr)];
    if (p?.side === by && p.kind === "king") return true;
  }
  const pawnDir = by === "white" ? -1 : 1;
  for (const df of [-1, 1]) {
    const nf = f + df;
    const nr = r + pawnDir;
    if (nf < 0 || nf > 7 || nr < 0 || nr > 7) continue;
    const p = squares[idx(nf, nr)];
    if (p?.side === by && p.kind === "pawn") return true;
  }
  const slides = (deltas: [number, number][], kinds: Kind[]) => {
    for (const [df, dr] of deltas) {
      let nf = f + df!;
      let nr = r + dr!;
      while (nf >= 0 && nf <= 7 && nr >= 0 && nr <= 7) {
        const p = squares[idx(nf, nr)];
        if (p) {
          if (p.side === by && kinds.includes(p.kind)) return true;
          break;
        }
        nf += df!;
        nr += dr!;
      }
    }
    return false;
  };
  if (slides(BISHOP, ["bishop", "queen"])) return true;
  if (slides(ROOK, ["rook", "queen"])) return true;
  return false;
}

function kingAttacked(squares: (Piece | null)[], side: Side): boolean {
  const k = kingSq(squares, side);
  return k >= 0 && attacked(squares, k, other(side));
}

function applyUnchecked(state: State, move: ChessMove): State {
  const next = state.squares.slice();
  const moving = next[move.from];
  if (!moving) return state;
  let nextWk = state.wk;
  let nextWq = state.wq;
  let nextBk = state.bk;
  let nextBq = state.bq;
  let nextEp = -1;
  let captured = false;
  if (move.ep) {
    const cap = idx(fileOf(move.to), rankOf(move.from));
    next[cap] = null;
    captured = true;
  } else if (next[move.to] != null) {
    captured = true;
  }
  next[move.to] = move.promo ? { side: moving.side, kind: move.promo } : moving;
  next[move.from] = null;
  if (move.castle) {
    if (move.to === 6) {
      next[5] = next[7];
      next[7] = null;
    } else if (move.to === 2) {
      next[3] = next[0];
      next[0] = null;
    } else if (move.to === 62) {
      next[61] = next[63];
      next[63] = null;
    } else if (move.to === 58) {
      next[59] = next[56];
      next[56] = null;
    }
  }
  if (moving.kind === "king") {
    if (moving.side === "white") {
      nextWk = false;
      nextWq = false;
    } else {
      nextBk = false;
      nextBq = false;
    }
  }
  if (moving.kind === "rook") {
    if (move.from === 0) nextWq = false;
    if (move.from === 7) nextWk = false;
    if (move.from === 56) nextBq = false;
    if (move.from === 63) nextBk = false;
  }
  if (move.to === 0) nextWq = false;
  if (move.to === 7) nextWk = false;
  if (move.to === 56) nextBq = false;
  if (move.to === 63) nextBk = false;
  if (moving.kind === "pawn" && Math.abs(rankOf(move.to) - rankOf(move.from)) === 2) {
    nextEp = idx(fileOf(move.from), ((rankOf(move.from) + rankOf(move.to)) / 2) | 0);
  }
  const resetHalf = moving.kind === "pawn" || captured;
  return {
    ...state,
    squares: next,
    wk: nextWk,
    wq: nextWq,
    bk: nextBk,
    bq: nextBq,
    ep: nextEp,
    half: resetHalf ? 0 : state.half + 1,
  };
}

function leap(state: State, from: number, side: Side, deltas: [number, number][]): ChessMove[] {
  const f = fileOf(from);
  const r = rankOf(from);
  const out: ChessMove[] = [];
  for (const [df, dr] of deltas) {
    const nf = f + df!;
    const nr = r + dr!;
    if (nf < 0 || nf > 7 || nr < 0 || nr > 7) continue;
    const to = idx(nf, nr);
    const hit = state.squares[to];
    if (!hit || hit.side !== side) out.push({ from, to });
  }
  return out;
}

function rays(state: State, from: number, side: Side, deltas: [number, number][]): ChessMove[] {
  const f0 = fileOf(from);
  const r0 = rankOf(from);
  const out: ChessMove[] = [];
  for (const [df, dr] of deltas) {
    let f = f0 + df!;
    let r = r0 + dr!;
    while (f >= 0 && f <= 7 && r >= 0 && r <= 7) {
      const to = idx(f, r);
      const hit = state.squares[to];
      if (!hit) out.push({ from, to });
      else {
        if (hit.side !== side) out.push({ from, to });
        break;
      }
      f += df!;
      r += dr!;
    }
  }
  return out;
}

function pawnMoves(state: State, from: number, side: Side): ChessMove[] {
  const dir = side === "white" ? 1 : -1;
  const startRank = side === "white" ? 1 : 6;
  const last = side === "white" ? 7 : 0;
  const f = fileOf(from);
  const r = rankOf(from);
  const out: ChessMove[] = [];
  const push = (to: number) => {
    if (rankOf(to) === last) {
      for (const promo of PROMOS) out.push({ from, to, promo });
    } else out.push({ from, to });
  };
  const one = idx(f, r + dir);
  if (r + dir >= 0 && r + dir <= 7 && state.squares[one] == null) {
    push(one);
    const two = idx(f, r + dir * 2);
    if (r === startRank && state.squares[two] == null) out.push({ from, to: two });
  }
  for (const df of [-1, 1]) {
    const nf = f + df;
    const nr = r + dir;
    if (nf < 0 || nf > 7 || nr < 0 || nr > 7) continue;
    const to = idx(nf, nr);
    const hit = state.squares[to];
    if (hit && hit.side !== side) push(to);
    else if (to === state.ep) out.push({ from, to, ep: true });
  }
  return out;
}

function kingMoves(state: State, from: number, side: Side): ChessMove[] {
  const out = leap(state, from, side, KING);
  if (side === "white" && from === 4 && !kingAttacked(state.squares, "white")) {
    if (
      state.wk &&
      state.squares[5] == null &&
      state.squares[6] == null &&
      !attacked(state.squares, 5, "black") &&
      !attacked(state.squares, 6, "black")
    ) {
      out.push({ from: 4, to: 6, castle: true });
    }
    if (
      state.wq &&
      state.squares[3] == null &&
      state.squares[2] == null &&
      state.squares[1] == null &&
      !attacked(state.squares, 3, "black") &&
      !attacked(state.squares, 2, "black")
    ) {
      out.push({ from: 4, to: 2, castle: true });
    }
  }
  if (side === "black" && from === 60 && !kingAttacked(state.squares, "black")) {
    if (
      state.bk &&
      state.squares[61] == null &&
      state.squares[62] == null &&
      !attacked(state.squares, 61, "white") &&
      !attacked(state.squares, 62, "white")
    ) {
      out.push({ from: 60, to: 62, castle: true });
    }
    if (
      state.bq &&
      state.squares[59] == null &&
      state.squares[58] == null &&
      state.squares[57] == null &&
      !attacked(state.squares, 59, "white") &&
      !attacked(state.squares, 58, "white")
    ) {
      out.push({ from: 60, to: 58, castle: true });
    }
  }
  return out;
}

function pseudo(state: State, from: number): ChessMove[] {
  const piece = state.squares[from];
  if (!piece || piece.side !== state.turn) return [];
  switch (piece.kind) {
    case "pawn":
      return pawnMoves(state, from, piece.side);
    case "knight":
      return leap(state, from, piece.side, KNIGHT);
    case "bishop":
      return rays(state, from, piece.side, BISHOP);
    case "rook":
      return rays(state, from, piece.side, ROOK);
    case "queen":
      return [...rays(state, from, piece.side, BISHOP), ...rays(state, from, piece.side, ROOK)];
    case "king":
      return kingMoves(state, from, piece.side);
  }
}

function legalFrom(state: State, from: number): ChessMove[] {
  return pseudo(state, from).filter((move) => {
    const next = applyUnchecked(state, move);
    return !kingAttacked(next.squares, state.turn);
  });
}

function legalAll(state: State): ChessMove[] {
  const out: ChessMove[] = [];
  for (let i = 0; i < 64; i++) {
    if (state.squares[i]?.side === state.turn) out.push(...legalFrom(state, i));
  }
  return out;
}

function insufficient(squares: (Piece | null)[]): boolean {
  const pieces = squares.filter((p): p is Piece => p != null);
  if (pieces.length === 2) return true;
  if (pieces.length === 3) {
    const extra = pieces.find((p) => p.kind !== "king");
    return !extra || extra.kind === "knight" || extra.kind === "bishop";
  }
  return false;
}

function commit(state: State, move: ChessMove): State {
  const applied = applyUnchecked(state, move);
  const nextTurn = other(state.turn);
  const withTurn = { ...applied, turn: nextTurn };
  const legal = legalAll(withTurn);
  const check = kingAttacked(applied.squares, nextTurn);
  let end: ChessEnd | null = null;
  if (legal.length === 0 && check) end = "mate";
  else if (legal.length === 0) end = "stale";
  else if (insufficient(applied.squares)) end = "draw";
  else if (applied.half >= 100) end = "draw";
  return {
    ...applied,
    turn: nextTurn,
    selected: null,
    targets: [],
    last: move,
    phase: end ? "over" : "playing",
    end,
    promoFrom: -1,
    promoTo: -1,
    passPending: end == null,
    full: state.turn === "black" ? state.full + 1 : state.full,
  };
}

function select(state: State, square: number): State {
  return { ...state, selected: square, targets: legalFrom(state, square) };
}

function tap(state: State, square: number): State {
  if (state.phase !== "playing" || state.passPending) return state;
  const piece = state.squares[square] ?? null;
  if (state.selected != null) {
    const hits = state.targets.filter((m) => m.to === square);
    if (hits.length) {
      const promos = hits.filter((m) => m.promo != null);
      if (promos.length > 1) {
        return {
          ...state,
          phase: "promote",
          promoFrom: hits[0]!.from,
          promoTo: square,
          selected: null,
          targets: [],
        };
      }
      return commit(state, hits[0]!);
    }
    if (piece?.side === state.turn) return select(state, square);
    return { ...state, selected: null, targets: [] };
  }
  return piece?.side === state.turn ? select(state, square) : state;
}

function pickPromo(state: State, kind: Kind): State {
  if (state.phase !== "promote") return state;
  return commit(state, { from: state.promoFrom, to: state.promoTo, promo: kind });
}

function logicalFromDisplay(display: number, flipped: boolean): number {
  const col = display % 8;
  const row = (display / 8) | 0;
  const file = flipped ? 7 - col : col;
  const rank = flipped ? row : 7 - row;
  return idx(file, rank);
}

function endLine(state: State): string {
  if (state.end === "mate") return `${nameOf(state, other(state.turn))} mates`;
  if (state.end === "stale") return "Stalemate";
  if (state.end === "draw") return "Draw";
  return "Over";
}

function promoOverlay(pit: HTMLElement, onPick: (kind: Kind) => void) {
  clearOverlays(pit);
  const box = el("div", "overlay");
  box.append(el("h2", undefined, "PROMOTE"));
  const actions = el("div", "actions");
  for (const kind of PROMOS) {
    const btn = leafButton(GLYPH.white[kind], () => onPick(kind));
    btn.style.fontSize = "1.4rem";
    actions.append(btn);
  }
  box.append(actions);
  pit.append(box);
}

export function mountChess(root: HTMLElement, onBack: () => void): GameHandle {
  let state = lobby();

  const wrap = el("div", "game-root");
  const hudHost = el("div");
  const pit = el("div", "pit");
  const grid = el("div", "cell-grid");
  grid.style.gridTemplateColumns = "repeat(8, 1fr)";
  grid.style.padding = "8px";
  grid.style.aspectRatio = "1";
  grid.style.margin = "auto";
  grid.style.gap = "0";
  grid.style.maxWidth = "100%";
  const nodes = Array.from({ length: 64 }, (_, display) => {
    const c = el("div", "cell");
    c.style.borderRadius = "0";
    c.style.border = "none";
    c.addEventListener("click", () => {
      const flipped = state.turn === "black";
      const square = logicalFromDisplay(display, flipped);
      state = tap(state, square);
      render();
    });
    grid.append(c);
    return c;
  });
  pit.append(grid);
  const controls = el("div", "controls");
  controls.append(
    leafButton(
      "NEW",
      () => {
        state = lobby();
        render();
      },
      true,
    ),
  );
  const tip = el("p", "pit-hint", "White at the bottom after you sit. Board flips each turn.");
  tip.style.position = "static";
  tip.style.marginTop = "8px";
  wrap.append(hudHost, pit, controls, tip);
  root.replaceChildren(wrap);

  const maybeRecord = () => {
    if (state.phase !== "over" || state.recorded || !state.end) return;
    state = { ...state, recorded: true };
    if (state.end === "mate") {
      const winner = other(state.turn);
      const pts = Math.max(1, material(state.squares, winner));
      scores.submit("chess", nameOf(state, winner), pts, true);
    } else {
      scores.submit("chess", state.whiteName, 0, false);
      scores.submit("chess", state.blackName, 0, false);
    }
  };

  const render = () => {
    const inCheck = kingAttacked(state.squares, state.turn);
    const subtitle =
      state.phase === "setup"
        ? "hot seat"
        : state.phase === "over"
          ? endLine(state)
          : state.phase === "promote"
            ? "promote"
            : inCheck
              ? `${nameOf(state, state.turn)}  ·  check`
              : `${nameOf(state, state.turn)} to move`;
    hudHost.replaceChildren(
      hud({
        title: "CHESS",
        subtitle,
        value: state.phase === "setup" ? "2P" : String(state.full),
        valueLabel: state.phase === "setup" ? "hot seat" : "move",
        onBack,
      }),
    );

    const flipped = state.turn === "black";
    const targetSet = new Set(state.targets.map((m) => m.to));
    const king = kingSq(state.squares, state.turn);

    nodes.forEach((node, display) => {
      const square = logicalFromDisplay(display, flipped);
      const file = fileOf(square);
      const rank = rankOf(square);
      const light = (file + rank) % 2 === 1;
      const piece = state.squares[square];
      const selected = state.selected === square;
      const target = targetSet.has(square);
      const lastHit = state.last?.from === square || state.last?.to === square;
      const check = inCheck && square === king;
      node.replaceChildren();
      node.style.outline = "none";
      node.style.background = check
        ? "rgba(255,90,90,0.55)"
        : selected
          ? "rgba(61,220,132,0.45)"
          : lastHit
            ? "rgba(200,255,122,0.18)"
            : light
              ? "var(--pit-alt)"
              : "var(--pad)";
      if (piece) {
        const g = el("div");
        g.textContent = GLYPH[piece.side][piece.kind];
        g.style.fontSize = "1.55rem";
        g.style.lineHeight = "1";
        g.style.color = piece.side === "white" ? "var(--head)" : "var(--leaf)";
        g.style.fontWeight = "700";
        node.append(g);
      }
      if (target && !piece) {
        const dot = el("div");
        dot.style.width = "18%";
        dot.style.height = "18%";
        dot.style.borderRadius = "50%";
        dot.style.background = "rgba(61,220,132,0.85)";
        node.append(dot);
      } else if (target && piece) {
        node.style.outline = "2px solid rgba(255,90,90,0.9)";
        node.style.outlineOffset = "-2px";
      }
    });

    maybeRecord();
    if (state.phase === "setup") {
      hotSeatLobby(pit, {
        title: "CHESS",
        leafRole: "WHITE",
        headRole: "BLACK",
        blurb: "Hot seat chess. Castling, en passant, promotion. Pass the phone.",
        onStart: (white, black) => {
          state = start(white, black);
          render();
        },
      });
    } else if (state.passPending) {
      passOverlay(pit, nameOf(state, state.turn), () => {
        state = { ...state, passPending: false };
        render();
      });
    } else if (state.phase === "promote") {
      promoOverlay(pit, (kind) => {
        state = pickPromo(state, kind);
        render();
      });
    } else if (state.phase === "over") {
      seatResult(pit, {
        title: endLine(state),
        detail:
          state.end === "mate"
            ? `${nameOf(state, other(state.turn))} wins`
            : "No winner this round",
        onRematch: () => {
          state = start(state.whiteName, state.blackName);
          render();
        },
        onLobby: () => {
          state = lobby();
          render();
        },
      });
    } else clearOverlays(pit);
  };

  render();
  return { destroy: () => root.replaceChildren() };
}
