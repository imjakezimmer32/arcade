import { el, frameLoop, hud, leafButton, maybeOfferScore, type GameHandle } from "../core/ui";

const W = 10;
const H = 20;
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
const COLORS = ["", C.leaf, C.head, "#8BE8B0", "#E8F5C8", "#7FDBFF", "#FFB86B", "#C792EA"];

type Phase = "ready" | "running" | "paused" | "dead";
type Piece = { type: number; rot: number; x: number; y: number };

const TETS: [number, number][][][] = [
  [
    [
      [0, 1],
      [1, 1],
      [2, 1],
      [3, 1],
    ],
    [
      [2, 0],
      [2, 1],
      [2, 2],
      [2, 3],
    ],
    [
      [0, 2],
      [1, 2],
      [2, 2],
      [3, 2],
    ],
    [
      [1, 0],
      [1, 1],
      [1, 2],
      [1, 3],
    ],
  ],
  [
    [
      [1, 0],
      [2, 0],
      [1, 1],
      [2, 1],
    ],
  ],
  [
    [
      [1, 0],
      [0, 1],
      [1, 1],
      [2, 1],
    ],
    [
      [1, 0],
      [1, 1],
      [2, 1],
      [1, 2],
    ],
    [
      [0, 1],
      [1, 1],
      [2, 1],
      [1, 2],
    ],
    [
      [1, 0],
      [0, 1],
      [1, 1],
      [1, 2],
    ],
  ],
  [
    [
      [1, 0],
      [2, 0],
      [0, 1],
      [1, 1],
    ],
    [
      [1, 0],
      [1, 1],
      [2, 1],
      [2, 2],
    ],
  ],
  [
    [
      [0, 0],
      [1, 0],
      [1, 1],
      [2, 1],
    ],
    [
      [2, 0],
      [1, 1],
      [2, 1],
      [1, 2],
    ],
  ],
  [
    [
      [0, 0],
      [0, 1],
      [1, 1],
      [2, 1],
    ],
    [
      [1, 0],
      [2, 0],
      [1, 1],
      [1, 2],
    ],
    [
      [0, 1],
      [1, 1],
      [2, 1],
      [2, 2],
    ],
    [
      [1, 0],
      [1, 1],
      [0, 2],
      [1, 2],
    ],
  ],
  [
    [
      [2, 0],
      [0, 1],
      [1, 1],
      [2, 1],
    ],
    [
      [1, 0],
      [1, 1],
      [1, 2],
      [2, 2],
    ],
    [
      [0, 1],
      [1, 1],
      [2, 1],
      [0, 2],
    ],
    [
      [0, 0],
      [1, 0],
      [1, 1],
      [1, 2],
    ],
  ],
];

function refill() {
  return [1, 2, 3, 4, 5, 6, 7].sort(() => Math.random() - 0.5);
}

function cellsOf(p: Piece) {
  const rot = TETS[p.type - 1]![p.rot % TETS[p.type - 1]!.length]!;
  return rot.map(([x, y]) => [p.x + x, p.y + y] as const);
}

function collides(board: Int8Array, p: Piece) {
  for (const [x, y] of cellsOf(p)) {
    if (x < 0 || x >= W || y >= H) return true;
    if (y < 0) continue;
    if (board[y * W + x]) return true;
  }
  return false;
}

function clearLines(board: Int8Array): [Int8Array, number] {
  const keep: number[][] = [];
  for (let y = 0; y < H; y++) {
    const row = Array.from({ length: W }, (_, x) => board[y * W + x]!);
    if (row.some((v) => !v)) keep.push(row);
  }
  const cleared = H - keep.length;
  const next = new Int8Array(W * H);
  keep.forEach((row, i) => row.forEach((v, x) => (next[(cleared + i) * W + x] = v)));
  return [next, cleared];
}

type State = {
  cells: Int8Array;
  active: Piece | null;
  nextType: number;
  bag: number[];
  score: number;
  lines: number;
  phase: Phase;
  tickMs: number;
  offered: boolean;
};

function spawn(s: State): State {
  let bag = s.bag.length < 8 ? [...s.bag, ...refill()] : [...s.bag];
  const type = bag.shift()!;
  const piece: Piece = { type, rot: 0, x: 3, y: 0 };
  const coming = bag[0]!;
  if (collides(s.cells, piece)) return { ...s, active: piece, nextType: coming, bag, phase: "dead" };
  return { ...s, active: piece, nextType: coming, bag };
}

function fresh(): State {
  return spawn({
    cells: new Int8Array(W * H),
    active: null,
    nextType: 1,
    bag: [...refill(), ...refill()],
    score: 0,
    lines: 0,
    phase: "ready",
    tickMs: 700,
    offered: false,
  });
}

