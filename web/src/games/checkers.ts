import { el, GameHandle, hud, leafButton } from "../core/ui";
import { scores } from "../core/scores";
import { clearOverlays, hotSeatLobby, passOverlay, seatResult } from "./types";

type Man = { leaf: boolean; king: boolean };
type CkMove = { from: number; to: number; over: number };
type Phase = "setup" | "playing" | "over";

type State = {
  board: (Man | null)[];
  leafTurn: boolean;
  selected: number | null;
  targets: CkMove[];
  chain: number | null;
  last: CkMove | null;
  phase: Phase;
  leafName: string;
  headName: string;
  passPending: boolean;
  winnerLeaf: boolean | null;
  recorded: boolean;
};

const EMPTY_MOVE: CkMove = { from: -1, to: -1, over: -1 };

function playable(x: number, y: number) {
  return (x + y) % 2 === 0;
}
function sq(x: number, y: number) {
  return y * 8 + x;
}
function xOf(i: number) {
  return i % 8;
}
function yOf(i: number) {
  return (i / 8) | 0;
}

function dirs(man: Man): [number, number][] {
  if (man.king) return [
    [1, 1],
    [-1, 1],
    [1, -1],
    [-1, -1],
  ];
  return man.leaf
    ? [
        [1, 1],
        [-1, 1],
      ]
    : [
        [1, -1],
        [-1, -1],
      ];
}

function startBoard(): (Man | null)[] {
  const board: (Man | null)[] = Array(64).fill(null);
  for (let y = 0; y <= 2; y++) {
    for (let x = 0; x <= 7; x++) {
      if (playable(x, y)) board[sq(x, y)] = { leaf: true, king: false };
    }
  }
  for (let y = 5; y <= 7; y++) {
    for (let x = 0; x <= 7; x++) {
      if (playable(x, y)) board[sq(x, y)] = { leaf: false, king: false };
    }
  }
  return board;
}

function lobby(): State {
  return {
    board: startBoard(),
    leafTurn: true,
    selected: null,
    targets: [],
    chain: null,
    last: null,
    phase: "setup",
    leafName: "LEAF",
    headName: "HEAD",
    passPending: false,
    winnerLeaf: null,
    recorded: false,
  };
}

function start(leafName: string, headName: string): State {
  return { ...lobby(), phase: "playing", leafName, headName };
}

function pieceScore(board: (Man | null)[], leaf: boolean): number {
  return board.reduce((acc, m) => {
    if (!m || m.leaf !== leaf) return acc;
    return acc + (m.king ? 2 : 1);
  }, 0);
}

function jumpsFrom(board: (Man | null)[], from: number, man: Man): CkMove[] {
  const out: CkMove[] = [];
  for (const [dx, dy] of dirs(man)) {
    const mx = xOf(from) + dx!;
    const my = yOf(from) + dy!;
    const tx = xOf(from) + dx! * 2;
    const ty = yOf(from) + dy! * 2;
    if (tx < 0 || tx > 7 || ty < 0 || ty > 7) continue;
    const mid = sq(mx, my);
    const to = sq(tx, ty);
    const jumped = board[mid];
    if (!jumped || jumped.leaf === man.leaf) continue;
    if (board[to] != null) continue;
    out.push({ from, to, over: mid });
  }
  return out;
}

function allJumps(board: (Man | null)[], leafTurn: boolean): CkMove[] {
  const out: CkMove[] = [];
  for (let i = 0; i < 64; i++) {
    const man = board[i];
    if (!man || man.leaf !== leafTurn) continue;
    out.push(...jumpsFrom(board, i, man));
  }
  return out;
}

function quietFrom(board: (Man | null)[], from: number): CkMove[] {
  const man = board[from];
  if (!man) return [];
  const out: CkMove[] = [];
  for (const [dx, dy] of dirs(man)) {
    const x = xOf(from) + dx!;
    const y = yOf(from) + dy!;
    if (x < 0 || x > 7 || y < 0 || y > 7) continue;
    const to = sq(x, y);
    if (board[to] == null) out.push({ from, to, over: -1 });
  }
  return out;
}

