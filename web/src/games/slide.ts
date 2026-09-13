import type { Dir } from "../core/catalog";
import { formatScore } from "../core/catalog";
import { scores } from "../core/scores";
import {
  bindSwipe,
  el,
  frameLoop,
  GameHandle,
  hud,
  leafButton,
  maybeOfferScore,
} from "../core/ui";
import { clearOverlays } from "./types";

type Phase = "ready" | "playing" | "won";

type State = {
  cells: number[];
  elapsed: number;
  moves: number;
  phase: Phase;
  scoreOffered: boolean;
};

function solved(): number[] {
  return Array.from({ length: 16 }, (_, i) => (i === 15 ? 0 : i + 1));
}

function scramble(): number[] {
  const cells = solved();
  let empty = 15;
  for (let n = 0; n < 90; n++) {
    const r = (empty / 4) | 0;
    const c = empty % 4;
    const opts: number[] = [];
    if (r > 0) opts.push(empty - 4);
    if (r < 3) opts.push(empty + 4);
    if (c > 0) opts.push(empty - 1);
    if (c < 3) opts.push(empty + 1);
    const pick = opts[(Math.random() * opts.length) | 0]!;
    cells[empty] = cells[pick]!;
    cells[pick] = 0;
    empty = pick;
  }
  return cells;
}

function fresh(): State {
  return { cells: scramble(), elapsed: 0, moves: 0, phase: "ready", scoreOffered: false };
}

function tap(state: State, i: number): State {
  if (state.phase === "won" || i < 0 || i > 15) return state;
  const empty = state.cells.indexOf(0);
  const er = (empty / 4) | 0;
  const ec = empty % 4;
  const r = (i / 4) | 0;
  const c = i % 4;
  const adj = (r === er && Math.abs(c - ec) === 1) || (c === ec && Math.abs(r - er) === 1);
  if (!adj) return state.phase === "ready" ? { ...state, phase: "playing" } : state;
  const next = state.cells.slice();
  next[empty] = state.cells[i]!;
  next[i] = 0;
  const playing: Phase = state.phase === "ready" ? "playing" : state.phase;
  const won = next.every((v, idx) => v === solved()[idx]);
  return {
    ...state,
    cells: next,
    moves: state.moves + 1,
    phase: won ? "won" : playing,
  };
}

function swipe(state: State, dir: Dir): State {
  const empty = state.cells.indexOf(0);
  const r = (empty / 4) | 0;
  const c = empty % 4;
  let from = -1;
  if (dir === "left" && c < 3) from = empty + 1;
  else if (dir === "right" && c > 0) from = empty - 1;
  else if (dir === "up" && r < 3) from = empty + 4;
  else if (dir === "down" && r > 0) from = empty - 4;
  else return state;
  return tap(state, from);
}

export function mountSlide(root: HTMLElement, onBack: () => void): GameHandle {
  let state = fresh();
  let destroyed = false;
  let tickAcc = 0;

  const wrap = el("div", "game-root");
  const hudHost = el("div");
  const pit = el("div", "pit");
  const board = el("div", "cell-grid");
  board.style.gridTemplateColumns = "repeat(4, 1fr)";
  board.style.padding = "12px";
  board.style.maxWidth = "100%";
  board.style.aspectRatio = "1";
  board.style.margin = "auto";
  const nodes = Array.from({ length: 16 }, (_, i) => {
    const c = el("div", "cell");
    c.addEventListener("click", () => {
      state = tap(state, i);
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
        state = fresh();
        render();
      },
      true,
    ),
  );
  const tip = el("p", "pit-hint", "Tap a tile beside the gap, or swipe.");
  tip.style.position = "static";
  tip.style.marginTop = "8px";
  wrap.append(hudHost, pit, controls, tip);
  root.replaceChildren(wrap);

  const offer = () => {
    if (state.scoreOffered) return;
    state = { ...state, scoreOffered: true };
    const score = Math.max(1, state.elapsed);
    maybeOfferScore(pit, {
      boardKey: "slide",
      score,
      lowerBetter: true,
      headline: "Solved",
      detail: formatScore(score, true),
      onDone: () => {
        if (destroyed) return;
        clearOverlays(pit);
        const box = el("div", "overlay");
        box.append(el("h2", undefined, "Solved"), el("p", undefined, formatScore(score, true)));
        const actions = el("div", "actions");
        actions.append(
          leafButton("AGAIN", () => {
            state = fresh();
            render();
          }),
        );
        box.append(actions);
        pit.append(box);
      },
    });
  };

  const render = () => {
    const best = scores.best("slide", true);
    hudHost.replaceChildren(
      hud({
        title: "SLIDE",
        subtitle: `best ${best ? formatScore(best.score, true) : "—"}  ·  ${state.moves} moves`,
        value: formatScore(state.elapsed, true),
        valueLabel: "time",
        onBack,
      }),
    );
    nodes.forEach((node, i) => {
      const v = state.cells[i]!;
      node.textContent = v ? String(v) : "";
      node.style.background = v === 0 ? "var(--pit-alt)" : "var(--leaf)";
      node.style.color = "var(--void)";
      node.style.fontWeight = "800";
      node.style.fontSize = "1.25rem";
    });
    if (state.phase === "won") offer();
    else clearOverlays(pit);
  };

  bindSwipe(pit, (dir) => {
    state = swipe(state, dir);
    render();
  });

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
    state = swipe(state, dir);
    render();
  };
  window.addEventListener("keydown", onKey);

  const stop = frameLoop((dt) => {
    if (state.phase !== "playing") return;
    tickAcc += dt;
    if (tickAcc >= 1) {
      tickAcc -= 1;
      state = { ...state, elapsed: state.elapsed + 1 };
      render();
    }
  });

  render();
  return {
    destroy: () => {
      destroyed = true;
      stop();
      window.removeEventListener("keydown", onKey);
      root.replaceChildren();
    },
  };
}
