import { formatScore } from "../core/catalog";
import { el, frameLoop, GameHandle, hud, leafButton, maybeOfferScore } from "../core/ui";
import { clearOverlays, endOverlay } from "./types";

type Diff = { cols: number; rows: number; mines: number; label: string; boardKey: string };
type Mark = "none" | "flag" | "question";
type Status = "ready" | "playing" | "won" | "lost";
type Tile = { mine: boolean; adj: number; open: boolean; mark: Mark };

type State = {
  diff: Diff;
  tiles: Tile[];
  status: Status;
  elapsedSec: number;
  flagMode: boolean;
  questions: boolean;
  scoreOffered: boolean;
};

const DIFFS: Diff[] = [
  { cols: 9, rows: 9, mines: 10, label: "EASY", boardKey: "mines_beginner" },
  { cols: 16, rows: 16, mines: 40, label: "MED", boardKey: "mines_intermediate" },
  { cols: 16, rows: 30, mines: 99, label: "HARD", boardKey: "mines_expert" },
];

const NUM = ["#7fdbff", "#3ddc84", "#ff5a6a", "#9aa8ff", "#e6a15c", "#64d8cb", "#e8f5e9", "#90a4ae"];

function blank(diff: Diff): Tile[] {
  return Array.from({ length: diff.cols * diff.rows }, () => ({
    mine: false,
    adj: 0,
    open: false,
    mark: "none" as Mark,
  }));
}

function make(diff: Diff, flagMode = false, questions = true): State {
  return {
    diff,
    tiles: blank(diff),
    status: "ready",
    elapsedSec: 0,
    flagMode,
    questions,
    scoreOffered: false,
  };
}

function flags(state: State) {
  return state.tiles.filter((t) => t.mark === "flag").length;
}

function placeMines(state: State, safeX: number, safeY: number): State {
  const { cols, rows, mines } = state.diff;
  const forbidden = new Set<number>();
  for (let dy = -1; dy <= 1; dy++)
    for (let dx = -1; dx <= 1; dx++) {
      const nx = safeX + dx;
      const ny = safeY + dy;
      if (nx >= 0 && nx < cols && ny >= 0 && ny < rows) forbidden.add(ny * cols + nx);
    }
  const pool = Array.from({ length: cols * rows }, (_, i) => i).filter((i) => !forbidden.has(i));
  for (let i = pool.length - 1; i > 0; i--) {
    const j = (Math.random() * (i + 1)) | 0;
    [pool[i], pool[j]] = [pool[j]!, pool[i]!];
  }
  const mineSet = new Set(pool.slice(0, mines));
  const tiles = blank(state.diff);
  for (let i = 0; i < tiles.length; i++) {
    const x = i % cols;
    const y = (i / cols) | 0;
    let adj = 0;
    for (let dy = -1; dy <= 1; dy++)
      for (let dx = -1; dx <= 1; dx++) {
        if (!dx && !dy) continue;
        const nx = x + dx;
        const ny = y + dy;
        if (nx >= 0 && nx < cols && ny >= 0 && ny < rows && mineSet.has(ny * cols + nx)) adj++;
      }
    tiles[i] = { mine: mineSet.has(i), adj, open: false, mark: "none" };
  }
  return { ...state, tiles };
}

function checkWin(state: State): State {
  if (state.tiles.some((t) => !t.mine && !t.open)) return state;
  return {
    ...state,
    status: "won",
    tiles: state.tiles.map((t) => (t.mine ? { ...t, mark: "flag" as Mark } : t)),
  };
}

