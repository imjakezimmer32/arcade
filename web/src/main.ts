import "./style.css";
import {
  GAMES,
  formatScore,
  gameById,
  scoreKeys,
  type GameId,
  type GameMeta,
} from "./core/catalog";
import { scores } from "./core/scores";
import { clear, el, type GameHandle } from "./core/ui";
import { mountBoxes } from "./games/boxes";
import { mountBreakout } from "./games/breakout";
import { mountCatch } from "./games/catch";
import { mountCheckers } from "./games/checkers";
import { mountChess } from "./games/chess";
import { mountDodge } from "./games/dodge";
import { mountEcho } from "./games/echo";
import { mountFlip } from "./games/flip";
import { mountFlit } from "./games/flit";
import { mountFour } from "./games/four";
import { mountHop } from "./games/hop";
import { mountInvaders } from "./games/invaders";
import { mountMemory } from "./games/memory";
import { mountMerge } from "./games/merge";
import { mountMines } from "./games/mines";
import { mountPeck } from "./games/peck";
import { mountPong } from "./games/pong";
import { mountRocks } from "./games/rocks";
import { mountSlide } from "./games/slide";
import { mountSnake } from "./games/snake";
import { mountStacks } from "./games/stacks";

type Route = { screen: "menu" } | { screen: "scores" } | { screen: "play"; id: GameId };

const appEl = document.querySelector<HTMLElement>("#app");
if (!appEl) throw new Error("#app missing");
const app: HTMLElement = appEl;

const mounts: Record<GameId, (root: HTMLElement, onBack: () => void) => GameHandle> = {
  snake: mountSnake,
  mines: mountMines,
  breakout: mountBreakout,
  stacks: mountStacks,
  merge: mountMerge,
  pong: mountPong,
  invaders: mountInvaders,
  flit: mountFlit,
  memory: mountMemory,
  dodge: mountDodge,
  checkers: mountCheckers,
  chess: mountChess,
  four: mountFour,
  flip: mountFlip,
  hop: mountHop,
  echo: mountEcho,
  rocks: mountRocks,
  peck: mountPeck,
  catch: mountCatch,
  slide: mountSlide,
  boxes: mountBoxes,
};

let route: Route = { screen: "menu" };
let active: GameHandle | null = null;

function go(next: Route) {
  active?.destroy();
  active = null;
  route = next;
  render();
}

function caption(meta: GameMeta): string {
  if (meta.hotSeat) return "hot seat";
  const bests = scoreKeys(meta)
    .map((key) => scores.best(key, !!meta.lowerBetter))
    .filter((row): row is NonNullable<typeof row> => row != null);
  const best = meta.lowerBetter
    ? bests.slice().sort((a, b) => a.score - b.score)[0]
    : bests.slice().sort((a, b) => b.score - a.score)[0];
  if (!best) return meta.seatChoice ? "solo · 2P" : "play";
  return formatScore(best.score, !!meta.lowerBetter);
}

function renderMenu() {
  clear(app);
  app.append(el("h1", "brand", "ARCADE"), el("p", "sub", "Apps on the cabinet"));
  const grid = el("div", "grid");
  for (const meta of GAMES) {
    const tile = el("button", "tile");
    tile.type = "button";
    tile.append(
      el("div", "tile-icon", meta.glyph),
      el("div", "tile-title", meta.title),
      el("div", "tile-cap", caption(meta)),
    );
    tile.addEventListener("click", () => go({ screen: "play", id: meta.id }));
    grid.append(tile);
  }
  const scoresTile = el("button", "tile");
  scoresTile.type = "button";
  scoresTile.append(
    el("div", "tile-icon", "★"),
    el("div", "tile-title", "SCORES"),
    el("div", "tile-cap", "hall"),
  );
  scoresTile.addEventListener("click", () => go({ screen: "scores" }));
  grid.append(scoresTile);
  app.append(grid);
}

function renderScores() {
  clear(app);
  const backRow = el("div", "hud");
  const back = el("button", "icon-btn", "←");
  back.type = "button";
  back.addEventListener("click", () => go({ screen: "menu" }));
  const titles = el("div");
  titles.append(el("div", "hud-title", "SCORES"), el("div", "hud-sub", "hall of leaf"));
  const left = el("div", "hud-left");
  left.append(back, titles);
  backRow.append(left);
  app.append(backRow);

  let selected = GAMES[0]!;
  let minesKey = "mines_beginner";
  const shell = el("div", "leader");
  const chips = el("div", "chip-row");
  const body = el("div");

  const paint = () => {
    clear(chips);
    for (const meta of GAMES) {
      const chip = el("button", selected.id === meta.id ? "chip on" : "chip", meta.title);
      chip.type = "button";
      chip.addEventListener("click", () => {
        selected = meta;
        paint();
      });
      chips.append(chip);
    }
    clear(body);
    if (selected.id === "mines") {
      const row = el("div", "chip-row");
      for (const [key, label] of [
        ["mines_beginner", "Easy"],
        ["mines_intermediate", "Med"],
        ["mines_expert", "Hard"],
      ] as const) {
        const chip = el("button", minesKey === key ? "chip on" : "chip", label);
        chip.type = "button";
        chip.addEventListener("click", () => {
          minesKey = key;
          paint();
        });
        row.append(chip);
      }
      body.append(row);
    }
    const key = selected.id === "mines" ? minesKey : selected.boardKey;
    const lower = !!selected.lowerBetter;
    const podium = el("div", "podium");
    podium.append(el("div", "hud-sub", "TOP 3"));
    const top = scores.top(key, lower);
    if (top.length === 0) podium.append(el("p", "muted", "No runs yet."));
    top.forEach((row, i) => {
      const line = el("div", "row-line");
      line.append(
        el("span", undefined, `${i + 1}. ${row.name}`),
        el("span", undefined, formatScore(row.score, lower)),
      );
      podium.append(line);
    });
    const history = el("div", "history");
    history.append(el("div", "hud-sub", "HISTORY"));
    const hist = scores.history(key);
    if (hist.length === 0) history.append(el("p", "muted", "Empty soil."));
    for (const row of hist.slice(0, 40)) {
      const line = el("div", "row-line");
      const when = new Date(row.at).toLocaleString(undefined, {
        month: "short",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit",
      });
      line.append(
        el("span", undefined, `${row.name}${row.win ? "" : " · loss"}`),
        el("span", "muted", `${formatScore(row.score, lower)} · ${when}`),
      );
      history.append(line);
    }
    body.append(podium, history);
  };

  paint();
  shell.append(chips, body);
  app.append(shell);
}

function renderPlay(id: GameId) {
  clear(app);
  const meta = gameById(id);
  if (!meta) {
    go({ screen: "menu" });
    return;
  }
  active = mounts[id](app, () => go({ screen: "menu" }));
}

function render() {
  if (route.screen === "menu") renderMenu();
  else if (route.screen === "scores") renderScores();
  else renderPlay(route.id);
}

render();
