import type { Dir } from "../core/catalog";
import { scores } from "../core/scores";
import {
  bindSwipe,
  el,
  GameHandle,
  hud,
  leafButton,
  maybeOfferScore,
} from "../core/ui";
import { clearOverlays, endOverlay } from "./types";

type Phase = "ready" | "playing" | "won" | "dead";

type State = {
  cells: number[];
  score: number;
  phase: Phase;
  wonKeptGoing: boolean;
  scoreOffered: boolean;
};

function spawn(cells: number[]): number[] {
  const empty = cells.map((v, i) => (v === 0 ? i : -1)).filter((i) => i >= 0);
  if (!empty.length) return cells;
  const next = cells.slice();
  next[empty[(Math.random() * empty.length) | 0]!] = Math.random() < 0.1 ? 4 : 2;
  return next;
}

function fresh(): State {
  return {
    cells: spawn(spawn(Array(16).fill(0))),
    score: 0,
    phase: "ready",
    wonKeptGoing: false,
    scoreOffered: false,
  };
}

function slide(cells: number[], dir: Dir): { next: number[]; gained: number } {
  const next = Array(16).fill(0) as number[];
  let gained = 0;
  const vertical = dir === "up" || dir === "down";
  const reverse = dir === "right" || dir === "down";
  for (let line = 0; line < 4; line++) {
    const vals: number[] = [];
    for (let i = 0; i < 4; i++) {
      const idx = vertical
        ? (reverse ? 3 - i : i) * 4 + line
        : line * 4 + (reverse ? 3 - i : i);
      vals.push(cells[idx]!);
    }
    const compact = vals.filter((v) => v !== 0);
    const merged: number[] = [];
    let i = 0;
    while (i < compact.length) {
      if (i + 1 < compact.length && compact[i] === compact[i + 1]) {
        const v = compact[i]! * 2;
        merged.push(v);
        gained += v;
        i += 2;
      } else {
        merged.push(compact[i]!);
        i += 1;
      }
    }
    while (merged.length < 4) merged.push(0);
    for (let j = 0; j < 4; j++) {
      const idx = vertical
        ? (reverse ? 3 - j : j) * 4 + line
        : line * 4 + (reverse ? 3 - j : j);
      next[idx] = merged[j]!;
    }
  }
  return { next, gained };
}

function hasMove(cells: number[]): boolean {
  if (cells.some((v) => v === 0)) return true;
  for (let y = 0; y < 4; y++)
    for (let x = 0; x < 4; x++) {
      const v = cells[y * 4 + x]!;
      if (x < 3 && cells[y * 4 + x + 1] === v) return true;
      if (y < 3 && cells[(y + 1) * 4 + x] === v) return true;
    }
  return false;
}

function swipe(state: State, dir: Dir): State {
  if (state.phase === "dead") return state;
  const { next, gained } = slide(state.cells, dir);
  if (next.every((v, i) => v === state.cells[i])) return state;
  let s: State = {
    ...state,
    cells: spawn(next),
    score: state.score + gained,
    phase: "playing",
  };
  if (!s.wonKeptGoing && s.cells.some((v) => v >= 2048)) {
    s = { ...s, phase: "won", wonKeptGoing: true };
  }
  if (!hasMove(s.cells)) s = { ...s, phase: "dead" };
  return s;
}

function tileColor(value: number): string {
  if (value === 0) return "var(--pad)";
  const t = Math.min(1, Math.log2(value) / 11);
  const leaf = [61, 220, 132];
  const bite = [255, 90, 106];
  const r = Math.round(leaf[0]! + (bite[0]! - leaf[0]!) * t);
  const g = Math.round(leaf[1]! + (64 - leaf[1]!) * t);
  const b = Math.round(leaf[2]! + (bite[2]! - leaf[2]!) * t);
  return `rgb(${r},${g},${b})`;
}

