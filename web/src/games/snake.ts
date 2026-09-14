import type { Dir } from "../core/catalog";
import { DX, DY, opposite } from "../core/catalog";
import {
  bindSwipe,
  el,
  frameLoop,
  hud,
  leafButton,
  maybeOfferScore,
  type GameHandle,
} from "../core/ui";

const COLS = 17;
const ROWS = 21;
const START_MS = 155;
const MIN_MS = 70;
const C = {
  void: "#070A08",
  pit: "#0C1410",
  leaf: "#3DDC84",
  head: "#C8FF7A",
  bite: "#FF5A6A",
  mist: "#D7F5E3",
  dim: "#7FA88F",
  pad: "#15241C",
};

type Cell = { x: number; y: number };
type Phase = "ready" | "running" | "paused" | "dead" | "won";

function eq(a: Cell, b: Cell) {
  return a.x === b.x && a.y === b.y;
}

function spawnFood(snake: Cell[]): Cell {
  const blocked = new Set(snake.map((c) => `${c.x},${c.y}`));
  const open: Cell[] = [];
  for (let y = 0; y < ROWS; y++)
    for (let x = 0; x < COLS; x++) if (!blocked.has(`${x},${y}`)) open.push({ x, y });
  return open[Math.floor(Math.random() * open.length)]!;
}

function fresh() {
  const start = { x: (COLS / 2) | 0, y: (ROWS / 2) | 0 };
  const snake = [start, { x: start.x - 1, y: start.y }, { x: start.x - 2, y: start.y }];
  return {
    snake,
    dir: "right" as Dir,
    queued: "right" as Dir,
    queued2: null as Dir | null,
    food: spawnFood(snake),
    score: 0,
    phase: "ready" as Phase,
    tickMs: START_MS,
    offered: false,
  };
}