function legalAny(board: (Man | null)[], leafTurn: boolean): CkMove[] {
  const jumps: CkMove[] = [];
  const quiet: CkMove[] = [];
  for (let i = 0; i < 64; i++) {
    const man = board[i];
    if (!man || man.leaf !== leafTurn) continue;
    jumps.push(...jumpsFrom(board, i, man));
    for (const [dx, dy] of dirs(man)) {
      const x = xOf(i) + dx!;
      const y = yOf(i) + dy!;
      if (x < 0 || x > 7 || y < 0 || y > 7) continue;
      const to = sq(x, y);
      if (board[to] == null) quiet.push({ from: i, to, over: -1 });
    }
  }
  return jumps.length ? jumps : quiet;
}

function play(state: State, move: CkMove): State {
  const next = state.board.slice();
  let man = next[move.from];
  if (!man) return state;
  next[move.from] = null;
  if (move.over >= 0) next[move.over] = null;
  const y = yOf(move.to);
  const crowned = !man.king && ((man.leaf && y === 7) || (!man.leaf && y === 0));
  if (crowned) man = { ...man, king: true };
  next[move.to] = man;
  const more =
    !crowned && move.over >= 0 ? jumpsFrom(next, move.to, man) : [];
  if (more.length) {
    return {
      ...state,
      board: next,
      selected: move.to,
      targets: more,
      chain: move.to,
      last: move,
    };
  }
  const nextTurn = !state.leafTurn;
  const opponentMoves = legalAny(next, nextTurn);
  const opponentLeft = next.some((m) => m != null && m.leaf === nextTurn);
  const over = !opponentLeft || opponentMoves.length === 0;
  return {
    ...state,
    board: next,
    leafTurn: nextTurn,
    selected: null,
    targets: [],
    chain: null,
    last: move,
    phase: over ? "over" : "playing",
    passPending: !over,
    winnerLeaf: over ? state.leafTurn : null,
  };
}

function tap(state: State, square: number): State {
  if (state.phase !== "playing" || state.passPending) return state;
  if (state.chain != null) {
    const hit = state.targets.find((m) => m.to === square);
    return hit ? play(state, hit) : state;
  }
  const hit = state.targets.find((m) => m.to === square);
  if (state.selected != null && hit) return play(state, hit);
  const man = state.board[square];
  if (!man) return { ...state, selected: null, targets: [] };
  if (man.leaf !== state.leafTurn) return { ...state, selected: null, targets: [] };
  const jumps = allJumps(state.board, state.leafTurn);
  let moves: CkMove[];
  if (jumps.length) {
    const mine = jumps.filter((m) => m.from === square);
    if (!mine.length) return { ...state, selected: null, targets: [] };
    moves = mine;
  } else {
    moves = quietFrom(state.board, square);
  }
  return { ...state, selected: square, targets: moves };
}

function logicalFromDisplay(display: number, flipped: boolean): number {
  const col = display % 8;
  const row = (display / 8) | 0;
  const x = flipped ? 7 - col : col;
  const y = flipped ? row : 7 - row;
  return sq(x, y);
}

