import { el, frameLoop, hud, leafButton, maybeOfferScore, type GameHandle } from "../core/ui";

const SHIP_R = 0.042;
const SHOT_R = 0.014;
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

type Rock = { x: number; y: number; vx: number; vy: number; r: number; stage: number };
type Shot = { x: number; y: number; vx: number; vy: number; life: number };
type Phase = "ready" | "running" | "paused" | "dead";

function wrapPos(v: number) {
  if (v < 0) return v + 1;
  if (v > 1) return v - 1;
  return v;
}

function dist2(ax: number, ay: number, bx: number, by: number) {
  let dx = ax - bx;
  let dy = ay - by;
  if (dx > 0.5) dx -= 1;
  if (dx < -0.5) dx += 1;
  if (dy > 0.5) dy -= 1;
  if (dy < -0.5) dy += 1;
  return dx * dx + dy * dy;
}

function field(wave: number): Rock[] {
  return Array.from({ length: 3 + wave }, () => {
    const ang = Math.random() * 6.28;
    return {
      x: Math.random() < 0.5 ? 0.08 : 0.92,
      y: Math.random(),
      vx: Math.cos(ang) * (0.08 + wave * 0.02),
      vy: Math.sin(ang) * (0.08 + wave * 0.02),
      r: 0.08,
      stage: 3,
    };
  });
}

function fresh() {
  return {
    x: 0.5,
    y: 0.55,
    vx: 0,
    vy: 0,
    ang: -1.57,
    thrusting: false,
    aimX: 0.5,
    aimY: 0.35,
    cooldown: 0,
    rocks: field(1),
    shots: [] as Shot[],
    lives: 3,
    score: 0,
    wave: 1,
    invuln: 1.2,
    phase: "ready" as Phase,
    offered: false,
    keys: { left: false, right: false, up: false, fire: false },
  };
}