function reveal(state: State, startX: number, startY: number): State {
  const { cols, rows } = state.diff;
  const next = state.tiles.map((t) => ({ ...t }));
  const stack: Array<[number, number]> = [[startX, startY]];
  while (stack.length) {
    const [x, y] = stack.pop()!;
    const i = y * cols + x;
    const tile = next[i]!;
    if (tile.open || tile.mark === "flag") continue;
    if (tile.mine) {
      for (let idx = 0; idx < next.length; idx++) {
        if (next[idx]!.mine) next[idx] = { ...next[idx]!, open: true, mark: "none" };
      }
      return { ...state, tiles: next, status: "lost" };
    }
    next[i] = { ...tile, open: true, mark: "none" };
    if (tile.adj === 0) {
      for (let dy = -1; dy <= 1; dy++)
        for (let dx = -1; dx <= 1; dx++) {
          const nx = x + dx;
          const ny = y + dy;
          if (nx >= 0 && nx < cols && ny >= 0 && ny < rows) stack.push([nx, ny]);
        }
    }
  }
  return checkWin({ ...state, tiles: next });
}

function chord(state: State, x: number, y: number): State {
  const { cols, rows } = state.diff;
  const tile = state.tiles[y * cols + x]!;
  if (!tile.open || tile.adj === 0) return state;
  let flagCount = 0;
  const hidden: Array<[number, number]> = [];
  for (let dy = -1; dy <= 1; dy++)
    for (let dx = -1; dx <= 1; dx++) {
      if (!dx && !dy) continue;
      const nx = x + dx;
      const ny = y + dy;
      if (nx < 0 || nx >= cols || ny < 0 || ny >= rows) continue;
      const n = state.tiles[ny * cols + nx]!;
      if (n.mark === "flag") flagCount++;
      else if (!n.open) hidden.push([nx, ny]);
    }
  if (flagCount !== tile.adj) return state;
  let cur = state;
  for (const [nx, ny] of hidden) {
    cur = reveal(cur, nx, ny);
    if (cur.status === "lost") return cur;
  }
  return cur;
}

function openAt(state: State, x: number, y: number): State {
  if (state.status === "won" || state.status === "lost") return state;
  const { cols, rows } = state.diff;
  if (x < 0 || x >= cols || y < 0 || y >= rows) return state;
  const tile = state.tiles[y * cols + x]!;
  if (tile.mark === "flag") return state;
  if (tile.open) return chord(state, x, y);
  let next = state;
  if (state.status === "ready") {
    next = { ...placeMines(state, x, y), status: "playing" };
  }
  return reveal(next, x, y);
}

function cycleMark(state: State, x: number, y: number): State {
  if (state.status === "won" || state.status === "lost") return state;
  const { cols, rows } = state.diff;
  if (x < 0 || x >= cols || y < 0 || y >= rows) return state;
  const i = y * cols + x;
  const tile = state.tiles[i]!;
  if (tile.open) return state;
  const mark: Mark =
    tile.mark === "none" ? "flag" : tile.mark === "flag" ? (state.questions ? "question" : "none") : "none";
  const tiles = state.tiles.slice();
  tiles[i] = { ...tile, mark };
  return { ...state, tiles };
}

