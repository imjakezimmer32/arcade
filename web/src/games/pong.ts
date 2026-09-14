import { el, frameLoop, hud, leafButton, maybeOfferScore, type GameHandle } from "../core/ui";

const PAD = 0.28;
const PAD_H = 0.028;
const LEAF_Y = 0.88;
const HEAD_Y = 0.08;
const BALL = 0.018;
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

type Phase = "ready" | "running" | "paused" | "dead";

function clampVel(vx: number, vy: number): [number, number] {
  let x = vx;
  let y = vy;
  if (Math.abs(y) < 0.36) y = 0.36 * (y === 0 ? -1 : Math.sign(y));
  const speed = Math.hypot(x, y);
  const target = Math.min(1.05, Math.max(0.48, speed));
  if (speed > 0.001) {
    x = (x / speed) * target;
    y = (y / speed) * target;
  }
  return [x, y];
}

function fresh() {
  return {
    px: 0.5,
    cpu: 0.5,
    bx: 0.5,
    by: 0.72,
    vx: 0.22,
    vy: -0.48,
    score: 0,
    lives: 3,
    serve: 0,
    phase: "ready" as Phase,
    offered: false,
  };
}

export function mountPong(root: HTMLElement, onBack: () => void): GameHandle {
  let s = fresh();
  let destroyed = false;

  const wrap = el("div", "game-root");
  const pit = el("div", "pit");
  const canvas = document.createElement("canvas");
  const hint = el("div", "pit-hint", "move paddle · beat the moss");
  pit.append(canvas, hint);
  wrap.append(pit);
  root.replaceChildren(wrap);

  const paintHud = () => {
    wrap.replaceChildren(
      hud({
        title: "PONG",
        subtitle: `❤ ${s.lives}`,
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

  const moveLeaf = (x: number) => {
    const nx = Math.min(1 - PAD / 2, Math.max(PAD / 2, x));
    if (s.phase === "ready") {
      s = { ...s, px: nx, bx: nx, phase: "running", serve: 0.35 };
      hint.remove();
    } else s = { ...s, px: nx };
  };

  const end = () => {
    if (s.offered) return;
    s = { ...s, offered: true };
    paintHud();
    maybeOfferScore(pit, {
      boardKey: "pong",
      score: s.score,
      lowerBetter: false,
      headline: "RALLY OVER",
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
    if (s.serve > 0) {
      s = { ...s, serve: s.serve - dt, bx: s.px, by: 0.78 };
      return;
    }
    let x = s.bx + s.vx * dt;
    let y = s.by + s.vy * dt;
    let nvx = s.vx;
    let nvy = s.vy;
    if (x < BALL) {
      x = BALL;
      nvx = Math.abs(nvx);
    }
    if (x > 1 - BALL) {
      x = 1 - BALL;
      nvx = -Math.abs(nvx);
    }
    const half = PAD / 2;
    const cpuSpeed = Math.min(0.72, 0.42 + s.score * 0.012);
    const aim = x + (nvy < 0 ? (s.px - 0.5) * 0.04 : 0);
    const ncpu = Math.min(1 - half, Math.max(half, s.cpu + Math.min(cpuSpeed * dt, Math.max(-cpuSpeed * dt, aim - s.cpu))));

    if (
      nvy < 0 &&
      y - BALL <= HEAD_Y + PAD_H &&
      y + BALL >= HEAD_Y &&
      x >= ncpu - half - BALL &&
      x <= ncpu + half + BALL
    ) {
      y = HEAD_Y + PAD_H + BALL;
      const hit = Math.min(1, Math.max(-1, (x - ncpu) / half));
      nvx = hit * 0.62;
      nvy = Math.max(0.42, Math.abs(nvy)) * 1.03;
      [nvx, nvy] = clampVel(nvx, nvy);
      s = { ...s, bx: x, by: y, vx: nvx, vy: nvy, cpu: ncpu };
      return;
    }
    if (
      nvy > 0 &&
      y + BALL >= LEAF_Y &&
      y - BALL <= LEAF_Y + PAD_H &&
      x >= s.px - half - BALL &&
      x <= s.px + half + BALL
    ) {
      y = LEAF_Y - BALL;
      const hit = Math.min(1, Math.max(-1, (x - s.px) / half));
      nvx = hit * 0.72;
      nvy = -Math.max(0.42, Math.abs(nvy)) * 1.035;
      [nvx, nvy] = clampVel(nvx, nvy);
      s = { ...s, bx: x, by: y, vx: nvx, vy: nvy, cpu: ncpu };
      return;
    }
    if (y < -0.02) {
      s = { ...s, bx: s.px, by: 0.78, vx: 0.2, vy: -0.52, cpu: ncpu, score: s.score + 1, serve: 0.6 };
      paintHud();
      return;
    }
    if (y > 1.04) {
      const left = s.lives - 1;
      if (left <= 0) {
        s = { ...s, by: y, lives: 0, phase: "dead" };
        end();
        return;
      }
      s = { ...s, bx: s.px, by: 0.78, vx: 0.2, vy: -0.52, cpu: ncpu, lives: left, serve: 0.7 };
      paintHud();
      return;
    }
    s = { ...s, bx: x, by: y, vx: nvx, vy: nvy, cpu: ncpu };
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
    ctx.strokeStyle = C.dim;
    ctx.setLineDash([6, 8]);
    ctx.beginPath();
    ctx.moveTo(0, h * 0.5);
    ctx.lineTo(w, h * 0.5);
    ctx.stroke();
    ctx.setLineDash([]);
    ctx.fillStyle = C.head;
    ctx.fillRect((s.cpu - PAD / 2) * w, HEAD_Y * h, PAD * w, PAD_H * h);
    ctx.fillStyle = C.leaf;
    ctx.fillRect((s.px - PAD / 2) * w, LEAF_Y * h, PAD * w, PAD_H * h);
    ctx.fillStyle = C.mist;
    ctx.beginPath();
    ctx.arc(s.bx * w, s.by * h, BALL * w, 0, Math.PI * 2);
    ctx.fill();
  };

  const ptr = (e: PointerEvent) => {
    const rect = pit.getBoundingClientRect();
    moveLeaf((e.clientX - rect.left) / rect.width);
  };
  pit.addEventListener("pointerdown", (e) => {
    ptr(e);
    pit.setPointerCapture(e.pointerId);
  });
  pit.addEventListener("pointermove", ptr);

  const stop = frameLoop((dt) => {
    if (destroyed) return;
    step(dt);
    draw();
  });

  return {
    destroy: () => {
      destroyed = true;
      stop();
      root.replaceChildren();
    },
  };
}
