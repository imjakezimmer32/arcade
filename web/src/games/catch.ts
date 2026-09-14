import { el, frameLoop, hud, leafButton, maybeOfferScore, type GameHandle } from "../core/ui";

const BASKET_Y = 0.88;
const BASKET_W = 0.32;
const BASKET_H = 0.08;
const BERRY = 0.055;
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

type Berry = { x: number; y: number; vy: number; kind: number };
type Phase = "ready" | "running" | "paused" | "dead";

function overlap(ax: number, ay: number, aw: number, ah: number, bx: number, by: number, bw: number, bh: number) {
  return Math.abs(ax - bx) * 2 < aw + bw && Math.abs(ay - by) * 2 < ah + bh;
}

function fresh() {
  return {
    x: 0.5,
    berries: [] as Berry[],
    spawn: 0.3,
    lives: 3,
    score: 0,
    phase: "ready" as Phase,
    offered: false,
  };
}

export function mountCatch(root: HTMLElement, onBack: () => void): GameHandle {
  let s = fresh();
  let destroyed = false;

  const wrap = el("div", "game-root");
  const pit = el("div", "pit");
  const canvas = document.createElement("canvas");
  const hint = el("div", "pit-hint", "catch berries · avoid poison");
  pit.append(canvas, hint);
  wrap.append(pit);
  root.replaceChildren(wrap);

  const paintHud = () => {
    wrap.replaceChildren(
      hud({
        title: "CATCH",
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
      x: Math.min(0.86, Math.max(0.14, nx)),
      phase: s.phase === "ready" ? "running" : s.phase,
    };
    hint.remove();
  };

  const end = () => {
    if (s.offered) return;
    s = { ...s, offered: true };
    paintHud();
    maybeOfferScore(pit, {
      boardKey: "catch",
      score: s.score,
      lowerBetter: false,
      headline: "SPILLED",
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
    const fall = Math.min(0.72, 0.32 + s.score * 0.004);
    let nextScore = s.score;
    let nextLives = s.lives;
    const moved: Berry[] = [];
    for (const b of s.berries) {
      const ny = b.y + (b.vy + fall) * 0.5 * dt;
      const caught = overlap(b.x, ny, BERRY, BERRY, s.x, BASKET_Y, BASKET_W, BASKET_H);
      if (caught && b.kind === 2) nextLives -= 1;
      else if (caught) nextScore += b.kind === 0 ? 10 : 25;
      else if (ny > 1.08) {
        if (b.kind === 0) nextLives -= 1;
      } else moved.push({ ...b, y: ny });
    }
    let spawnIn = s.spawn - dt;
    if (spawnIn <= 0) {
      const roll = Math.random();
      const kind = roll < 0.12 ? 2 : roll < 0.3 ? 1 : 0;
      moved.push({ x: 0.12 + Math.random() * 0.76, y: -0.06, vy: fall, kind });
      spawnIn = Math.max(0.18, 0.55 - s.score * 0.003);
    }
    if (nextLives <= 0) {
      s = { ...s, berries: moved, spawn: spawnIn, score: nextScore, lives: 0, phase: "dead" };
      end();
      return;
    }
    if (nextScore !== s.score || nextLives !== s.lives) paintHud();
    s = { ...s, berries: moved, spawn: spawnIn, lives: nextLives, score: nextScore };
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
    for (const b of s.berries) {
      ctx.fillStyle = b.kind === 2 ? C.bite : b.kind === 1 ? C.head : C.leaf;
      ctx.beginPath();
      ctx.arc(b.x * w, b.y * h, (BERRY / 2) * w, 0, Math.PI * 2);
      ctx.fill();
    }
    ctx.fillStyle = C.mist;
    ctx.beginPath();
    ctx.moveTo((s.x - BASKET_W / 2) * w, BASKET_Y * h);
    ctx.lineTo((s.x + BASKET_W / 2) * w, BASKET_Y * h);
    ctx.lineTo((s.x + BASKET_W / 2 - 0.02) * w, (BASKET_Y + BASKET_H) * h);
    ctx.lineTo((s.x - BASKET_W / 2 + 0.02) * w, (BASKET_Y + BASKET_H) * h);
    ctx.closePath();
    ctx.fill();
    ctx.strokeStyle = C.leaf;
    ctx.lineWidth = 2;
    ctx.stroke();
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
