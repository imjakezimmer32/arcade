import type { Dir } from "../core/catalog";
import { DX, DY } from "../core/catalog";
import { bindSwipe, el, frameLoop, hud, leafButton, maybeOfferScore, type GameHandle } from "../core/ui";

const COLS = 9;
const ROWS = 10;
const FROG_W = 0.09;
const WATER = new Set([1, 2, 3]);
const ROAD = new Set([5, 6, 7, 8]);
const HOME_COLS = [0, 2, 4, 6, 8];
const C = {
  void: "#070A08",
  pit: "#0C1410",
  leaf: "#3DDC84",
  head: "#C8FF7A",
  bite: "#FF5A6A",
  mist: "#D7F5E3",
  dim: "#7FA88F",
  pad: "#15241C",
  pond: "#143D4A",
  moss: "#1A3A28",
};

type Hopper = { row: number; x: number; w: number; vx: number; water: boolean };
type Phase = "ready" | "running" | "paused" | "dead";

function spawn(wave: number): Hopper[] {
  const mul = 0.82 + wave * 0.08;
  const lane = (row: number, water: boolean, vx: number, count: number, w: number) =>
    Array.from({ length: count }, (_, i) => ({
      row,
      x: i * (1 / count) + 0.02,
      w,
      vx: vx * mul,
      water,
    }));
  return [
    ...lane(1, true, 0.18, 3, 0.28),
    ...lane(2, true, -0.22, 3, 0.32),
    ...lane(3, true, 0.16, 2, 0.38),
    ...lane(5, false, -0.26, 3, 0.18),
    ...lane(6, false, 0.3, 3, 0.16),
    ...lane(7, false, -0.22, 4, 0.14),
    ...lane(8, false, 0.34, 3, 0.18),
  ];
}

function rideOn(h: Hopper, frogX: number) {
  const half = FROG_W / 2;
  const hit = (lx: number) => frogX + half > lx && frogX - half < lx + h.w;
  return hit(h.x) || hit(h.x - 1.5) || hit(h.x + 1.5);
}

function fresh() {
  return {
    col: 4,
    row: ROWS - 1,
    ride: 0.5,
    homes: [false, false, false, false, false],
    traffic: spawn(1),
    lives: 3,
    score: 0,
    wave: 1,
    hopLock: 0,
    invuln: 0,
    phase: "ready" as Phase,
    offered: false,
  };
}

