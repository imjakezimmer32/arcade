import { el, frameLoop, hud, leafButton, maybeOfferScore, type GameHandle } from "../core/ui";

const COLS = 8;
const ROWS = 6;
const PADDLE_W = 0.28;
const PADDLE_H = 0.032;
const PADDLE_Y = 0.9;
const BALL_RX = 0.018;
const BALL_RY = 0.012;
const BRICK_TOP = 0.06;
const BRICK_H = 0.06;
const BRICK_BODY = 0.72;
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

type Brick = { col: number; row: number; alive: boolean };
type Phase = "ready" | "running" | "paused" | "dead";

function spawnBricks(wave: number): Brick[] {
  const rows = Math.min(8, ROWS + Math.floor((wave - 1) / 2));
  const out: Brick[] = [];
  for (let r = 0; r < rows; r++) for (let c = 0; c < COLS; c++) out.push({ col: c, row: r, alive: true });
  return out;
}

function clampBall(vx: number, vy: number): [number, number] {
  let x = vx;
  let y = vy;
  if (Math.abs(y) < 0.38) y = 0.38 * (y === 0 ? -1 : Math.sign(y));
  const speed = Math.hypot(x, y);
  const target = Math.min(1.15, Math.max(0.52, speed));
  if (speed > 0.001) {
    x = (x / speed) * target;
    y = (y / speed) * target;
  }
  return [x, y];
}

function fresh() {
  return {
    paddleX: 0.5,
    ballX: 0.5,
    ballY: 0.84,
    vx: 0.28,
    vy: -0.58,
    bricks: spawnBricks(1),
    lives: 3,
    score: 0,
    wave: 1,
    stuck: true,
    phase: "ready" as Phase,
    offered: false,
  };
}

