import { el, frameLoop, hud, leafButton, maybeOfferScore, type GameHandle } from "../core/ui";

const COLS = 9;
const ROWS = 5;
const CELL_W = 0.092;
const CELL_H = 0.062;
const SHIP_Y = 0.9;
const ALIEN_RX = 0.034;
const ALIEN_RY = 0.024;
const SHIP_W = 0.1;
const SHIP_H = 0.055;
const SHOT_W = 0.012;
const SHOT_H = 0.028;
const UFO_W = 0.1;
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

type Alien = { col: number; row: number; kind: number; alive: boolean };
type Shot = { x: number; y: number; up: boolean };
type Ufo = { x: number; vx: number; prize: number };
type Bunker = { x: number; cells: boolean[] };
type Phase = "ready" | "running" | "paused" | "dead";

function overlap(ax: number, ay: number, aw: number, ah: number, bx: number, by: number, bw: number, bh: number) {
  return Math.abs(ax - bx) * 2 < aw + bw && Math.abs(ay - by) * 2 < ah + bh;
}

function swarm(): Alien[] {
  const out: Alien[] = [];
  for (let row = 0; row < ROWS; row++) {
    const kind = row === 0 ? 2 : row <= 2 ? 1 : 0;
    for (let col = 0; col < COLS; col++) out.push({ col, row, kind, alive: true });
  }
  return out;
}

function bunkers(): Bunker[] {
  return [0.18, 0.4, 0.6, 0.82].map((x) => ({ x, cells: Array.from({ length: 12 }, () => true) }));
}

function fresh() {
  return {
    ship: 0.5,
    aliens: swarm(),
    shots: [] as Shot[],
    bunkers: bunkers(),
    ufo: null as Ufo | null,
    dir: 1,
    originX: 0.08,
    originY: 0.1,
    march: 0,
    shootIn: 0.9,
    ufoIn: 12,
    cooldown: 0,
    holding: false,
    score: 0,
    lives: 3,
    wave: 1,
    frame: 0,
    invuln: 0,
    phase: "ready" as Phase,
    offered: false,
  };
}