export function mountSnake(root: HTMLElement, onBack: () => void): GameHandle {
  let s = fresh();
  let acc = 0;
  let destroyed = false;

  const wrap = el("div", "game-root");
  const pit = el("div", "pit");
  const canvas = document.createElement("canvas");
  const hint = el("div", "pit-hint", "swipe or arrows · tap to start");
  pit.append(canvas, hint);
  wrap.append(pit);
  root.replaceChildren(wrap);

  const paintHud = () => {
    const bar = hud({
      title: "SNAKE",
      subtitle: s.phase === "paused" ? "paused" : "solo",
      value: String(s.score),
      valueLabel: "SCORE",
      onBack,
      onPause: () => {
        if (s.phase === "running") s = { ...s, phase: "paused" };
        else if (s.phase === "paused") s = { ...s, phase: "running" };
        paintHud();
      },
      paused: s.phase === "paused",
    });
    wrap.replaceChildren(bar, pit);
  };
  paintHud();

  const queue = (next: Dir) => {
    if (s.phase !== "running" && s.phase !== "ready") return;
    const against = s.queued !== s.dir ? s.queued : s.dir;
    if (opposite(next, against)) return;
    const phase = s.phase === "ready" ? "running" : s.phase;
    if (s.queued === s.dir) s = { ...s, queued: next, phase };
    else s = { ...s, queued2: next, phase };
    hint.remove();
  };

  const step = () => {
    if (s.phase !== "running") return;
    const facing = opposite(s.queued, s.dir) ? s.dir : s.queued;
    const nextQ = s.queued2 ?? facing;
    const head = s.snake[0]!;
    const next = { x: head.x + DX[facing], y: head.y + DY[facing] };
    const die = () => {
      s = { ...s, dir: facing, queued: facing, queued2: null, phase: "dead" };
      end();
    };
    if (next.x < 0 || next.x >= COLS || next.y < 0 || next.y >= ROWS) return die();
    const eating = eq(next, s.food);
    const body = eating ? s.snake : s.snake.slice(0, -1);
    if (body.some((c) => eq(c, next))) return die();
    const grown = [next, ...body];
    if (eating) {
      const blocked = new Set(grown.map((c) => `${c.x},${c.y}`));
      const open: Cell[] = [];
      for (let y = 0; y < ROWS; y++)
        for (let x = 0; x < COLS; x++) if (!blocked.has(`${x},${y}`)) open.push({ x, y });
      const score = s.score + 1;
      if (!open.length) {
        s = { ...s, snake: grown, dir: facing, queued: nextQ, queued2: null, score, phase: "won" };
        end(true);
        return;
      }
      s = {
        ...s,
        snake: grown,
        dir: facing,
        queued: nextQ,
        queued2: null,
        food: open[Math.floor(Math.random() * open.length)]!,
        score,
        tickMs: Math.max(MIN_MS, START_MS - score * 4),
      };
      paintHud();
      return;
    }
    s = { ...s, snake: grown, dir: facing, queued: nextQ, queued2: null };
  };

  const end = (win = false) => {
    if (s.offered) return;
    s = { ...s, offered: true };
    paintHud();
    draw();
    maybeOfferScore(pit, {
      boardKey: "snake",
      score: s.score,
      lowerBetter: false,
      headline: win ? "GARDEN FULL" : "BITTEN",
      detail: `Score ${s.score}`,
      win: true,
      onDone: () => {
        const ov = el("div", "overlay");
        ov.append(el("h2", undefined, win ? "WIN" : "DEAD"), el("p", undefined, `Score ${s.score}`));
        const row = el("div", "actions");
        row.append(
          leafButton("AGAIN", () => {
            s = fresh();
            acc = 0;
            pit.replaceChildren(canvas, hint);
            paintHud();
          }),
          leafButton("BACK", onBack, true),
        );
        ov.append(row);
        pit.append(ov);
      },
    });
  };

  const draw = () => {
    const dpr = devicePixelRatio || 1;
    const w = pit.clientWidth;
    const h = pit.clientHeight;
    if (w < 2 || h < 2) return;
    canvas.width = w * dpr;
    canvas.height = h * dpr;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.fillStyle = C.pit;
    ctx.fillRect(0, 0, w, h);
    const pad = 8;
    const cw = (w - pad * 2) / COLS;
    const ch = (h - pad * 2) / ROWS;
    const cell = Math.min(cw, ch);
    const ox = (w - cell * COLS) / 2;
    const oy = (h - cell * ROWS) / 2;
    ctx.fillStyle = C.void;
    ctx.fillRect(ox, oy, cell * COLS, cell * ROWS);
    for (let i = s.snake.length - 1; i >= 0; i--) {
      const c = s.snake[i]!;
      ctx.fillStyle = i === 0 ? C.head : C.leaf;
      const r = cell * (i === 0 ? 0.42 : 0.36);
      ctx.beginPath();
      ctx.arc(ox + (c.x + 0.5) * cell, oy + (c.y + 0.5) * cell, r, 0, Math.PI * 2);
      ctx.fill();
    }
    ctx.fillStyle = C.bite;
    ctx.beginPath();
    ctx.arc(ox + (s.food.x + 0.5) * cell, oy + (s.food.y + 0.5) * cell, cell * 0.28, 0, Math.PI * 2);
    ctx.fill();
  };

  const onKey = (e: KeyboardEvent) => {
    const map: Record<string, Dir> = {
      ArrowUp: "up",
      ArrowDown: "down",
      ArrowLeft: "left",
      ArrowRight: "right",
      w: "up",
      s: "down",
      a: "left",
      d: "right",
    };
    const d = map[e.key] ?? map[e.key.toLowerCase()];
    if (d) {
      e.preventDefault();
      queue(d);
    } else if (e.key === " " || e.key === "Enter") {
      if (s.phase === "ready") s = { ...s, phase: "running" };
      else if (s.phase === "paused") s = { ...s, phase: "running" };
      hint.remove();
    }
  };
  window.addEventListener("keydown", onKey);
  bindSwipe(pit, queue, () => {
    if (s.phase === "ready") {
      s = { ...s, phase: "running" };
      hint.remove();
    }
  });

  const stop = frameLoop((dt) => {
    if (destroyed) return;
    if (s.phase === "running") {
      acc += dt * 1000;
      while (acc >= s.tickMs) {
        acc -= s.tickMs;
        step();
        if (s.phase !== "running") break;
      }
    }
    draw();
  });

  return {
    destroy: () => {
      destroyed = true;
      stop();
      window.removeEventListener("keydown", onKey);
      root.replaceChildren();
    },
  };
}
