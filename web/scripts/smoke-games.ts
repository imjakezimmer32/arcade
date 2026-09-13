/**
 * Smoke: mount each cabinet, poke controls, destroy.
 * Run: npx tsx scripts/smoke-games.ts
 */
import { JSDOM } from "jsdom";

const dom = new JSDOM("<!doctype html><html><body><div id='app'></div></body></html>", {
  url: "http://localhost/",
  pretendToBeVisual: true,
});
const { window } = dom;
Object.assign(globalThis, {
  window,
  document: window.document,
  HTMLElement: window.HTMLElement,
  HTMLButtonElement: window.HTMLButtonElement,
  Element: window.Element,
  Node: window.Node,
  localStorage: window.localStorage,
  performance: { now: () => Date.now() },
  requestAnimationFrame: (cb: FrameRequestCallback) =>
    window.setTimeout(() => cb(Date.now()), 16) as unknown as number,
  cancelAnimationFrame: (id: number) => window.clearTimeout(id),
});

const root = document.getElementById("app")!;

async function run() {
  const mods = [
    ["merge", () => import("../src/games/merge.ts").then((m) => m.mountMerge)],
    ["slide", () => import("../src/games/slide.ts").then((m) => m.mountSlide)],
    ["echo", () => import("../src/games/echo.ts").then((m) => m.mountEcho)],
    ["memory", () => import("../src/games/memory.ts").then((m) => m.mountMemory)],
    ["mines", () => import("../src/games/mines.ts").then((m) => m.mountMines)],
    ["four", () => import("../src/games/four.ts").then((m) => m.mountFour)],
    ["flip", () => import("../src/games/flip.ts").then((m) => m.mountFlip)],
    ["boxes", () => import("../src/games/boxes.ts").then((m) => m.mountBoxes)],
  ] as const;

  for (const [name, load] of mods) {
    root.replaceChildren();
    const mount = await load();
    const handle = mount(root, () => {});
    if (!root.querySelector(".game-root")) throw new Error(`${name}: missing .game-root`);
    if (!root.querySelector(".hud")) throw new Error(`${name}: missing .hud`);
    if (!root.querySelector(".pit")) throw new Error(`${name}: missing .pit`);
    handle.destroy();
    if (root.childNodes.length) throw new Error(`${name}: destroy left children`);
    console.log(`ok ${name}`);
  }
  console.log("all cabinets mounted cleanly");
}

run().catch((e) => {
  console.error(e);
  process.exit(1);
});
