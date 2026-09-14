import { el, GameHandle, hud, leafButton } from "../core/ui";
import { clearOverlays, hotSeatLobby, passOverlay, recordHotSeat, seatResult } from "./types";

type Phase = "setup" | "playing" | "over";

type State = {
  h: boolean[];
  v: boolean[];
  owner: number[];
  leafTurn: boolean;
  phase: Phase;
  leafName: string;
  headName: string;
  passPending: boolean;
  recorded: boolean;
  lastH: number;
  lastV: number;
};

function lobby(): State {
  return {
    h: Array(5 * 4).fill(false),
    v: Array(4 * 5).fill(false),
    owner: Array(16).fill(0),
    leafTurn: true,
    phase: "setup",
    leafName: "LEAF",
    headName: "HEAD",
    passPending: false,
    recorded: false,
    lastH: -1,
    lastV: -1,
  };
}

function start(leaf: string, head: string): State {
  return { ...lobby(), phase: "playing", leafName: leaf, headName: head };
}

function count(owner: number[], leaf: boolean) {
  const side = leaf ? 1 : 2;
  return owner.filter((v) => v === side).length;
}

function settle(state: State, nextH: boolean[], nextV: boolean[], lastH: number, lastV: number): State {
  const piece = state.leafTurn ? 1 : 2;
  const nextOwner = state.owner.slice();
  let gained = 0;
  for (let r = 0; r < 4; r++)
    for (let c = 0; c < 4; c++) {
      const idx = r * 4 + c;
      if (nextOwner[idx] !== 0) continue;
      const top = nextH[r * 4 + c];
      const bot = nextH[(r + 1) * 4 + c];
      const left = nextV[r * 5 + c];
      const right = nextV[r * 5 + c + 1];
      if (top && bot && left && right) {
        nextOwner[idx] = piece;
        gained++;
      }
    }
  const full = nextOwner.every((v) => v !== 0);
  return {
    ...state,
    h: nextH,
    v: nextV,
    owner: nextOwner,
    leafTurn: gained > 0 ? state.leafTurn : !state.leafTurn,
    lastH,
    lastV,
    phase: full ? "over" : "playing",
    passPending: !full && gained === 0,
  };
}

function claimH(state: State, i: number): State {
  if (state.phase !== "playing" || state.passPending || i < 0 || i >= state.h.length || state.h[i]) return state;
  const nextH = state.h.slice();
  nextH[i] = true;
  return settle(state, nextH, state.v.slice(), i, -1);
}

function claimV(state: State, i: number): State {
  if (state.phase !== "playing" || state.passPending || i < 0 || i >= state.v.length || state.v[i]) return state;
  const nextV = state.v.slice();
  nextV[i] = true;
  return settle(state, state.h.slice(), nextV, -1, i);
}