export function mountInvaders(root: HTMLElement, onBack: () => void): GameHandle {
  let s = fresh();
  let destroyed = false;

  const wrap = el("div", "game-root");
  const pit = el("div", "pit");
  const canvas = document.createElement("canvas");
  const hint = el("div", "pit-hint", "move to aim · hold / space to fire");
  pit.append(canvas, hint);
  wrap.append(pit);
  root.replaceChildren(wrap);

  const paintHud = () => {
    wrap.replaceChildren(
      hud({
        title: "INVADERS",
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

  const aim = (x: number) => {
    s = {
      ...s,
      ship: Math.min(0.93, Math.max(0.07, x)),
      phase: s.phase === "ready" ? "running" : s.phase,
    };
    hint.remove();
  };

  const fire = () => {
    if (s.phase === "dead") return;
    if (s.phase === "ready" || s.phase === "paused") s = { ...s, phase: "running" };
    if (s.cooldown > 0 || s.shots.some((sh) => sh.up)) return;
    s = {
      ...s,
      shots: [...s.shots, { x: s.ship, y: SHIP_Y - 0.04, up: true }],
      cooldown: 0.22,
      phase: "running",
    };
    hint.remove();
  };

  const end = () => {
    if (s.offered) return;
    s = { ...s, offered: true, phase: "dead" };
    paintHud();
    maybeOfferScore(pit, {
      boardKey: "invaders",
      score: s.score,
      lowerBetter: false,
      headline: "OVERRUN",
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

  const advanceWave = () => {
    const bonus = 50 * s.wave;
    s = {
      ...s,
      aliens: swarm(),
      shots: [],
      bunkers: bunkers(),
      ufo: null,
      dir: 1,
      originX: 0.08,
      originY: Math.min(0.22, 0.1 + s.wave * 0.02),
      march: 0,
      shootIn: Math.max(0.28, 0.85 - s.wave * 0.06),
      ufoIn: 10,
      cooldown: 0.4,
      score: s.score + bonus,
      wave: s.wave + 1,
      frame: 0,
      invuln: 1.2,
    };
    paintHud();
  };

  const step = (dt: number) => {
    if (s.phase !== "running") return;
    s = {
      ...s,
      cooldown: Math.max(0, s.cooldown - dt),
      invuln: Math.max(0, s.invuln - dt),
      ufoIn: s.ufoIn - dt,
    };
    if (s.holding && s.cooldown <= 0 && !s.shots.some((sh) => sh.up)) fire();

    const living = s.aliens.filter((a) => a.alive);
    if (!living.length) {
      advanceWave();
      return;
    }

    // march
    const left = Math.min(...living.map((a) => s.originX + a.col * CELL_W));
    const right = Math.max(...living.map((a) => s.originX + a.col * CELL_W));
    const remain = living.length / s.aliens.length;
    const interval = (0.12 + remain * 0.42) / (1 + (s.wave - 1) * 0.12);
    let t = s.march + dt;
    let ox = s.originX;
    let oy = s.originY;
    let nd = s.dir;
    let fr = s.frame;
    if (t >= interval) {
      t = 0;
      fr++;
      const stepX = 0.014;
      ox += nd * stepX;
      const nextLeft = left + nd * stepX;
      const nextRight = right + nd * stepX;
      if (nextRight > 0.94 || nextLeft < 0.04) {
        nd = -nd;
        oy += 0.026;
        ox = s.originX;
      }
    }
    s = { ...s, originX: ox, originY: oy, dir: nd, march: t, frame: fr };

    // move shots + ufo
    let shots = s.shots
      .map((sh) => ({ ...sh, y: sh.y + (sh.up ? -0.95 * dt : 0.42 * dt) }))
      .filter((sh) => sh.y >= -0.08 && sh.y <= 1.08);
    let ufo = s.ufo ? { ...s.ufo, x: s.ufo.x + s.ufo.vx * dt } : null;
    if (ufo && (ufo.x < -0.12 || ufo.x > 1.12)) ufo = null;
    s = { ...s, shots, ufo };

    // collide
    let nextAliens = s.aliens.slice();
    let nextBunkers = s.bunkers.map((b) => ({ x: b.x, cells: b.cells.slice() }));
    let nextUfo = s.ufo;
    let nextScore = s.score;
    let nextLives = s.lives;
    let nextInvuln = s.invuln;
    let lifeHit = false;

    const bunkerHit = (x: number, y: number) => {
      if (y < 0.7 || y > 0.82) return false;
      let hit = false;
      nextBunkers = nextBunkers.map((bunker) => {
        const localX = x - bunker.x + 0.055;
        const localY = y - 0.7;
        const c = (localX / 0.028) | 0;
        const r = (localY / 0.04) | 0;
        if (c < 0 || c > 3 || r < 0 || r > 2) return bunker;
        const i = r * 4 + c;
        if (!bunker.cells[i]) return bunker;
        hit = true;
        const cells = bunker.cells.slice();
        cells[i] = false;
        return { x: bunker.x, cells };
      });
      return hit;
    };

    shots = shots.filter((shot) => {
      if (bunkerHit(shot.x, shot.y)) return false;
      if (shot.up) {
        if (nextUfo && Math.abs(shot.x - nextUfo.x) < UFO_W / 2 && shot.y < 0.08) {
          nextScore += nextUfo.prize;
          nextUfo = null;
          return false;
        }
        const hit = nextAliens.findIndex((a) => {
          if (!a.alive) return false;
          const ax = s.originX + a.col * CELL_W;
          const ay = s.originY + a.row * CELL_H;
          return overlap(ax, ay, ALIEN_RX * 2, ALIEN_RY * 2, shot.x, shot.y, SHOT_W, SHOT_H);
        });
        if (hit >= 0) {
          const alien = nextAliens[hit]!;
          nextAliens = nextAliens.slice();
          nextAliens[hit] = { ...alien, alive: false };
          nextScore += alien.kind === 2 ? 30 : alien.kind === 1 ? 20 : 10;
          return false;
        }
        return true;
      }
      if (
        s.invuln <= 0 &&
        overlap(shot.x, shot.y, SHOT_W, SHOT_H, s.ship, SHIP_Y, SHIP_W, SHIP_H)
      ) {
        nextLives -= 1;
        nextInvuln = 2;
        lifeHit = true;
        return false;
      }
      return true;
    });
    if (lifeHit) shots = shots.filter((sh) => sh.up);

    const scored = nextScore !== s.score || nextLives !== s.lives;
    s = {
      ...s,
      aliens: nextAliens,
      shots,
      bunkers: nextBunkers,
      ufo: nextUfo,
      score: nextScore,
      lives: Math.max(0, nextLives),
      invuln: nextInvuln,
    };
    if (scored) paintHud();

    if (!s.ufo && s.ufoIn <= 0) {
      const goingRight = s.wave % 2 === 0;
      s = {
        ...s,
        ufo: {
          x: goingRight ? -0.08 : 1.08,
          vx: goingRight ? 0.22 : -0.22,
          prize: [50, 100, 150, 300][(Math.random() * 4) | 0]!,
        },
        ufoIn: 16 + s.wave * 2,
      };
    }

    let shootIn = s.shootIn - dt;
    if (shootIn <= 0) {
      if (s.shots.filter((sh) => !sh.up).length >= 3) shootIn = 0.2;
      else {
        const alive = s.aliens.filter((a) => a.alive);
        if (alive.length) {
          const byCol = new Map<number, Alien>();
          for (const a of alive) {
            const prev = byCol.get(a.col);
            if (!prev || a.row > prev.row) byCol.set(a.col, a);
          }
          const bottoms = [...byCol.values()];
          const shooter = bottoms[(Math.random() * bottoms.length) | 0]!;
          s = {
            ...s,
            shots: [
              ...s.shots,
              {
                x: s.originX + shooter.col * CELL_W,
                y: s.originY + shooter.row * CELL_H + 0.03,
                up: false,
              },
            ],
          };
          shootIn = Math.max(0.26, 0.72 - s.wave * 0.05);
        } else shootIn = 0.4;
      }
    }
    s = { ...s, shootIn };

    if (s.aliens.some((a) => a.alive && s.originY + a.row * CELL_H > 0.78)) {
      s = { ...s, lives: 0, phase: "dead" };
      end();
      return;
    }
    if (s.lives <= 0) {
      end();
      return;
    }
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

    for (const a of s.aliens) {
      if (!a.alive) continue;
      const ax = s.originX + a.col * CELL_W;
      const ay = s.originY + a.row * CELL_H;
      ctx.fillStyle = a.kind === 2 ? C.head : a.kind === 1 ? C.leaf : C.mist;
      const wobble = (s.frame & 1) * 2;
      ctx.fillRect(ax * w - ALIEN_RX * w + wobble, ay * h - ALIEN_RY * h, ALIEN_RX * 2 * w, ALIEN_RY * 2 * h);
    }

    for (const b of s.bunkers) {
      for (let i = 0; i < 12; i++) {
        if (!b.cells[i]) continue;
        const c = i % 4;
        const r = (i / 4) | 0;
        ctx.fillStyle = C.dim;
        ctx.fillRect((b.x - 0.055 + c * 0.028) * w, (0.7 + r * 0.04) * h, 0.026 * w, 0.036 * h);
      }
    }

    if (s.ufo) {
      ctx.fillStyle = C.bite;
      ctx.fillRect((s.ufo.x - UFO_W / 2) * w, 0.03 * h, UFO_W * w, 0.035 * h);
    }

    for (const sh of s.shots) {
      ctx.fillStyle = sh.up ? C.head : C.bite;
      ctx.fillRect((sh.x - SHOT_W / 2) * w, (sh.y - SHOT_H / 2) * h, SHOT_W * w, SHOT_H * h);
    }

    ctx.globalAlpha = s.invuln > 0 ? 0.45 + 0.55 * Math.abs(Math.sin(performance.now() / 80)) : 1;
    ctx.fillStyle = C.leaf;
    ctx.beginPath();
    ctx.moveTo(s.ship * w, (SHIP_Y - SHIP_H / 2) * h);
    ctx.lineTo((s.ship + SHIP_W / 2) * w, (SHIP_Y + SHIP_H / 2) * h);
    ctx.lineTo((s.ship - SHIP_W / 2) * w, (SHIP_Y + SHIP_H / 2) * h);
    ctx.closePath();
    ctx.fill();
    ctx.globalAlpha = 1;
  };

  const ptr = (e: PointerEvent) => {
    const rect = pit.getBoundingClientRect();
    aim((e.clientX - rect.left) / rect.width);
  };
  pit.addEventListener("pointerdown", (e) => {
    s = { ...s, holding: true };
    ptr(e);
    fire();
    pit.setPointerCapture(e.pointerId);
  });
  pit.addEventListener("pointermove", ptr);
  pit.addEventListener("pointerup", () => {
    s = { ...s, holding: false };
  });
  const onKey = (e: KeyboardEvent) => {
    if (e.key === " " || e.key === "ArrowUp" || e.key === "w") {
      e.preventDefault();
      fire();
    }
    if (e.key === "ArrowLeft" || e.key === "a") aim(s.ship - 0.04);
    if (e.key === "ArrowRight" || e.key === "d") aim(s.ship + 0.04);
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