export function mountStacks(root: HTMLElement, onBack: () => void): GameHandle {
  let s = fresh();
  let acc = 0;
  let destroyed = false;

  const wrap = el("div", "game-root");
  const pit = el("div", "pit");
  const canvas = document.createElement("canvas");
  const hint = el("div", "pit-hint", "← → rotate soft/hard drop");
  pit.append(canvas, hint);
  const controls = el("div", "controls");
  wrap.append(pit, controls);
  root.replaceChildren(wrap);

  const paintHud = () => {
    wrap.replaceChildren(
      hud({
        title: "STACKS",
        subtitle: `lines ${s.lines}`,
        value: String(s.score),
        valueLabel: "SCORE",
        onBack,
        onPause: () => {
          if (s.phase === "running") s = { ...s, phase: "paused" };
          else if (s.phase === "paused") s = { ...s, phase: "running" };
          paintHud();
        },
        paused: s.phase === "paused",
      }),
      pit,
      controls,
    );
  };
  paintHud();

  const startIfReady = () => (s.phase === "ready" ? "running" : s.phase);

  const gravity = (lock: boolean) => {
    if (s.phase === "dead") return;
    const piece = s.active;
    if (!piece) {
      s = spawn(s);
      return;
    }
    const down = { ...piece, y: piece.y + 1 };
    s = { ...s, phase: startIfReady() };
    if (!collides(s.cells, down)) {
      s = { ...s, active: down };
      return;
    }
    if (!lock) return;
    const locked = s.cells.slice();
    for (const [x, y] of cellsOf(piece)) {
      if (y >= 0 && y < H && x >= 0 && x < W) locked[y * W + x] = piece.type;
    }
    const [cleared, count] = clearLines(locked);
    const add = count === 1 ? 100 : count === 2 ? 300 : count === 3 ? 500 : count === 4 ? 800 : 0;
    s = spawn({
      ...s,
      cells: cleared,
      active: null,
      score: s.score + add + 1,
      lines: s.lines + count,
      tickMs: Math.max(120, 700 - (s.lines + count) * 18),
    });
    paintHud();
    if (s.phase === "dead") end();
  };

  const shift = (dx: number) => {
    if (!s.active || s.phase === "dead" || s.phase === "paused") return;
    const moved = { ...s.active, x: s.active.x + dx };
    if (!collides(s.cells, moved)) {
      s = { ...s, active: moved, phase: startIfReady() };
      hint.remove();
    }
  };

  const rotate = () => {
    if (!s.active || s.phase === "dead" || s.phase === "paused") return;
    const tet = TETS[s.active.type - 1]!;
    const nextRot = (s.active.rot + 1) % tet.length;
    for (const k of [0, -1, 1, -2, 2]) {
      const moved = { ...s.active, rot: nextRot, x: s.active.x + k };
      if (!collides(s.cells, moved)) {
        s = { ...s, active: moved, phase: startIfReady() };
        hint.remove();
        return;
      }
    }
  };

  const hardDrop = () => {
    if (!s.active || s.phase === "dead" || s.phase === "paused") return;
    let y = s.active.y;
    while (!collides(s.cells, { ...s.active, y: y + 1 })) y++;
    s = { ...s, active: { ...s.active, y }, phase: startIfReady() };
    gravity(true);
    hint.remove();
  };

  const end = () => {
    if (s.offered) return;
    s = { ...s, offered: true };
    paintHud();
    maybeOfferScore(pit, {
      boardKey: "stacks",
      score: s.score,
      lowerBetter: false,
      headline: "STACKED",
      detail: `Score ${s.score}`,
      onDone: () => {
        const ov = el("div", "overlay");
        ov.append(el("h2", undefined, "DEAD"), el("p", undefined, `Score ${s.score}`));
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

  controls.append(
    leafButton("←", () => shift(-1)),
    leafButton("⟳", () => rotate()),
    leafButton("→", () => shift(1)),
    leafButton("↓", () => gravity(false)),
    leafButton("⇓", () => hardDrop()),
  );

  const ghostY = () => {
    if (!s.active) return 0;
    let y = s.active.y;
    while (!collides(s.cells, { ...s.active, y: y + 1 })) y++;
    return y;
  };

  const draw = () => {
    const dpr = devicePixelRatio || 1;
    const pw = pit.clientWidth;
    const ph = pit.clientHeight;
    if (pw < 2 || ph < 2) return;
    canvas.width = pw * dpr;
    canvas.height = ph * dpr;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.fillStyle = C.pit;
    ctx.fillRect(0, 0, pw, ph);
    const cell = Math.min(pw / W, ph / H) * 0.92;
    const ox = (pw - cell * W) / 2;
    const oy = (ph - cell * H) / 2;
    ctx.fillStyle = C.void;
    ctx.fillRect(ox, oy, cell * W, cell * H);
    const paintCell = (x: number, y: number, color: string, a = 1) => {
      if (y < 0) return;
      ctx.globalAlpha = a;
      ctx.fillStyle = color;
      ctx.fillRect(ox + x * cell + 1, oy + y * cell + 1, cell - 2, cell - 2);
      ctx.globalAlpha = 1;
    };
    for (let i = 0; i < s.cells.length; i++) {
      const v = s.cells[i]!;
      if (v) paintCell(i % W, (i / W) | 0, COLORS[v]!);
    }
    if (s.active) {
      const gy = ghostY();
      for (const [x, y] of cellsOf({ ...s.active, y: gy })) paintCell(x, y, C.dim, 0.35);
      for (const [x, y] of cellsOf(s.active)) paintCell(x, y, COLORS[s.active.type]!);
    }
  };

  const onKey = (e: KeyboardEvent) => {
    const k = e.key;
    if (k === "ArrowLeft" || k === "a") {
      e.preventDefault();
      shift(-1);
    } else if (k === "ArrowRight" || k === "d") {
      e.preventDefault();
      shift(1);
    } else if (k === "ArrowUp" || k === "w" || k === "x") {
      e.preventDefault();
      rotate();
    } else if (k === "ArrowDown" || k === "s") {
      e.preventDefault();
      gravity(false);
    } else if (k === " " || k === "Enter") {
      e.preventDefault();
      hardDrop();
    }
  };
  window.addEventListener("keydown", onKey);

  const stop = frameLoop((dt) => {
    if (destroyed) return;
    if (s.phase === "running") {
      acc += dt * 1000;
      while (acc >= s.tickMs) {
        acc -= s.tickMs;
        gravity(true);
        if (s.phase !== "running") break;
      }
    } else if (s.phase === "ready") {
      // idle
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