export function mountBoxes(root: HTMLElement, onBack: () => void): GameHandle {
  let state = lobby();

  const wrap = el("div", "game-root");
  const hudHost = el("div");
  const pit = el("div", "pit");
  const board = el("div");
  board.style.display = "grid";
  board.style.padding = "10px";
  board.style.aspectRatio = "1";
  board.style.margin = "auto";
  board.style.width = "100%";
  board.style.maxWidth = "100%";
  // 5 dots + 4 boxes: rows alternate h-lines and v+box rows
  // Use CSS grid: 9 tracks (dot, gap, box, gap...): simpler absolute-ish flex layout
  board.style.gridTemplateColumns = "14px 1fr 14px 1fr 14px 1fr 14px 1fr 14px";
  board.style.gridTemplateRows = "14px 1fr 14px 1fr 14px 1fr 14px 1fr 14px";
  board.style.gap = "0";
  pit.append(board);

  const controls = el("div", "controls");
  controls.append(
    leafButton(
      "NEW",
      () => {
        state = lobby();
        render();
      },
      true,
    ),
  );
  const tip = el("p", "pit-hint", "Close a square to keep the turn.");
  tip.style.position = "static";
  tip.style.marginTop = "8px";
  wrap.append(hudHost, pit, controls, tip);
  root.replaceChildren(wrap);

  const lineColor = (on: boolean, last: boolean) =>
    last ? "var(--head)" : on ? "var(--leaf)" : "var(--pad-stroke)";

  const paintBoard = () => {
    board.replaceChildren();
    // Place 5x5 dots at odd intersections of the 9x9 track
    for (let r = 0; r < 5; r++) {
      for (let c = 0; c < 5; c++) {
        const dot = el("div");
        dot.style.gridColumn = `${c * 2 + 1}`;
        dot.style.gridRow = `${r * 2 + 1}`;
        dot.style.borderRadius = "50%";
        dot.style.background = "var(--leaf)";
        board.append(dot);
      }
    }
    // Horizontal lines: between dots on each of 5 rows, 4 per row
    for (let r = 0; r < 5; r++) {
      for (let c = 0; c < 4; c++) {
        const i = r * 4 + c;
        const line = el("div");
        line.style.gridColumn = `${c * 2 + 2}`;
        line.style.gridRow = `${r * 2 + 1}`;
        line.style.alignSelf = "center";
        line.style.height = "10px";
        line.style.borderRadius = "8px";
        line.style.background = lineColor(!!state.h[i], state.lastH === i);
        line.style.cursor = state.h[i] || state.phase !== "playing" ? "default" : "pointer";
        line.addEventListener("click", () => {
          state = claimH(state, i);
          render();
        });
        board.append(line);
      }
    }
    // Vertical lines + boxes
    for (let r = 0; r < 4; r++) {
      for (let c = 0; c < 5; c++) {
        const i = r * 5 + c;
        const line = el("div");
        line.style.gridColumn = `${c * 2 + 1}`;
        line.style.gridRow = `${r * 2 + 2}`;
        line.style.justifySelf = "center";
        line.style.width = "10px";
        line.style.borderRadius = "8px";
        line.style.background = lineColor(!!state.v[i], state.lastV === i);
        line.style.cursor = state.v[i] || state.phase !== "playing" ? "default" : "pointer";
        line.addEventListener("click", () => {
          state = claimV(state, i);
          render();
        });
        board.append(line);
      }
      for (let c = 0; c < 4; c++) {
        const own = state.owner[r * 4 + c]!;
        const box = el("div");
        box.style.gridColumn = `${c * 2 + 2}`;
        box.style.gridRow = `${r * 2 + 2}`;
        box.style.margin = "4px";
        box.style.borderRadius = "10px";
        box.style.background =
          own === 1 ? "rgba(61,220,132,0.45)" : own === 2 ? "rgba(200,255,122,0.45)" : "var(--pad)";
        board.append(box);
      }
    }
  };

  const maybeRecord = () => {
    if (state.phase !== "over" || state.recorded) return;
    state = { ...state, recorded: true };
    recordHotSeat("boxes", state.leafName, count(state.owner, true), state.headName, count(state.owner, false));
  };

  const render = () => {
    const name = state.leafTurn ? state.leafName : state.headName;
    const leafN = count(state.owner, true);
    const headN = count(state.owner, false);
    const subtitle =
      state.phase === "setup" ? "hot seat" : state.phase === "over" ? "grid full" : `${name} draws a line`;
    hudHost.replaceChildren(
      hud({
        title: "BOXES",
        subtitle,
        value: `${leafN}–${headN}`,
        valueLabel: "boxes",
        onBack,
      }),
    );
    paintBoard();
    maybeRecord();
    if (state.phase === "setup") {
      hotSeatLobby(pit, {
        title: "BOXES",
        leafRole: "GREEN",
        headRole: "LIME",
        blurb: "Draw a line. Close a box, go again. Most boxes wins.",
        onStart: (leaf, head) => {
          state = start(leaf, head);
          render();
        },
      });
    } else if (state.passPending) {
      passOverlay(pit, name, () => {
        state = { ...state, passPending: false };
        render();
      });
    } else if (state.phase === "over") {
      seatResult(pit, {
        title: leafN > headN ? `${state.leafName} takes it` : headN > leafN ? `${state.headName} takes it` : "Draw",
        detail: `${state.leafName} ${leafN}  ·  ${state.headName} ${headN}`,
        onRematch: () => {
          state = start(state.leafName, state.headName);
          render();
        },
        onLobby: () => {
          state = lobby();
          render();
        },
      });
    } else clearOverlays(pit);
  };

  render();
  return { destroy: () => root.replaceChildren() };
}