export function mountBreakout(root: HTMLElement, onBack: () => void): GameHandle {
  let s = fresh();
  let destroyed = false;

  const wrap = el("div", "game-root");
  const pit = el("div", "pit");
  const canvas = document.createElement("canvas");
  const hint = el("div", "pit-hint", "move to aim · click / tap to launch");
  pit.append(canvas, hint);
  wrap.append(pit);
  root.replaceChildren(wrap);

  const paintHud = () => {
    wrap.replaceChildren(
      hud({
        title: "BREAKOUT",
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

  const movePaddle = (nx: number) => {
    const x = Math.min(1 - PADDLE_W / 2, Math.max(PADDLE_W / 2, nx));
    if (s.stuck || s.phase === "ready") {
      s = { ...s, paddleX: x, ballX: x, phase: s.phase === "ready" ? "running" : s.phase };
      hint.remove();
    } else s = { ...s, paddleX: x };
  };

  const launch = () => {
    if (s.phase === "dead") return;
    if (s.phase === "ready" || s.stuck) {
      s = { ...s, phase: "running", stuck: false };
      hint.remove();
    } else if (s.phase === "paused") s = { ...s, phase: "running" };
  };

  const end = () => {
    if (s.offered) return;
    s = { ...s, offered: true };
    paintHud();
    maybeOfferScore(pit, {
      boardKey: "breakout",
      score: s.score,
      lowerBetter: false,
      headline: "CRACKED",
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
    if (s.stuck) {
      s = { ...s, ballX: s.paddleX, ballY: 0.84 };
      return;
    }
    let x = s.ballX + s.vx * dt;
    let y = s.ballY + s.vy * dt;
    let nvx = s.vx;
    let nvy = s.vy;
    if (x < BALL_RX) {
      x = BALL_RX;
      nvx = Math.abs(nvx);
    }
    if (x > 1 - BALL_RX) {
      x = 1 - BALL_RX;
      nvx = -Math.abs(nvx);
    }
    if (y < BALL_RY) {
      y = BALL_RY;
      nvy = Math.abs(nvy);
    }
    const half = PADDLE_W / 2;
    if (
      nvy > 0 &&
      y + BALL_RY >= PADDLE_Y &&
      y - BALL_RY <= PADDLE_Y + PADDLE_H &&
      x >= s.paddleX - half - BALL_RX &&
      x <= s.paddleX + half + BALL_RX
    ) {
      y = PADDLE_Y - BALL_RY;
      const hit = Math.min(1, Math.max(-1, (x - s.paddleX) / half));
      nvx = hit * 0.78;
      nvy = -Math.max(0.48, Math.abs(nvy));
      [nvx, nvy] = clampBall(nvx, nvy);
    }
    const rows = Math.max(...s.bricks.map((b) => b.row)) + 1;
    const bw = 1 / COLS;
    let nextBricks = s.bricks;
    let nextScore = s.score;
    let consumed = false;
    nextBricks = s.bricks.map((brick) => {
      if (consumed || !brick.alive) return brick;
      const left = brick.col * bw + 0.006;
      const right = (brick.col + 1) * bw - 0.006;
      const bt = BRICK_TOP + brick.row * BRICK_H;
      const bb = bt + BRICK_H * BRICK_BODY;
      const hit = x >= left - BALL_RX && x <= right + BALL_RX && y >= bt - BALL_RY && y <= bb + BALL_RY;
      if (!hit) return brick;
      consumed = true;
      const cx = (left + right) / 2;
      const cy = (bt + bb) / 2;
      if (Math.abs(x - cx) * (bb - bt) > Math.abs(y - cy) * (right - left)) nvx = x < cx ? -Math.abs(nvx) : Math.abs(nvx);
      else nvy = y < cy ? -Math.abs(nvy) : Math.abs(nvy);
      [nvx, nvy] = clampBall(nvx * 1.03, nvy * 1.03);
      nextScore += (rows - brick.row) * 10 * s.wave;
      return { ...brick, alive: false };
    });
    if (nextBricks.every((b) => !b.alive)) {
      const nxt = s.wave + 1;
      s = {
        ...s,
        ballX: s.paddleX,
        ballY: 0.84,
        vx: 0.28 + nxt * 0.02,
        vy: -0.58 - nxt * 0.02,
        bricks: spawnBricks(nxt),
        score: nextScore + 150 * s.wave,
        wave: nxt,
        stuck: true,
      };
      paintHud();
      return;
    }
    if (y > 1.04) {
      const left = s.lives - 1;
      if (left <= 0) {
        s = { ...s, ballX: x, ballY: y, bricks: nextBricks, score: nextScore, lives: 0, phase: "dead" };
        end();
        return;
      }
      s = {
        ...s,
        ballX: s.paddleX,
        ballY: 0.84,
        vx: 0.32,
        vy: -0.55,
        bricks: nextBricks,
        score: nextScore,
        lives: left,
        stuck: true,
      };
      paintHud();
      return;
    }
    const scored = nextScore !== s.score;
    s = { ...s, ballX: x, ballY: y, vx: nvx, vy: nvy, bricks: nextBricks, score: nextScore };
    if (scored) paintHud();
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
    const bw = 1 / COLS;
    for (const b of s.bricks) {
      if (!b.alive) continue;
      const left = b.col * bw + 0.006;
      const top = BRICK_TOP + b.row * BRICK_H;
      ctx.fillStyle = b.row % 2 ? C.leaf : C.head;
      ctx.fillRect(left * w, top * h, (bw - 0.012) * w, BRICK_H * BRICK_BODY * h);
    }
    ctx.fillStyle = C.mist;
    ctx.fillRect((s.paddleX - PADDLE_W / 2) * w, PADDLE_Y * h, PADDLE_W * w, PADDLE_H * h);
    ctx.fillStyle = C.head;
    ctx.beginPath();
    ctx.ellipse(s.ballX * w, s.ballY * h, BALL_RX * w, BALL_RY * h, 0, 0, Math.PI * 2);
    ctx.fill();
  };

  const ptr = (e: PointerEvent) => {
    const rect = pit.getBoundingClientRect();
    movePaddle((e.clientX - rect.left) / rect.width);
  };
  pit.addEventListener("pointerdown", (e) => {
    ptr(e);
    launch();
    pit.setPointerCapture(e.pointerId);
  });
  pit.addEventListener("pointermove", (e) => {
    if (e.buttons || e.pressure > 0) ptr(e);
    else if (e.pointerType === "mouse") ptr(e);
  });
  const onKey = (e: KeyboardEvent) => {
    if (e.key === " " || e.key === "Enter") launch();
    if (e.key === "ArrowLeft") movePaddle(s.paddleX - 0.05);
    if (e.key === "ArrowRight") movePaddle(s.paddleX + 0.05);
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
