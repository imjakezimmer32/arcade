import { scores } from "../core/scores";
import { el, GameHandle, hud, maybeOfferScore } from "../core/ui";
import { clearOverlays, endOverlay } from "./types";

type Phase = "ready" | "watch" | "input" | "dead";

type State = {
  seq: number[];
  at: number;
  lit: number;
  lives: number;
  score: number;
  phase: Phase;
  scoreOffered: boolean;
};

const PADS = ["var(--leaf)", "var(--head)", "var(--bite)", "#7fdbff"];
const GLYPHS = ["✿", "★", "●", "◆"];

function fresh(): State {
  return { seq: [], at: 0, lit: -1, lives: 3, score: 0, phase: "ready", scoreOffered: false };
}

function sleep(ms: number) {
  return new Promise<void>((r) => setTimeout(r, ms));
}

export function mountEcho(root: HTMLElement, onBack: () => void): GameHandle {
  let state = fresh();
  let destroyed = false;
  let showToken = 0;

  const wrap = el("div", "game-root");
  const hudHost = el("div");
  const pit = el("div", "pit");
  const grid = el("div", "cell-grid");
  grid.style.gridTemplateColumns = "repeat(2, 1fr)";
  grid.style.padding = "14px";
  grid.style.height = "100%";
  grid.style.gap = "10px";

  const pads = Array.from({ length: 4 }, (_, i) => {
    const pad = el("div", "cell");
    pad.style.borderRadius = "18px";
    pad.style.fontSize = "2rem";
    pad.style.cursor = "pointer";
    pad.addEventListener("pointerdown", (e) => {
      e.preventDefault();
      void onTap(i);
    });
    grid.append(pad);
    return pad;
  });
  pit.append(grid);
  const tip = el("p", "pit-hint", "Repeat the garden. It grows each round.");
  tip.style.position = "static";
  tip.style.marginTop = "8px";
  wrap.append(hudHost, pit, tip);
  root.replaceChildren(wrap);

  const render = () => {
    const best = scores.best("echo", false)?.score ?? 0;
    const hearts = "♥".repeat(Math.max(0, state.lives));
    const sub =
      state.phase === "watch"
        ? "watch"
        : state.phase === "input"
          ? `your turn  ·  best ${best}  ·  ${hearts}`
          : `best ${best}  ·  ${hearts}`;
    hudHost.replaceChildren(
      hud({
        title: "ECHO",
        subtitle: sub,
        value: String(state.score).padStart(2, "0"),
        valueLabel: "round",
        onBack,
      }),
    );
    pads.forEach((pad, i) => {
      const on = state.lit === i;
      pad.style.background = on ? PADS[i]! : "var(--pad)";
      pad.style.borderColor = on ? PADS[i]! : "var(--pad-stroke)";
      pad.style.color = on ? "var(--void)" : PADS[i]!;
      pad.style.opacity = on ? "1" : "0.85";
      pad.textContent = GLYPHS[i]!;
    });
    if (state.phase === "ready") {
      clearOverlays(pit);
      const hint = el("div", "pit-hint", "Tap a pad to listen");
      pit.append(hint);
    } else if (state.phase === "dead") {
      offerDead();
    } else {
      clearOverlays(pit);
      pit.querySelectorAll(".pit-hint").forEach((n) => n.remove());
    }
  };

  const offerDead = () => {
    if (state.scoreOffered) {
      endOverlay(pit, {
        title: "Broke the chain",
        detail: `${state.score} rounds`,
        onRetry: () => void begin(),
      });
      return;
    }
    state = { ...state, scoreOffered: true };
    maybeOfferScore(pit, {
      boardKey: "echo",
      score: state.score,
      lowerBetter: false,
      headline: "Broke the chain",
      detail: `${state.score} rounds`,
      onDone: () => {
        if (destroyed) return;
        endOverlay(pit, {
          title: "Broke the chain",
          detail: `${state.score} rounds`,
          onRetry: () => void begin(),
        });
      },
    });
  };

  const playSeq = async (token: number) => {
    await sleep(380);
    if (destroyed || token !== showToken) return;
    const seq = state.seq;
    for (const pad of seq) {
      if (destroyed || token !== showToken || state.phase !== "watch") return;
      state = { ...state, lit: pad };
      render();
      await sleep(Math.max(160, 380 - seq.length * 14));
      if (destroyed || token !== showToken) return;
      state = { ...state, lit: -1 };
      render();
      await sleep(Math.max(60, 110 - seq.length * 4));
    }
    if (destroyed || token !== showToken) return;
    state = { ...state, phase: "input", lit: -1, at: 0 };
    render();
  };

  const growAndPlay = async () => {
    const pad = (Math.random() * 4) | 0;
    state = { ...state, seq: [...state.seq, pad], at: 0, phase: "watch", lit: -1 };
    render();
    const token = ++showToken;
    await playSeq(token);
  };

  const begin = async () => {
    const wasDead = state.phase === "dead";
    if (wasDead) state = { ...fresh(), phase: "watch" };
    else if (state.phase === "ready") state = { ...state, phase: "watch" };
    render();
    if (wasDead || state.seq.length === 0) await growAndPlay();
    else {
      const token = ++showToken;
      await playSeq(token);
    }
  };

  const onTap = async (pad: number) => {
    if (state.phase === "ready" || state.phase === "dead") {
      await begin();
      return;
    }
    if (state.phase !== "input") return;
    const before = state;
    if (state.seq[state.at] !== pad) {
      const left = state.lives - 1;
      if (left <= 0) {
        state = { ...state, lives: 0, phase: "dead", lit: pad };
        render();
        return;
      }
      state = { ...state, lives: left, at: 0, phase: "watch", lit: pad };
      render();
      await sleep(420);
      if (destroyed) return;
      const token = ++showToken;
      await playSeq(token);
      return;
    }
    const next = state.at + 1;
    if (next >= state.seq.length) {
      state = { ...state, score: state.seq.length, at: 0, phase: "watch", lit: -1 };
      render();
      await growAndPlay();
    } else {
      state = { ...before, at: next, lit: pad };
      render();
      await sleep(120);
      if (destroyed || state.phase !== "input") return;
      state = { ...state, lit: -1 };
      render();
    }
  };

  render();
  return {
    destroy: () => {
      destroyed = true;
      showToken++;
      root.replaceChildren();
    },
  };
}
