import { el, GameHandle, hud, leafButton } from "../core/ui";
import { clearOverlays, hotSeatLobby, passOverlay, recordHotSeat, seatResult } from "./types";

const N = 8;

type Phase = "setup" | "playing" | "over";

type State = {
  cells: number[];
  leafTurn: boolean;
  phase: Phase;
  leafName: string;
  headName: string;
  passPending: boolean;
  recorded: boolean;
  skipped: boolean;
  last: number;
  flipped: number[];
};

function startBoard(): number[] {
  const b = Array(64).fill(0);
  b[3 * 8 + 3] = 2;
  b[3 * 8 + 4] = 1;
  b[4 * 8 + 3] = 1;
  b[4 * 8 + 4] = 2;
  return b;
}

function lobby(): State {
  return {
    cells: startBoard(),
    leafTurn: true,
    phase: "setup",
    leafName: "LEAF",
    headName: "HEAD",
    passPending: false,
    recorded: false,
    skipped: false,
    last: -1,
    flipped: [],
  };
}

function start(leaf: string, head: string): State {
  return { ...lobby(), phase: "playing", leafName: leaf, headName: head };
}

function flips(board: number[], at: number, side: number): number[] {
  const opp = side === 1 ? 2 : 1;
  const c0 = at % 8;
  const r0 = (at / 8) | 0;
  const out: number[] = [];
  const dirs = [
    [1, 0],
    [-1, 0],
    [0, 1],
    [0, -1],
    [1, 1],
    [1, -1],
    [-1, 1],
    [-1, -1],
  ];
  for (const [dx, dy] of dirs) {
    const run: number[] = [];
    let c = c0 + dx!;
    let r = r0 + dy!;
    while (c >= 0 && c <= 7 && r >= 0 && r <= 7) {
      const i = r * 8 + c;
      const p = board[i]!;
      if (p === opp) run.push(i);
      else if (p === side && run.length) {
        out.push(...run);
        break;
      } else break;
      c += dx!;
      r += dy!;
    }
  }
  return out;
}

function legalOn(board: number[], leaf: boolean): number[] {
  const side = leaf ? 1 : 2;
  return Array.from({ length: 64 }, (_, i) => i).filter((i) => board[i] === 0 && flips(board, i, side).length);
}

function count(cells: number[], leaf: boolean) {
  const side = leaf ? 1 : 2;
  return cells.filter((v) => v === side).length;
}

function play(state: State, i: number): State {
  if (state.phase !== "playing" || state.passPending) return state;
  const side = state.leafTurn ? 1 : 2;
  const flipped = flips(state.cells, i, side);
  if (state.cells[i] !== 0 || !flipped.length) return state;
  const next = state.cells.slice();
  next[i] = side;
  for (const f of flipped) next[f] = side;
  const opp = !state.leafTurn;
  const oppMoves = legalOn(next, opp);
  const selfMoves = legalOn(next, state.leafTurn);
  if (oppMoves.length) {
    return { ...state, cells: next, leafTurn: opp, passPending: true, skipped: false, last: i, flipped };
  }
  if (selfMoves.length) {
    return { ...state, cells: next, skipped: true, passPending: false, last: i, flipped };
  }
  return { ...state, cells: next, phase: "over", skipped: false, last: i, flipped };
}

export function mountFlip(root: HTMLElement, onBack: () => void): GameHandle {
  let state = lobby();

  const wrap = el("div", "game-root");
  const hudHost = el("div");
  const pit = el("div", "pit");
  const board = el("div", "cell-grid");
  board.style.gridTemplateColumns = `repeat(${N}, 1fr)`;
  board.style.padding = "8px";
  board.style.aspectRatio = "1";
  board.style.margin = "auto";
  board.style.gap = "0";
  board.style.maxWidth = "100%";
  const nodes = Array.from({ length: 64 }, (_, i) => {
    const c = el("div", "cell");
    c.style.borderRadius = "0";
    c.style.border = "none";
    c.addEventListener("click", () => {
      const legal = legalOn(state.cells, state.leafTurn);
      if (!legal.includes(i)) return;
      state = play(state, i);
      render();
    });
    board.append(c);
    return c;
  });
  pit.append(board);
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
  const tip = el("p", "pit-hint", "Dots mark legal drops.");
  tip.style.position = "static";
  tip.style.marginTop = "8px";
  wrap.append(hudHost, pit, controls, tip);
  root.replaceChildren(wrap);

  const maybeRecord = () => {
    if (state.phase !== "over" || state.recorded) return;
    state = { ...state, recorded: true };
    recordHotSeat("flip", state.leafName, count(state.cells, true), state.headName, count(state.cells, false));
  };

  const render = () => {
    const name = state.leafTurn ? state.leafName : state.headName;
    const leafN = count(state.cells, true);
    const headN = count(state.cells, false);
    const subtitle =
      state.phase === "setup"
        ? "hot seat"
        : state.phase === "over"
          ? "board full"
          : state.skipped
            ? `${name} plays again — no move`
            : `${name} to flip`;
    hudHost.replaceChildren(
      hud({
        title: "FLIP",
        subtitle,
        value: `${leafN}–${headN}`,
        valueLabel: "discs",
        onBack,
      }),
    );
    const legal = state.phase === "playing" && !state.passPending ? legalOn(state.cells, state.leafTurn) : [];
    nodes.forEach((node, i) => {
      const c = i % 8;
      const r = (i / 8) | 0;
      const v = state.cells[i]!;
      node.replaceChildren();
      node.style.background = (c + r) % 2 === 0 ? "var(--pad)" : "var(--pit-alt)";
      if (v !== 0) {
        const disc = el("div");
        disc.style.width = "72%";
        disc.style.height = "72%";
        disc.style.borderRadius = "50%";
        disc.style.background = v === 1 ? "var(--leaf)" : "var(--head)";
        if (i === state.last || state.flipped.includes(i)) {
          disc.style.border = "2px solid var(--mist)";
        }
        node.append(disc);
      } else if (legal.includes(i)) {
        const dot = el("div");
        dot.style.width = "22%";
        dot.style.height = "22%";
        dot.style.borderRadius = "50%";
        dot.style.background = state.leafTurn ? "rgba(61,220,132,0.55)" : "rgba(200,255,122,0.55)";
        node.append(dot);
      }
    });

    maybeRecord();
    if (state.phase === "setup") {
      hotSeatLobby(pit, {
        title: "FLIP",
        leafRole: "GREEN",
        headRole: "LIME",
        blurb: "Reversi. Trap a line, flip them. Most discs wins.",
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
        title: leafN > headN ? `${state.leafName} takes it` : headN > leafN ? `${state.headName} takes it` : "Draw",
        detail: `${state.leafName} ${leafN}  ·  ${state.headName} ${headN}`,
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
