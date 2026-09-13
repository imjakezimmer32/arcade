import { el, leafButton } from "../core/ui";
import { scores } from "../core/scores";

/** Shared hot-seat / end overlays used by board games. */

export function clearOverlays(pit: HTMLElement) {
  pit.querySelectorAll(".overlay").forEach((n) => n.remove());
}

export function endOverlay(
  pit: HTMLElement,
  opts: {
    title: string;
    detail: string;
    retryLabel?: string;
    onRetry: () => void;
  },
) {
  clearOverlays(pit);
  const box = el("div", "overlay");
  box.append(el("h2", undefined, opts.title), el("p", undefined, opts.detail));
  const actions = el("div", "actions");
  actions.append(leafButton(opts.retryLabel ?? "AGAIN", opts.onRetry));
  box.append(actions);
  pit.append(box);
  return box;
}

export function passOverlay(pit: HTMLElement, name: string, onReady: () => void) {
  clearOverlays(pit);
  const box = el("div", "overlay");
  box.append(
    el("p", undefined, "PASS THE PHONE"),
    el("h2", undefined, name),
    el("p", undefined, "Tap when you're seated."),
  );
  box.addEventListener("click", onReady);
  pit.append(box);
  return box;
}

export function seatResult(
  pit: HTMLElement,
  opts: { title: string; detail: string; onRematch: () => void; onLobby: () => void },
) {
  clearOverlays(pit);
  const box = el("div", "overlay");
  box.append(el("h2", undefined, opts.title), el("p", undefined, opts.detail));
  const actions = el("div", "actions");
  actions.append(leafButton("REMATCH", opts.onRematch), leafButton("NEW", opts.onLobby, true));
  box.append(actions);
  pit.append(box);
  return box;
}

export function hotSeatLobby(
  pit: HTMLElement,
  opts: {
    title: string;
    leafRole: string;
    headRole: string;
    blurb: string;
    onStart: (leaf: string, head: string) => void;
  },
) {
  clearOverlays(pit);
  const box = el("div", "overlay");
  box.append(el("h2", undefined, opts.title), el("p", undefined, opts.blurb));

  const form = el("div", "score-form");
  const leafInput = document.createElement("input");
  leafInput.maxLength = 12;
  leafInput.placeholder = opts.leafRole;
  leafInput.value = scores.lastName || "LEAF";
  leafInput.style.borderColor = "var(--leaf)";

  const headInput = document.createElement("input");
  headInput.maxLength = 12;
  headInput.placeholder = opts.headRole;
  headInput.value = "HEAD";
  headInput.style.borderColor = "var(--head)";

  const names = scores.knownNames();
  const chips = el("div", "name-chips");
  for (const name of names.slice(0, 6)) {
    const chip = el("button", "name-chip", name);
    chip.type = "button";
    chip.addEventListener("click", () => {
      if (document.activeElement === headInput) headInput.value = name;
      else leafInput.value = name;
    });
    chips.append(chip);
  }

  form.append(
    el("p", undefined, `Bottom · ${opts.leafRole}`),
    chips,
    leafInput,
    el("p", undefined, `Top · ${opts.headRole}`),
    headInput,
  );
  const actions = el("div", "actions");
  actions.append(
    leafButton("SIT DOWN", () => {
      let a = leafInput.value.trim().toUpperCase() || "LEAF";
      let b = headInput.value.trim().toUpperCase() || "HEAD";
      a = a.slice(0, 12);
      b = b.slice(0, 12);
      if (b === a) b = a === "HEAD" ? "LEAF" : "HEAD";
      opts.onStart(a, b);
    }),
  );
  form.append(actions);
  box.append(form);
  pit.append(box);
  return box;
}

export function recordHotSeat(
  boardKey: string,
  leafName: string,
  leafScore: number,
  headName: string,
  headScore: number,
) {
  if (leafScore > headScore) {
    scores.submit(boardKey, leafName, leafScore, true);
    scores.submit(boardKey, headName, headScore, false);
  } else if (headScore > leafScore) {
    scores.submit(boardKey, headName, headScore, true);
    scores.submit(boardKey, leafName, leafScore, false);
  } else {
    scores.submit(boardKey, leafName, leafScore, false);
    scores.submit(boardKey, headName, headScore, false);
  }
}