export function mountCheckers(root: HTMLElement, onBack: () => void): GameHandle {
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
      const flipped = !state.leafTurn;
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
  const tip = el("p", "pit-hint", "Jumps are forced. Kings move both ways. Board flips each turn.");
  tip.style.position = "static";
  tip.style.marginTop = "8px";
  wrap.append(hudHost, pit, controls, tip);
  root.replaceChildren(wrap);

  const maybeRecord = () => {
    if (state.phase !== "over" || state.recorded) return;
    state = { ...state, recorded: true };
    if (state.winnerLeaf != null) {
      const name = state.winnerLeaf ? state.leafName : state.headName;
      const pts = Math.max(1, pieceScore(state.board, state.winnerLeaf));
      scores.submit("checkers", name, pts, true);
    } else {
      scores.submit("checkers", state.leafName, 0, false);
      scores.submit("checkers", state.headName, 0, false);
    }
  };

  const render = () => {
    const name = state.leafTurn ? state.leafName : state.headName;
    const mustJump = state.chain != null || allJumps(state.board, state.leafTurn).length > 0;
    const leafPts = pieceScore(state.board, true);
    const headPts = pieceScore(state.board, false);
    const subtitle =
      state.phase === "setup"
        ? "hot seat"
        : state.phase === "over"
          ? `${state.winnerLeaf != null ? (state.winnerLeaf ? state.leafName : state.headName) : name} wins`
          : state.chain != null
            ? `${name}  ·  keep jumping`
            : mustJump && state.selected != null
              ? `${name}  ·  jump`
              : `${name} to move`;
    hudHost.replaceChildren(
      hud({
        title: "CHECKERS",
        subtitle,
        value: state.phase === "setup" ? "2P" : `${leafPts}–${headPts}`,
        valueLabel: "men",
        onBack,
      }),
    );

    const flipped = !state.leafTurn;
    const targetSet = new Set(state.targets.map((m) => m.to));
    const last = state.last ?? EMPTY_MOVE;

    nodes.forEach((node, display) => {
      const square = logicalFromDisplay(display, flipped);
      const x = xOf(square);
      const y = yOf(square);
      const dark = playable(x, y);
      const man = state.board[square];
      const selected = state.selected === square;
      const target = targetSet.has(square);
      const lastHit = last.from === square || last.to === square || last.over === square;
      node.replaceChildren();
      node.style.background = selected
        ? "rgba(61,220,132,0.5)"
        : lastHit
          ? "rgba(200,255,122,0.16)"
          : dark
            ? "var(--pad)"
            : "var(--pit-alt)";
      if (man) {
        const disc = el("div");
        disc.style.width = "72%";
        disc.style.height = "72%";
        disc.style.borderRadius = "50%";
        disc.style.background = man.leaf ? "var(--leaf)" : "var(--head)";
        disc.style.display = "grid";
        disc.style.placeItems = "center";
        disc.style.fontSize = "0.7rem";
        disc.style.fontWeight = "700";
        disc.style.color = "var(--mist)";
        if (man.king) disc.textContent = "K";
        if (selected) disc.style.border = "2px solid var(--mist)";
        node.append(disc);
      }
      if (target) {
        const dot = el("div");
        dot.style.width = "22%";
        dot.style.height = "22%";
        dot.style.borderRadius = "50%";
        dot.style.background = "rgba(61,220,132,0.85)";
        if (man) {
          node.style.outline = "2px solid var(--bite, #ff6b6b)";
          node.style.outlineOffset = "-2px";
        } else {
          node.style.outline = "none";
          node.append(dot);
        }
      } else {
        node.style.outline = "none";
      }
    });

    maybeRecord();
    if (state.phase === "setup") {
      hotSeatLobby(pit, {
        title: "CHECKERS",
        leafRole: "GREEN",
        headRole: "LIME",
        blurb: "American checkers. Forced jumps. Pass the phone.",
        onStart: (leaf, head) => {
          state = start(leaf, head);
          render();
        },
      });
    } else if (state.passPending) {
      passOverlay(pit, name, () => {
        state = { ...state, passPending: false };
        render();
      });
    } else if (state.phase === "over") {
      const winner =
        state.winnerLeaf != null ? (state.winnerLeaf ? state.leafName : state.headName) : name;
      seatResult(pit, {
        title: `${winner} takes the board`,
        detail: `${state.leafName} ${leafPts}  ·  ${state.headName} ${headPts}`,
        onRematch: () => {
          state = start(state.leafName, state.headName);
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