export function mountHop(root: HTMLElement, onBack: () => void): GameHandle {
  let s = fresh();
  let destroyed = false;

  const wrap = el("div", "game-root");
  const pit = el("div", "pit");
  const canvas = document.createElement("canvas");
  const hint = el("div", "pit-hint", "arrows / swipe to hop home");
  pit.append(canvas, hint);
  wrap.append(pit);
  root.replaceChildren(wrap);

  const paintHud = () => {
    wrap.replaceChildren(
      hud({
        title: "HOP",
        subtitle: `wave ${s.wave} · ❤ ${s.lives}`,
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
    );
  };
  paintHud();

  const hurt = () => {
    const left = s.lives - 1;
    if (left <= 0) {
      s = { ...s, lives: 0, phase: "dead" };
      end();
    } else {
      s = { ...s, lives: left, col: 4, row: ROWS - 1, ride: 0.5, invuln: 1.2 };
      paintHud();
    }
  };

  const hop = (dir: Dir) => {
    if (s.phase === "dead" || s.phase === "paused" || s.hopLock > 0) return;
    if (s.phase === "ready") {
      s = { ...s, phase: "running" };
      hint.remove();
    }
    const nc = Math.min(COLS - 1, Math.max(0, s.col + DX[dir]));
    const nr = Math.min(ROWS - 1, Math.max(0, s.row + DY[dir]));
    const cellMid = (nc + 0.5) / COLS;
    const rideX = nr === s.row ? s.ride + DX[dir] / COLS : cellMid;
    let landed = {
      ...s,
      col: nc,
      row: nr,
      ride: Math.min(0.95, Math.max(0.05, rideX)),
      hopLock: 0.12,
      phase: "running" as Phase,
    };
    if (WATER.has(nr)) {
      const log = s.traffic.find((it) => it.water && it.row === nr && rideOn(it, landed.ride));
      if (!log) {
        s = landed;
        hurt();
        return;
      }
      landed = {
        ...landed,
        ride: Math.min(log.x + log.w - 0.02, Math.max(log.x + 0.02, landed.ride)),
      };
    }
    if (ROAD.has(nr) && s.invuln <= 0 && s.traffic.some((it) => !it.water && it.row === nr && rideOn(it, landed.ride))) {
      s = landed;
      hurt();
      return;
    }
    s = landed;
  };

  const end = () => {
    if (s.offered) return;
    s = { ...s, offered: true };
    paintHud();
    maybeOfferScore(pit, {
      boardKey: "hop",
      score: s.score,
      lowerBetter: false,
      headline: "SPLASH",
      detail: `Score ${s.score}`,
      onDone: () => {
        const ov = el("div", "overlay");
        ov.append(el("h2", undefined, "DEAD"), el("p", undefined, `Score ${s.score}`));
        const row = el("div", "actions");
        row.append(
          leafButton("AGAIN", () => {
            s = fresh();
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

  const step = (dt: number) => {
    if (s.phase !== "running") return;
    const moved = s.traffic.map((h) => {
      let x = h.x + h.vx * dt;
      if (x > 1.25) x -= 1.5;
      if (x < -0.45) x += 1.5;
      return { ...h, x };
    });
    let x = s.ride;
    let c = s.col;
    if (WATER.has(s.row)) {
      const log = moved.find((it) => it.water && it.row === s.row && rideOn(it, x));
      if (log) {
        x = x + log.vx * dt;
        if (x < 0.04 || x > 0.96) {
          s = { ...s, traffic: moved, ride: x, col: c };
          hurt();
          return;
        }
        c = Math.min(COLS - 1, Math.max(0, (x * COLS) | 0));
      }
    }
    let next = {
      ...s,
      traffic: moved,
      ride: x,
      col: c,
      hopLock: Math.max(0, s.hopLock - dt),
      invuln: Math.max(0, s.invuln - dt),
    };
    if (next.row === 0) {
      const hi = HOME_COLS.indexOf(next.col);
      if (hi >= 0 && !next.homes[hi]) {
        const homes = next.homes.slice();
        homes[hi] = true;
        const cleared = homes.every(Boolean);
        s = {
          ...next,
          homes: cleared ? [false, false, false, false, false] : homes,
          col: 4,
          row: ROWS - 1,
          ride: 0.5,
          score: s.score + (cleared ? 200 * s.wave : 100),
          wave: cleared ? s.wave + 1 : s.wave,
          traffic: cleared ? spawn(s.wave + 1) : moved,
          invuln: 0.5,
        };
        paintHud();
        return;
      }
      s = next;
      hurt();
      return;
    }
    if (next.invuln > 0) {
      s = next;
      return;
    }
    if (WATER.has(next.row) && !moved.some((it) => it.water && it.row === next.row && rideOn(it, next.ride))) {
      s = next;
      hurt();
      return;
    }
    if (ROAD.has(next.row) && moved.some((it) => !it.water && it.row === next.row && rideOn(it, next.ride))) {
      s = next;
      hurt();
      return;
    }
    s = next;
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
    const rh = h / ROWS;
    for (let r = 0; r < ROWS; r++) {
      ctx.fillStyle = r === 0 ? C.moss : WATER.has(r) ? C.pond : ROAD.has(r) ? C.void : C.pad;
      ctx.fillRect(0, r * rh, w, rh + 1);
    }
    for (let i = 0; i < 5; i++) {
      const col = HOME_COLS[i]!;
      ctx.fillStyle = s.homes[i] ? C.leaf : C.dim;
      ctx.fillRect(((col + 0.15) / COLS) * w, rh * 0.15, (0.7 / COLS) * w, rh * 0.7);
    }
    for (const t of s.traffic) {
      ctx.fillStyle = t.water ? "#2A6B4A" : C.bite;
      ctx.fillRect(t.x * w, t.row * rh + rh * 0.2, t.w * w, rh * 0.6);
    }
    ctx.globalAlpha = s.invuln > 0 ? 0.5 : 1;
    ctx.fillStyle = C.head;
    ctx.beginPath();
    ctx.arc(s.ride * w, (s.row + 0.5) * rh, FROG_W * w * 0.55, 0, Math.PI * 2);
    ctx.fill();
    ctx.globalAlpha = 1;
  };

  bindSwipe(pit, hop, () => hop("up"));
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
      hop(d);
    }
  };
  window.addEventListener("keydown", onKey);

  const stop = frameLoop((dt) => {
    if (destroyed) return;
    step(dt);
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
