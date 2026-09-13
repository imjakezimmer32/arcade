import { formatScore } from "../core/catalog";
import { scores } from "../core/scores";
import { el, frameLoop, GameHandle, hud, leafButton, maybeOfferScore } from "../core/ui";
import { clearOverlays, endOverlay } from "./types";

type Phase = "ready" | "playing" | "won";

type Tile = { pair: number; open: boolean; matched: boolean };

type State = {
  tiles: Tile[];
  first: number | null;
  locked: boolean;
  misses: number;
  elapsed: number;
  phase: Phase;
  scoreOffered: boolean;
};

const FACES = [
  "var(--leaf)",
  "var(--head)",
  "var(--bite)",
  "#7fdbff",
  "#e6a15c",
  "#ce93d8",
  "#9aa8ff",
  "#ffc4c8",
];
const GLYPHS = ["●", "★", "✿", "☽", "◆", "♠", "♥", "☀"];

function deal(): Tile[] {
  const ids = Array.from({ length: 8 }, (_, i) => [i, i]).flat();
  for (let i = ids.length - 1; i > 0; i--) {
    const j = (Math.random() * (i + 1)) | 0;
    [ids[i], ids[j]] = [ids[j]!, ids[i]!];
  }
  return ids.map((pair) => ({ pair, open: false, matched: false }));
}

function fresh(): State {
  return {
    tiles: deal(),
    first: null,
    locked: false,
    misses: 0,
    elapsed: 0,
    phase: "ready",
    scoreOffered: false,
  };
}

function flip(state: State, index: number): State {
  if (state.locked || state.phase === "won") return state;
  const tile = state.tiles[index];
  if (!tile || tile.open || tile.matched) return state;
  const opened = state.tiles.map((t, i) => (i === index ? { ...t, open: true } : t));
  const started: Phase = state.phase === "ready" ? "playing" : state.phase;
  if (state.first == null) return { ...state, tiles: opened, first: index, phase: started };
  const a = opened[state.first]!;
  const b = opened[index]!;
  if (a.pair === b.pair) {
    opened[state.first] = { ...a, matched: true, open: true };
    opened[index] = { ...b, matched: true, open: true };
    const won = opened.every((t) => t.matched);
    return { ...state, tiles: opened, first: null, phase: won ? "won" : started };
  }
  return { ...state, tiles: opened, first: state.first, locked: true, misses: state.misses + 1, phase: started };
}

function resolveMismatch(state: State): State {
  if (!state.locked) return state;
  return {
    ...state,
    tiles: state.tiles.map((t) => (t.matched ? t : { ...t, open: false })),
    first: null,
    locked: false,
  };
}

export function mountMemory(root: HTMLElement, onBack: () => void): GameHandle {
  let state = fresh();
  let destroyed = false;
  let tickAcc = 0;
  let lockTimer = 0;

  const wrap = el("div", "game-root");
  const hudHost = el("div");
  const pit = el("div", "pit");
  const grid = el("div", "cell-grid");
  grid.style.gridTemplateColumns = "repeat(4, 1fr)";
  grid.style.padding = "10px";
  grid.style.height = "100%";
  const nodes = Array.from({ length: 16 }, (_, i) => {
    const c = el("div", "cell");
    c.style.cursor = "pointer";
    c.style.fontSize = "1.4rem";
    c.addEventListener("click", () => {
      if (state.locked) return;
      state = flip(state, i);
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
        state = fresh();
        lockTimer = 0;
        render();
      },
      true,
    ),
  );
  const tip = el("p", "pit-hint", "Match the pairs. Lower time wins.");
  tip.style.position = "static";
  tip.style.marginTop = "8px";
  wrap.append(hudHost, pit, controls, tip);
  root.replaceChildren(wrap);

  const offer = () => {
    if (state.scoreOffered) return;
    state = { ...state, scoreOffered: true };
    const score = Math.max(1, state.elapsed);
    maybeOfferScore(pit, {
      boardKey: "memory",
      score,
      lowerBetter: true,
      headline: "Matched",
      detail: formatScore(score, true),
      onDone: () => {
        if (destroyed) return;
        endOverlay(pit, {
          title: "Matched",
          detail: formatScore(score, true),
          onRetry: () => {
            state = fresh();
            lockTimer = 0;
            render();
          },
        });
      },
    });
  };

  const render = () => {
    const best = scores.best("memory", true);
    hudHost.replaceChildren(
      hud({
        title: "MEMORY",
        subtitle: `best ${best ? formatScore(best.score, true) : "—"}`,
        value: formatScore(state.elapsed, true),
        valueLabel: `${state.misses} misses`,
        onBack,
      }),
    );
    nodes.forEach((node, i) => {
      const t = state.tiles[i]!;
      if (t.open || t.matched) {
        node.style.background = FACES[t.pair]!;
        node.style.color = "var(--void)";
        node.textContent = GLYPHS[t.pair]!;
        node.style.opacity = t.matched ? "0.55" : "1";
      } else {
        node.style.background = "var(--pad)";
        node.style.color = "var(--dim)";
        node.textContent = "?";
        node.style.opacity = "1";
      }
    });
    if (state.phase === "won") offer();
    else clearOverlays(pit);
  };

  const stop = frameLoop((dt) => {
    if (state.phase === "playing") {
      tickAcc += dt;
      if (tickAcc >= 1) {
        tickAcc -= 1;
        state = { ...state, elapsed: state.elapsed + 1 };
        render();
      }
    }
    if (state.locked) {
      lockTimer += dt;
      if (lockTimer >= 0.52) {
        lockTimer = 0;
        state = resolveMismatch(state);
        render();
      }
    } else lockTimer = 0;
  });

  render();
  return {
    destroy: () => {
      destroyed = true;
      stop();
      root.replaceChildren();
    },
  };
}
