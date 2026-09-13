import { el, GameHandle, hud, leafButton } from "../core/ui";
import { scores } from "../core/scores";
import { clearOverlays, hotSeatLobby, passOverlay, seatResult } from "./types";

const COLS = 7;
const ROWS = 6;

type Phase = "setup" | "playing" | "over";

type State = {
  cells: number[];
  leafTurn: boolean;
  phase: Phase;
  leafName: string;
  headName: string;
  passPending: boolean;
  winner: number;
  last: number;
  winLine: number[];
  recorded: boolean;
};

function lobby(): State {
  return {
    cells: Array(COLS * ROWS).fill(0),
    leafTurn: true,
    phase: "setup",
    leafName: "LEAF",
    headName: "HEAD",
    passPending: false,
    winner: 0,
    last: -1,
    winLine: [],
    recorded: false,
  };
}

function start(leaf: string, head: string): State {
  return { ...lobby(), phase: "playing", leafName: leaf, headName: head };
}

function fourLine(board: number[], col: number, row: number, piece: number): number[] {
  const dirs = [
    [1, 0],
    [0, 1],
    [1, 1],
    [1, -1],
  ];
  for (const [dx, dy] of dirs) {
    const cells = [row * COLS + col];
    let c = col + dx!;
    let r = row + dy!;
    while (c >= 0 && c < COLS && r >= 0 && r < ROWS && board[r * COLS + c] === piece) {
      cells.push(r * COLS + c);
      c += dx!;
      r += dy!;
    }
    c = col - dx!;
    r = row - dy!;
    while (c >= 0 && c < COLS && r >= 0 && r < ROWS && board[r * COLS + c] === piece) {
      cells.push(r * COLS + c);
      c -= dx!;
      r -= dy!;
    }
    if (cells.length >= 4) return cells;
  }
  return [];
}

function drop(state: State, col: number): State {
  if (state.phase !== "playing" || state.passPending) return state;
  if (col < 0 || col >= COLS) return state;
  let row = ROWS - 1;
  while (row >= 0 && state.cells[row * COLS + col] !== 0) row--;
  if (row < 0) return state;
  const piece = state.leafTurn ? 1 : 2;
  const next = state.cells.slice();
  const i = row * COLS + col;
  next[i] = piece;
  const line = fourLine(next, col, row, piece);
  const full = next.every((v) => v !== 0);
  return {
    ...state,
    cells: next,
    leafTurn: !state.leafTurn,
    last: i,
    winLine: line,
    phase: line.length || full ? "over" : "playing",
    winner: line.length ? piece : 0,
    passPending: !line.length && !full,
  };
}

export function mountFour(root: HTMLElement, onBack: () => void): GameHandle {
  let state = lobby();

  const wrap = el("div", "game-root");
  const hudHost = el("div");
  const pit = el("div", "pit");
  const grid = el("div", "cell-grid");
  grid.style.gridTemplateColumns = `repeat(${COLS}, 1fr)`;
  grid.style.padding = "10px";
  grid.style.height = "100%";
  grid.style.gap = "6px";
  const nodes = Array.from({ length: COLS * ROWS }, (_, i) => {
    const c = el("div", "cell");
    c.style.borderRadius = "10px";
    c.addEventListener("click", () => {
      const col = i % COLS;
      state = drop(state, col);
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
  const tip = el("p", "pit-hint", "Tap a column. Gravity does the rest.");
  tip.style.position = "static";
  tip.style.marginTop = "8px";
  wrap.append(hudHost, pit, controls, tip);
  root.replaceChildren(wrap);

  const maybeRecord = () => {
    if (state.phase !== "over" || state.recorded) return;
    state = { ...state, recorded: true };
    if (state.winner === 1) {
      scores.submit("four", state.leafName, 1, true);
      scores.submit("four", state.headName, 0, false);
    } else if (state.winner === 2) {
      scores.submit("four", state.headName, 1, true);
      scores.submit("four", state.leafName, 0, false);
    } else {
      scores.submit("four", state.leafName, 0, false);
      scores.submit("four", state.headName, 0, false);
    }
  };

  const render = () => {
    const name = state.leafTurn ? state.leafName : state.headName;
    const subtitle =
      state.phase === "setup"
        ? "hot seat"
        : state.phase === "over"
          ? state.winner === 1
            ? `${state.leafName} connects`
            : state.winner === 2
              ? `${state.headName} connects`
              : "Draw"
          : `${name} to drop`;
    hudHost.replaceChildren(
      hud({
        title: "FOUR",
        subtitle,
        value: state.phase === "setup" ? "2P" : state.leafTurn ? "L" : "H",
        valueLabel: "turn",
        onBack,
      }),
    );
    nodes.forEach((node, i) => {
      const v = state.cells[i]!;
      const win = state.winLine.includes(i);
      const r = (i / COLS) | 0;
      node.replaceChildren();
      node.style.background = win ? "rgba(61,220,132,0.22)" : "var(--pit-alt)";
      if (v !== 0) {
        const disc = el("div");
        disc.style.width = "82%";
        disc.style.height = "82%";
        disc.style.borderRadius = "50%";
        disc.style.background = v === 1 ? "var(--leaf)" : "var(--head)";
        disc.style.border =
          win || state.last === i ? "2px solid var(--mist)" : "none";
        node.append(disc);
      } else if (r === 0 && state.phase === "playing") {
        const hint = el("div");
        hint.style.width = "28%";
        hint.style.height = "28%";
        hint.style.borderRadius = "50%";
        hint.style.background = state.leafTurn ? "rgba(61,220,132,0.18)" : "rgba(200,255,122,0.18)";
        node.append(hint);
      }
    });

    maybeRecord();
    if (state.phase === "setup") {
      hotSeatLobby(pit, {
        title: "FOUR",
        leafRole: "GREEN",
        headRole: "LIME",
        blurb: "Drop a disc. Four in a row wins. Pass the phone.",
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
      seatResult(pit, {
        title:
          state.winner === 1
            ? `${state.leafName} connects`
            : state.winner === 2
              ? `${state.headName} connects`
              : "Draw",
        detail: "Four in a row",
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
  return {
    destroy: () => root.replaceChildren(),
  };
}
