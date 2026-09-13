import { el, frameLoop, hud, leafButton, maybeOfferScore, type GameHandle } from "../core/ui";

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

type Hole = { up: number; hit: number };
type Phase = "ready" | "running" | "paused" | "dead";

function fresh() {
  return {
    holes: Array.from({ length: 9 }, () => ({ up: 0, hit: 0 })) as Hole[],
    spawn: 0.4,
    lives: 3,
    score: 0,
    combo: 0,
    phase: "ready" as Phase,
    offered: false,
  };
}

export function mountPeck(root: HTMLElement, onBack: () => void): GameHandle {
  let s = fresh();
  let destroyed = false;

  const wrap = el("div", "game-root");
  const pit = el("div", "pit");
  const canvas = document.createElement("canvas");
  const hint = el("div", "pit-hint", "whack the sprouts");
  pit.append(canvas, hint);
  wrap.append(pit);
  root.replaceChildren(wrap);

  const paintHud = () => {
    wrap.replaceChildren(
      hud({
        title: "PECK",
        subtitle: `❤ ${s.lives} · combo ${s.combo}`,
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

  const peck = (i: number) => {
    if (s.phase === "dead" || i < 0 || i > 8) return;
    if (s.phase === "ready") {
      s = { ...s, phase: "running" };
      hint.remove();
    }
    const hole = s.holes[i]!;
    if (hole.up > 0) {
      const holes = s.holes.slice();
      holes[i] = { up: 0, hit: 0.22 };
      s = { ...s, holes, score: s.score + 10 + s.combo * 2, combo: s.combo + 1 };
      paintHud();
    } else s = { ...s, combo: 0, phase: s.phase === "ready" ? "running" : s.phase };
  };

  const end = () => {
    if (s.offered) return;
    s = { ...s, offered: true };
    paintHud();
    maybeOfferScore(pit, {
      boardKey: "peck",
      score: s.score,
      lowerBetter: false,
      headline: "WILTED",
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
    let nextLives = s.lives;
    const next = s.holes.map((h) => {
      const hit = Math.max(0, h.hit - dt);
      if (h.up > 0) {
        const left = h.up - dt;
        if (left <= 0) {
          nextLives -= 1;
          return { up: 0, hit: 0 };
        }
        return { up: left, hit };
      }
      return { up: 0, hit };
    });
    if (nextLives <= 0) {
      s = { ...s, holes: next, lives: 0, phase: "dead" };
      end();
      return;
    }
    let spawnIn = s.spawn - dt;
    let holes2 = next;
    if (spawnIn <= 0 && next.filter((h) => h.up > 0).length < 3) {
      const open = next.map((_, i) => i).filter((i) => next[i]!.up <= 0 && next[i]!.hit <= 0);
      if (open.length) {
        const i = open[(Math.random() * open.length) | 0]!;
        holes2 = next.slice();
        holes2[i] = { up: Math.max(0.38, 0.72 - s.score * 0.002), hit: 0 };
      }
      spawnIn = Math.max(0.18, 0.55 - s.score * 0.0025);
    }
    if (nextLives !== s.lives) paintHud();
    s = { ...s, holes: holes2, spawn: spawnIn, lives: nextLives };
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
    const gap = 12;
    const size = Math.min((w - gap * 4) / 3, (h - gap * 4) / 3);
    const ox = (w - size * 3 - gap * 2) / 2;
    const oy = (h - size * 3 - gap * 2) / 2;
    for (let i = 0; i < 9; i++) {
      const cx = i % 3;
      const cy = (i / 3) | 0;
      const x = ox + cx * (size + gap);
      const y = oy + cy * (size + gap);
      ctx.fillStyle = C.pad;
      const r = 18;
      ctx.beginPath();
      ctx.moveTo(x + r, y);
      ctx.arcTo(x + size, y, x + size, y + size, r);
      ctx.arcTo(x + size, y + size, x, y + size, r);
      ctx.arcTo(x, y + size, x, y, r);
      ctx.arcTo(x, y, x + size, y, r);
      ctx.closePath();
      ctx.fill();
      const hole = s.holes[i]!;
      if (hole.hit > 0) {
        ctx.fillStyle = C.head;
        ctx.globalAlpha = hole.hit / 0.22;
        ctx.beginPath();
        ctx.arc(x + size / 2, y + size / 2, size * 0.28, 0, Math.PI * 2);
        ctx.fill();
        ctx.globalAlpha = 1;
      } else if (hole.up > 0) {
        const t = Math.min(1, hole.up);
        ctx.fillStyle = C.leaf;
        ctx.beginPath();
        ctx.ellipse(x + size / 2, y + size / 2 + (1 - Math.min(1, t / 0.5)) * size * 0.1, size * 0.22, size * 0.28, 0, 0, Math.PI * 2);
        ctx.fill();
        ctx.fillStyle = C.head;
        ctx.beginPath();
        ctx.arc(x + size / 2, y + size / 2 - size * 0.12, size * 0.1, 0, Math.PI * 2);
        ctx.fill();
      } else {
        ctx.fillStyle = C.void;
        ctx.beginPath();
        ctx.arc(x + size / 2, y + size * 0.62, size * 0.18, 0, Math.PI * 2);
        ctx.fill();
      }
    }
  };

  pit.addEventListener("pointerdown", (e) => {
    const rect = pit.getBoundingClientRect();
    const px = e.clientX - rect.left;
    const py = e.clientY - rect.top;
    const w = pit.clientWidth;
    const h = pit.clientHeight;
    const gap = 12;
    const size = Math.min((w - gap * 4) / 3, (h - gap * 4) / 3);
    const ox = (w - size * 3 - gap * 2) / 2;
    const oy = (h - size * 3 - gap * 2) / 2;
    for (let i = 0; i < 9; i++) {
      const cx = i % 3;
      const cy = (i / 3) | 0;
      const x = ox + cx * (size + gap);
      const y = oy + cy * (size + gap);
      if (px >= x && px <= x + size && py >= y && py <= y + size) {
        peck(i);
        break;
      }
    }
  });

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