export function mountMines(root: HTMLElement, onBack: () => void): GameHandle {
  let state = make(DIFFS[0]!);
  let destroyed = false;
  let tickAcc = 0;
  let pressTimer = 0;
  let longFired = false;

  const wrap = el("div", "game-root");
  const hudHost = el("div");
  const diffRow = el("div", "controls");
  const modeRow = el("div", "controls");
  const pit = el("div", "pit");
  pit.style.overflow = "auto";
  const grid = el("div", "cell-grid");
  grid.style.padding = "6px";
  pit.append(grid);
  const tip = el("p", "pit-hint", "Tap to open. Long-press to flag. Tap a number to chord.");
  tip.style.position = "static";
  tip.style.marginTop = "8px";
  wrap.append(hudHost, diffRow, modeRow, pit, tip);
  root.replaceChildren(wrap);

  const offer = () => {
    if (state.scoreOffered) return;
    state = { ...state, scoreOffered: true };
    const win = state.status === "won";
    const score = win ? Math.max(1, state.elapsedSec) : state.elapsedSec;
    maybeOfferScore(pit, {
      boardKey: state.diff.boardKey,
      score,
      lowerBetter: true,
      headline: win ? "Cleared" : "Boom",
      detail: formatScore(score, true),
      win,
      onDone: () => {
        if (destroyed) return;
        endOverlay(pit, {
          title: win ? "Cleared" : "Boom",
          detail: win ? formatScore(score, true) : "Long-press flags. Chord a number.",
          onRetry: () => {
            state = make(state.diff, state.flagMode, state.questions);
            render();
          },
        });
      },
    });
  };

  const paint = () => {
    const { cols, rows } = state.diff;
    grid.style.gridTemplateColumns = `repeat(${cols}, 1fr)`;
    grid.style.width = cols > 12 ? `${cols * 22}px` : "100%";
    grid.style.maxWidth = cols > 12 ? "none" : "100%";
    grid.style.margin = "0 auto";
    grid.replaceChildren();
    for (let y = 0; y < rows; y++) {
      for (let x = 0; x < cols; x++) {
        const tile = state.tiles[y * cols + x]!;
        const cell = el("div", "cell");
        cell.style.fontSize = cols > 12 ? "0.7rem" : "0.95rem";
        if (cols > 12) cell.style.minWidth = "20px";
        if (tile.open && tile.mine) {
          cell.style.background = "var(--bite)";
          cell.textContent = "●";
          cell.style.color = "var(--void)";
        } else if (tile.open) {
          cell.style.background = "var(--pit-alt)";
          if (tile.adj > 0) {
            cell.textContent = String(tile.adj);
            cell.style.color = NUM[tile.adj - 1]!;
          }
        } else {
          cell.style.background = "var(--pad)";
          if (tile.mark === "flag") {
            cell.textContent = "▲";
            cell.style.color = "var(--bite)";
          } else if (tile.mark === "question") {
            cell.textContent = "?";
            cell.style.color = "var(--head)";
          }
        }
        const act = (flag: boolean) => {
          state = flag
            ? state.flagMode
              ? openAt(state, x, y)
              : cycleMark(state, x, y)
            : state.flagMode
              ? cycleMark(state, x, y)
              : openAt(state, x, y);
          render();
        };
        cell.addEventListener("pointerdown", (e) => {
          if (e.button === 2) return;
          longFired = false;
          pressTimer = window.setTimeout(() => {
            longFired = true;
            act(true);
          }, 420);
        });
        cell.addEventListener("pointerup", () => {
          window.clearTimeout(pressTimer);
          if (!longFired) act(false);
        });
        cell.addEventListener("pointerleave", () => window.clearTimeout(pressTimer));
        cell.addEventListener("contextmenu", (e) => {
          e.preventDefault();
          act(true);
        });
        grid.append(cell);
      }
    }
  };

  const render = () => {
    const remaining = state.diff.mines - flags(state);
    hudHost.replaceChildren(
      hud({
        title: "MINES",
        subtitle: `${state.diff.mines} buried`,
        value: formatScore(state.elapsedSec, true),
        valueLabel: remaining >= 0 ? `${remaining} left` : `${-remaining} over`,
        onBack,
      }),
    );
    diffRow.replaceChildren(
      ...DIFFS.map((d) =>
        leafButton(
          d.label,
          () => {
            state = make(d, state.flagMode, state.questions);
            render();
          },
          state.diff.boardKey !== d.boardKey,
        ),
      ),
    );
    modeRow.replaceChildren(
      leafButton(
        state.flagMode ? "FLAG" : "DIG",
        () => {
          state = { ...state, flagMode: !state.flagMode };
          render();
        },
        !state.flagMode,
      ),
      leafButton(
        "NEW",
        () => {
          state = make(state.diff, state.flagMode, state.questions);
          render();
        },
        true,
      ),
    );
    paint();
    if (state.status === "won" || state.status === "lost") offer();
    else clearOverlays(pit);
  };

  const stop = frameLoop((dt) => {
    if (state.status !== "playing") return;
    tickAcc += dt;
    if (tickAcc >= 1) {
      tickAcc -= 1;
      state = { ...state, elapsedSec: state.elapsedSec + 1 };
      render();
    }
  });

  render();
  return {
    destroy: () => {
      destroyed = true;
      stop();
      window.clearTimeout(pressTimer);
      root.replaceChildren();
    },
  };
}
