import { type Dir, dirFromSwipe } from "./catalog";
import { scores } from "./scores";

export type GameHandle = {
  destroy: () => void;
};

export function el<K extends keyof HTMLElementTagNameMap>(
  tag: K,
  className?: string,
  text?: string,
): HTMLElementTagNameMap[K] {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text != null) node.textContent = text;
  return node;
}

export function clear(node: HTMLElement) {
  node.replaceChildren();
}

export function hud(opts: {
  title: string;
  subtitle: string;
  value: string;
  valueLabel?: string;
  onBack: () => void;
  onPause?: () => void;
  paused?: boolean;
}): HTMLElement {
  const root = el("div", "hud");
  const left = el("div", "hud-left");
  const back = el("button", "icon-btn", "←");
  back.type = "button";
  back.setAttribute("aria-label", "Back");
  back.addEventListener("click", opts.onBack);
  const titles = el("div");
  titles.append(el("div", "hud-title", opts.title), el("div", "hud-sub", opts.subtitle));
  left.append(back, titles);

  const right = el("div");
  right.append(el("div", "hud-value", opts.value));
  if (opts.valueLabel) right.append(el("div", "hud-label", opts.valueLabel));
  if (opts.onPause) {
    const pause = el("button", "icon-btn", opts.paused ? "▶" : "❚❚");
    pause.type = "button";
    pause.style.marginTop = "6px";
    pause.addEventListener("click", opts.onPause);
    const wrap = el("div");
    wrap.style.display = "flex";
    wrap.style.flexDirection = "column";
    wrap.style.alignItems = "flex-end";
    wrap.append(right, pause);
    root.append(left, wrap);
    return root;
  }
  root.append(left, right);
  return root;
}

export function leafButton(text: string, onClick: () => void, ghost = false): HTMLButtonElement {
  const btn = el("button", ghost ? "leaf-btn ghost" : "leaf-btn", text);
  btn.type = "button";
  btn.addEventListener("click", onClick);
  return btn;
}

export function offerScoreForm(opts: {
  headline: string;
  detail: string;
  onSave: (name: string) => void;
  onSkip: () => void;
  lastName: string;
  knownNames: string[];
}): HTMLElement {
  const box = el("div", "overlay");
  box.append(el("h2", undefined, opts.headline), el("p", undefined, opts.detail));
  const form = el("div", "score-form");
  const chips = el("div", "name-chips");
  const input = document.createElement("input");
  input.maxLength = 12;
  input.placeholder = "NAME";
  input.value = opts.lastName;
  for (const name of opts.knownNames.slice(0, 6)) {
    const chip = el("button", "name-chip", name);
    chip.type = "button";
    chip.addEventListener("click", () => {
      input.value = name;
    });
    chips.append(chip);
  }
  const actions = el("div", "actions");
  actions.append(
    leafButton("SAVE", () => opts.onSave(input.value)),
    leafButton("SKIP", opts.onSkip, true),
  );
  form.append(chips, input, actions);
  box.append(form);
  return box;
}

export function bindSwipe(target: HTMLElement, onSwipe: (dir: Dir) => void, onTap?: () => void) {
  let x0 = 0;
  let y0 = 0;
  let active = false;
  target.addEventListener("pointerdown", (e) => {
    active = true;
    x0 = e.clientX;
    y0 = e.clientY;
    target.setPointerCapture(e.pointerId);
  });
  target.addEventListener("pointerup", (e) => {
    if (!active) return;
    active = false;
    const dir = dirFromSwipe(e.clientX - x0, e.clientY - y0);
    if (dir) onSwipe(dir);
    else onTap?.();
  });
}

export function frameLoop(onFrame: (dt: number) => void): () => void {
  let last = performance.now();
  let raf = 0;
  const tick = (now: number) => {
    const dt = Math.min(0.033, (now - last) / 1000);
    last = now;
    onFrame(dt);
    raf = requestAnimationFrame(tick);
  };
  raf = requestAnimationFrame(tick);
  return () => cancelAnimationFrame(raf);
}

export function maybeOfferScore(
  pit: HTMLElement,
  opts: {
    boardKey: string;
    score: number;
    lowerBetter: boolean;
    headline: string;
    detail: string;
    win?: boolean;
    onDone: () => void;
  },
) {
  const win = opts.win ?? true;
  if (!scores.beatsBest(opts.boardKey, opts.score, opts.lowerBetter, win)) {
    scores.submit(opts.boardKey, scores.lastName || "PLAYER", opts.score, win);
    opts.onDone();
    return;
  }
  const form = offerScoreForm({
    headline: opts.headline,
    detail: opts.detail,
    lastName: scores.lastName,
    knownNames: scores.knownNames(),
    onSave: (name) => {
      scores.submit(opts.boardKey, name, opts.score, win);
      form.remove();
      opts.onDone();
    },
    onSkip: () => {
      scores.submit(opts.boardKey, scores.lastName || "PLAYER", opts.score, win);
      form.remove();
      opts.onDone();
    },
  });
  pit.append(form);
}
