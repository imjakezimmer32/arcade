import { el, frameLoop, hud, leafButton, maybeOfferScore, type GameHandle } from "../core/ui";

const SHIP_Y = 0.88;
const SHIP_W = 0.1;
const SHIP_H = 0.07;
const HAZ_H = 0.05;
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

type Hazard = { x: number; y: number; w: number; vx: number };
type Phase = "ready" | "running" | "paused" | "dead";

function overlap(ax: number, ay: number, aw: number, ah: number, bx: number, by: number, bw: number, bh: number) {
  return Math.abs(ax - bx) * 2 < aw + bw && Math.abs(ay - by) * 2 < ah + bh;
}

function fresh() {
  return {
    x: 0.5,
    hazards: [] as Hazard[],
    spawn: 0,
    score: 0,
    acc: 0,
    lives: 3,
    invuln: 0,
    phase: "ready" as Phase,
    offered: false,
  };
}

export function mountDodge(root: HTMLElement, onBack: () => void): GameHandle {
  let s = fresh();
  let destroyed = false;

  const wrap = el("div", "game-root");
  const pit = el("div", "pit");
  const canvas = document.createElement("canvas");
  const hint = el("div", "pit-hint", "drag to dodge falling thorns");
  pit.append(canvas, hint);
  wrap.append(pit);
  root.replaceChildren(wrap);

  const paintHud = () => {
    wrap.replaceChildren(
      hud({
        title: "DODGE",
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

  const move = (nx: number) => {
    s = {
      ...s,
      x: Math.min(0.92, Math.max(0.08, nx)),
      phase: s.phase === "ready" ? "running" : s.phase,
    };
    hint.remove();
  };

  const end = () => {
    if (s.offered) return;
    s = { ...s, offered: true };
    paintHud();
    maybeOfferScore(pit, {
      boardKey: "dodge",
      score: s.score,
      lowerBetter: false,
      headline: "SQUASHED",
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
    const fall = Math.min(0.85, 0.38 + s.score * 0.004);
    let nextSpawn = s.spawn - dt;
    let next = s.hazards
      .map((it) => {
        let x = it.x + it.vx * dt;
        let vx = it.vx;
        if (x < 0.08 || x > 0.92) {
          x = Math.min(0.92, Math.max(0.08, x));
          vx = -vx;
        }
        return { ...it, x, vx, y: it.y + fall * dt };
      })
      .filter((it) => it.y < 1.12);
    if (nextSpawn <= 0) {
      const drift = s.score > 40 && Math.random() < 0.35 ? (Math.random() - 0.5) * 0.22 : 0;
      next = [
        ...next,
        { x: 0.08 + Math.random() * 0.84, y: -0.08, w: 0.1 + Math.random() * 0.12, vx: drift },
      ];
      nextSpawn = Math.max(0.16, 0.46 - s.score * 0.0035);
    }
    const hit = next.some((h) => overlap(h.x, h.y, h.w, HAZ_H, s.x, SHIP_Y, SHIP_W, SHIP_H));
    const nextAcc = s.acc + dt * 10;
    const add = nextAcc | 0;
    const inv = Math.max(0, s.invuln - dt);
    if (hit && inv <= 0) {
      const left = s.lives - 1;
      if (left <= 0) {
        s = { ...s, hazards: next, spawn: nextSpawn, acc: nextAcc - add, score: s.score + add, lives: 0, invuln: 0, phase: "dead" };
        end();
        return;
      }
      s = {
        ...s,
        hazards: next.filter((it) => it.y < 0.62),
        spawn: 0.35,
        acc: nextAcc - add,
        score: s.score + add,
        lives: left,
        invuln: 1.5,
      };
      paintHud();
      return;
    }
    s = { ...s, hazards: next, spawn: nextSpawn, acc: nextAcc - add, score: s.score + add, invuln: inv };
    if (add) paintHud();
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
    for (const hz of s.hazards) {
      ctx.fillStyle = C.bite;
      ctx.fillRect((hz.x - hz.w / 2) * w, (hz.y - HAZ_H / 2) * h, hz.w * w, HAZ_H * h);
    }
    ctx.globalAlpha = s.invuln > 0 ? 0.45 + 0.55 * Math.abs(Math.sin(performance.now() / 80)) : 1;
    ctx.fillStyle = C.head;
    ctx.beginPath();
    ctx.moveTo(s.x * w, (SHIP_Y - SHIP_H / 2) * h);
    ctx.lineTo((s.x + SHIP_W / 2) * w, (SHIP_Y + SHIP_H / 2) * h);
    ctx.lineTo((s.x - SHIP_W / 2) * w, (SHIP_Y + SHIP_H / 2) * h);
    ctx.closePath();
    ctx.fill();
    ctx.globalAlpha = 1;
  };

  const ptr = (e: PointerEvent) => {
    const rect = pit.getBoundingClientRect();
    move((e.clientX - rect.left) / rect.width);
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
