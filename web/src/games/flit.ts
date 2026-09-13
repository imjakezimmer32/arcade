import { el, frameLoop, hud, leafButton, maybeOfferScore, type GameHandle } from "../core/ui";

const BIRD_X = 0.26;
const RADIUS = 0.034;
const GAP = 0.205;
const PIPE_W = 0.1;
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

type Pipe = { x: number; gap: number };
type Phase = "ready" | "running" | "paused" | "dead";

function randGap() {
  return 0.3 + Math.random() * 0.4;
}

function fresh() {
  return {
    y: 0.5,
    v: 0,
    pipes: [
      { x: 1.15, gap: 0.48 },
      { x: 1.72, gap: 0.52 },
    ] as Pipe[],
    score: 0,
    phase: "ready" as Phase,
    offered: false,
  };
}

export function mountFlit(root: HTMLElement, onBack: () => void): GameHandle {
  let s = fresh();
  let destroyed = false;

  const wrap = el("div", "game-root");
  const pit = el("div", "pit");
  const canvas = document.createElement("canvas");
  const hint = el("div", "pit-hint", "tap / space to flap");
  pit.append(canvas, hint);
  wrap.append(pit);
  root.replaceChildren(wrap);

  const paintHud = () => {
    wrap.replaceChildren(
      hud({
        title: "FLIT",
        subtitle: s.phase === "paused" ? "paused" : "stay aloft",
        value: String(s.score),
        valueLabel: "SCORE",
        onBack,
        onPause: () => {
          if (s.phase === "running") s = { ...s, phase: "paused" };
          else if (s.phase === "paused") s = { ...s, phase: "running", v: -0.58 };
          paintHud();
        },
        paused: s.phase === "paused",
      }),
      pit,
    );
  };
  paintHud();

  const flap = () => {
    if (s.phase === "dead") return;
    if (s.phase === "paused") s = { ...s, phase: "running", v: -0.58 };
    else s = { ...s, v: -0.58, phase: "running" };
    hint.remove();
  };

  const end = () => {
    if (s.offered) return;
    s = { ...s, offered: true };
    paintHud();
    maybeOfferScore(pit, {
      boardKey: "flit",
      score: s.score,
      lowerBetter: false,
      headline: "CLIPPED",
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
    const nv = Math.min(0.95, Math.max(-0.9, s.v + 1.85 * dt));
    const ny = s.y + nv * dt;
    if (ny - RADIUS < 0.02 || ny + RADIUS > 0.93) {
      s = { ...s, y: Math.min(0.93, Math.max(0.04, ny)), v: nv, phase: "dead" };
      end();
      return;
    }
    let nextScore = s.score;
    const speed = Math.min(0.42, 0.28 + s.score * 0.008);
    const moved = s.pipes.map((p) => {
      const nx = p.x - speed * dt;
      if (p.x > BIRD_X && nx <= BIRD_X) nextScore += 1;
      if (nx < -0.18) return { x: 1.22, gap: randGap() };
      return { x: nx, gap: p.gap };
    });
    const hit = moved.some((p) => {
      const inX = Math.abs(p.x - BIRD_X) < PIPE_W / 2 + RADIUS;
      const inGap = ny > p.gap - GAP + 0.018 && ny < p.gap + GAP - 0.018;
      return inX && !inGap;
    });
    if (hit) {
      s = { ...s, y: ny, v: nv, pipes: moved, score: nextScore, phase: "dead" };
      end();
      return;
    }
    if (nextScore !== s.score) {
      s = { ...s, y: ny, v: nv, pipes: moved, score: nextScore };
      paintHud();
    } else s = { ...s, y: ny, v: nv, pipes: moved, score: nextScore };
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
    for (const p of s.pipes) {
      const px = p.x * w;
      const half = (PIPE_W * w) / 2;
      const gapTop = (p.gap - GAP) * h;
      const gapBot = (p.gap + GAP) * h;
      ctx.fillStyle = C.leaf;
      ctx.fillRect(px - half, 0, PIPE_W * w, gapTop);
      ctx.fillRect(px - half, gapBot, PIPE_W * w, h - gapBot);
      ctx.fillStyle = C.head;
      ctx.fillRect(px - half - 2, gapTop - 8, PIPE_W * w + 4, 8);
      ctx.fillRect(px - half - 2, gapBot, PIPE_W * w + 4, 8);
    }
    ctx.fillStyle = C.dim;
    ctx.fillRect(0, h * 0.93, w, h * 0.07);
    ctx.fillStyle = C.head;
    ctx.beginPath();
    ctx.arc(BIRD_X * w, s.y * h, RADIUS * w, 0, Math.PI * 2);
    ctx.fill();
  };

  pit.addEventListener("pointerdown", (e) => {
    e.preventDefault();
    flap();
  });
  const onKey = (e: KeyboardEvent) => {
    if (e.key === " " || e.key === "ArrowUp" || e.key === "w") {
      e.preventDefault();
      flap();
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