export function mountMerge(root: HTMLElement, onBack: () => void): GameHandle {
  let state = fresh();
  let destroyed = false;
  const wrap = el("div", "game-root");
  const hudHost = el("div");
  const pit = el("div", "pit");
  pit.style.aspectRatio = "1";
  pit.style.flex = "0 0 auto";
  const grid = el("div", "cell-grid");
  grid.style.gridTemplateColumns = "repeat(4, 1fr)";
  grid.style.padding = "12px";
  grid.style.height = "100%";
  grid.style.boxSizing = "border-box";
  const cells = Array.from({ length: 16 }, () => {
    const c = el("div", "cell");
    grid.append(c);
    return c;
  });
  pit.append(grid);
  const controls = el("div", "controls");
  controls.append(
    leafButton(
      "NEW BOARD",
      () => {
        state = fresh();
        render();
      },
      true,
    ),
  );
  const hint = el("p", "pit-hint", "Swipe or arrow keys to slide.");
  hint.style.position = "static";
  hint.style.marginTop = "8px";
  wrap.append(hudHost, pit, controls, hint);
  root.replaceChildren(wrap);

  const offerEnd = () => {
    if (state.scoreOffered) {
      if (state.phase === "dead") {
        endOverlay(pit, {
          title: "No moves",
          detail: `${state.score} pts`,
          onRetry: () => {
            state = fresh();
            render();
          },
        });
      } else if (state.phase === "won") {
        endOverlay(pit, {
          title: "2048",
          detail: `${state.score} pts`,
          retryLabel: "KEEP GOING",
          onRetry: () => {
            state = { ...state, phase: "playing", scoreOffered: false };
            clearOverlays(pit);
            render();
          },
        });
      }
      return;
    }
    state = { ...state, scoreOffered: true };
    const win = state.phase === "won";
    maybeOfferScore(pit, {
      boardKey: "merge",
      score: state.score,
      lowerBetter: false,
      headline: win ? "2048" : "No moves",
      detail: `${state.score} pts`,
      win: true,
      onDone: () => {
        if (destroyed) return;
        if (state.phase === "won") {
          endOverlay(pit, {
            title: "2048",
            detail: `${state.score} pts`,
            retryLabel: "KEEP GOING",
            onRetry: () => {
              state = { ...state, phase: "playing", scoreOffered: false };
              clearOverlays(pit);
              render();
            },
          });
        } else {
          endOverlay(pit, {
            title: "No moves",
            detail: `${state.score} pts`,
            onRetry: () => {
              state = fresh();
              render();
            },
          });
        }
      },
    });
  };

  const render = () => {
    const best = scores.best("merge", false)?.score ?? 0;
    hudHost.replaceChildren(
      hud({
        title: "2048",
        subtitle: `best ${best}`,
        value: String(state.score),
        valueLabel: "score",
        onBack,
      }),
    );
    cells.forEach((node, i) => {
      const v = state.cells[i]!;
      node.textContent = v ? String(v) : "";
      node.style.background = tileColor(v);
      node.style.color = v <= 4 ? "var(--mist)" : "var(--void)";
      node.style.fontSize = v >= 1024 ? "1rem" : "1.25rem";
      node.style.fontWeight = "800";
    });
    clearOverlays(pit);
    if (state.phase === "won" || state.phase === "dead") offerEnd();
  };

  const apply = (dir: Dir) => {
    const next = swipe(state, dir);
    if (next === state) return;
    state = next;
    render();
  };

  bindSwipe(pit, apply);
  const onKey = (e: KeyboardEvent) => {
    const map: Record<string, Dir> = {
      ArrowUp: "up",
      ArrowDown: "down",
      ArrowLeft: "left",
      ArrowRight: "right",
    };
    const dir = map[e.key];
    if (!dir) return;
    e.preventDefault();
    apply(dir);
  };
  window.addEventListener("keydown", onKey);
  render();

  return {
    destroy: () => {
      destroyed = true;
      window.removeEventListener("keydown", onKey);
      root.replaceChildren();
    },
  };
}