export function mountRocks(root: HTMLElement, onBack: () => void): GameHandle {
  let s = fresh();
  let destroyed = false;
  let pointerDown = false;

  const wrap = el("div", "game-root");
  const pit = el("div", "pit");
  const canvas = document.createElement("canvas");
  const hint = el("div", "pit-hint", "WASD / arrows · space fire · or hold+aim");
  pit.append(canvas, hint);
  wrap.append(pit);
  root.replaceChildren(wrap);

  const paintHud = () => {
    wrap.replaceChildren(
      hud({
        title: "ROCKS",
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

  const fire = () => {
    if (s.phase === "dead" || s.cooldown > 0) return;
    if (s.phase === "ready") {
      s = { ...s, phase: "running" };
      hint.remove();
    }
    const sp = 0.62;
    s = {
      ...s,
      shots: [...s.shots, { x: s.x, y: s.y, vx: Math.cos(s.ang) * sp, vy: Math.sin(s.ang) * sp, life: 0.7 }],
      cooldown: 0.22,
      phase: "running",
    };
  };

  const end = () => {
    if (s.offered) return;
    s = { ...s, offered: true };
    paintHud();
    maybeOfferScore(pit, {
      boardKey: "rocks",
      score: s.score,
      lowerBetter: false,
      headline: "SHATTERED",
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
    if (s.keys.left) s = { ...s, ang: s.ang - 3.2 * dt, phase: s.phase === "ready" ? "running" : s.phase };
    if (s.keys.right) s = { ...s, ang: s.ang + 3.2 * dt, phase: s.phase === "ready" ? "running" : s.phase };
    if (s.keys.up) s = { ...s, thrusting: true, phase: s.phase === "ready" ? "running" : s.phase };
    else if (!pointerDown) s = { ...s, thrusting: false };
    if (s.keys.fire) fire();

    if (s.phase !== "running") return;
    if (s.phase === "running" && (s.keys.left || s.keys.right || s.keys.up || pointerDown)) hint.remove();

    const want = Math.atan2(s.aimY - s.y, s.aimX - s.x);
    let d = want - s.ang;
    while (d > Math.PI) d -= Math.PI * 2;
    while (d < -Math.PI) d += Math.PI * 2;
    const turn = Math.min(8 * dt, Math.abs(d)) * (d >= 0 ? 1 : -1);
    const nang = s.thrusting && pointerDown ? s.ang + turn : s.ang;
    const acc = s.thrusting ? 0.62 : 0;
    let nvx = (s.vx + Math.cos(nang) * acc * dt) * 0.988;
    let nvy = (s.vy + Math.sin(nang) * acc * dt) * 0.988;
    const speed = Math.hypot(nvx, nvy);
    if (speed > 0.85) {
      nvx = (nvx / speed) * 0.85;
      nvy = (nvy / speed) * 0.85;
    }
    let nx = wrapPos(s.x + nvx * dt);
    let ny = wrapPos(s.y + nvy * dt);
    const shots2 = s.shots
      .map((it) => ({ ...it, x: wrapPos(it.x + it.vx * dt), y: wrapPos(it.y + it.vy * dt), life: it.life - dt }))
      .filter((it) => it.life > 0);
    const rocks2 = s.rocks.map((r) => ({ ...r, x: wrapPos(r.x + r.vx * dt), y: wrapPos(r.y + r.vy * dt) }));
    const keptShots: Shot[] = [];
    let nextScore = s.score;
    const spawned: Rock[] = [];
    const living = rocks2.slice();
    for (const shot of shots2) {
      const hit = living.findIndex((it) => dist2(shot.x, shot.y, it.x, it.y) < (it.r + SHOT_R) ** 2);
      if (hit < 0) keptShots.push(shot);
      else {
        const r = living.splice(hit, 1)[0]!;
        nextScore += r.stage === 3 ? 20 : r.stage === 2 ? 50 : 100;
        if (r.stage > 1) {
          const nr = r.r * 0.62;
          const st = r.stage - 1;
          const a = Math.random() * 6.28;
          spawned.push({ x: r.x, y: r.y, vx: Math.cos(a) * 0.16, vy: Math.sin(a) * 0.16, r: nr, stage: st });
          spawned.push({ x: r.x, y: r.y, vx: -Math.cos(a) * 0.16, vy: -Math.sin(a) * 0.16, r: nr, stage: st });
        }
      }
    }
    living.push(...spawned);
    let lives2 = s.lives;
    let inv = Math.max(0, s.invuln - dt);
    let dead = false;
    if (inv <= 0) {
      const crash = living.some((it) => {
        const body = it.r * 0.88 + SHIP_R;
        return dist2(nx, ny, it.x, it.y) < body * body;
      });
      if (crash) {
        lives2 -= 1;
        if (lives2 <= 0) dead = true;
        else {
          nx = 0.5;
          ny = 0.55;
          nvx = 0;
          nvy = 0;
          inv = 2;
        }
      }
    }
    const waveClear = living.length === 0;
    const nextRocks = waveClear ? field(s.wave + 1) : living;
    const newScore = nextScore + (waveClear ? 150 * s.wave : 0);
    const hudDirty = newScore !== s.score || lives2 !== s.lives || waveClear;
    s = {
      ...s,
      x: nx,
      y: ny,
      vx: nvx,
      vy: nvy,
      ang: nang,
      cooldown: Math.max(0, s.cooldown - dt),
      rocks: nextRocks,
      shots: keptShots,
      lives: Math.max(0, lives2),
      score: newScore,
      wave: waveClear ? s.wave + 1 : s.wave,
      invuln: waveClear ? 1.2 : inv,
      phase: dead ? "dead" : s.phase,
    };
    if (hudDirty) paintHud();
    if (dead) end();
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
    for (const r of s.rocks) {
      ctx.strokeStyle = C.mist;
      ctx.lineWidth = 2;
      ctx.beginPath();
      ctx.arc(r.x * w, r.y * h, r.r * w, 0, Math.PI * 2);
      ctx.stroke();
    }
    for (const shot of s.shots) {
      ctx.fillStyle = C.head;
      ctx.beginPath();
      ctx.arc(shot.x * w, shot.y * h, SHOT_R * w, 0, Math.PI * 2);
      ctx.fill();
    }
    ctx.save();
    ctx.translate(s.x * w, s.y * h);
    ctx.rotate(s.ang);
    ctx.globalAlpha = s.invuln > 0 ? 0.45 + 0.55 * Math.abs(Math.sin(performance.now() / 80)) : 1;
    ctx.fillStyle = C.leaf;
    ctx.beginPath();
    ctx.moveTo(SHIP_R * w, 0);
    ctx.lineTo(-SHIP_R * w * 0.7, SHIP_R * w * 0.7);
    ctx.lineTo(-SHIP_R * w * 0.4, 0);
    ctx.lineTo(-SHIP_R * w * 0.7, -SHIP_R * w * 0.7);
    ctx.closePath();
    ctx.fill();
    if (s.thrusting) {
      ctx.fillStyle = C.bite;
      ctx.beginPath();
      ctx.moveTo(-SHIP_R * w * 0.4, 0);
      ctx.lineTo(-SHIP_R * w * 1.2, SHIP_R * w * 0.35);
      ctx.lineTo(-SHIP_R * w * 1.2, -SHIP_R * w * 0.35);
      ctx.fill();
    }
    ctx.restore();
  };

  const aim = (e: PointerEvent) => {
    const rect = pit.getBoundingClientRect();
    s = {
      ...s,
      aimX: Math.min(1, Math.max(0, (e.clientX - rect.left) / rect.width)),
      aimY: Math.min(1, Math.max(0, (e.clientY - rect.top) / rect.height)),
      thrusting: pointerDown,
      phase: s.phase === "ready" && pointerDown ? "running" : s.phase,
    };
  };
  pit.addEventListener("pointerdown", (e) => {
    pointerDown = true;
    aim(e);
    fire();
    pit.setPointerCapture(e.pointerId);
  });
  pit.addEventListener("pointermove", aim);
  pit.addEventListener("pointerup", () => {
    pointerDown = false;
    s = { ...s, thrusting: s.keys.up };
  });

  const onKey = (e: KeyboardEvent, down: boolean) => {
    const k = e.key;
    if (k === "ArrowLeft" || k === "a") s.keys.left = down;
    else if (k === "ArrowRight" || k === "d") s.keys.right = down;
    else if (k === "ArrowUp" || k === "w") s.keys.up = down;
    else if (k === " " || k === "ArrowDown" || k === "s") {
      e.preventDefault();
      s.keys.fire = down;
      if (down) fire();
    }
  };
  const kd = (e: KeyboardEvent) => onKey(e, true);
  const ku = (e: KeyboardEvent) => onKey(e, false);
  window.addEventListener("keydown", kd);
  window.addEventListener("keyup", ku);

  const stop = frameLoop((dt) => {
    if (destroyed) return;
    step(dt);
    draw();
  });

  return {
    destroy: () => {
      destroyed = true;
      stop();
      window.removeEventListener("keydown", kd);
      window.removeEventListener("keyup", ku);
      root.replaceChildren();
    },
  };
}
