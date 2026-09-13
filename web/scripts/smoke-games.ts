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
  HTMLCanvasElement: window.HTMLCanvasElement,
  Element: window.Element,
  Node: window.Node,
  localStorage: window.localStorage,
  performance: { now: () => Date.now() },
  devicePixelRatio: 1,
  requestAnimationFrame: (cb: FrameRequestCallback) =>
    window.setTimeout(() => cb(Date.now()), 16) as unknown as number,
  cancelAnimationFrame: (id: number) => window.clearTimeout(id),
});

// jsdom canvas stub
const proto = window.HTMLCanvasElement.prototype as HTMLCanvasElement & {
  getContext: (id: string) => CanvasRenderingContext2D | null;
};
proto.getContext = () =>
  ({
    setTransform() {},
    fillRect() {},
    strokeRect() {},
    beginPath() {},
    closePath() {},
    moveTo() {},
    lineTo() {},
    arc() {},
    ellipse() {},
    fill() {},
    stroke() {},
    save() {},
    restore() {},
    translate() {},
    rotate() {},
    arcTo() {},
    setLineDash() {},
    clearRect() {},
    fillStyle: "",
    strokeStyle: "",
    lineWidth: 1,
    globalAlpha: 1,
  }) as unknown as CanvasRenderingContext2D;

const root = document.getElementById("app")!;

async function run() {
  const mods = [
    ["snake", () => import("../src/games/snake.ts").then((m) => m.mountSnake)],
    ["breakout", () => import("../src/games/breakout.ts").then((m) => m.mountBreakout)],
    ["stacks", () => import("../src/games/stacks.ts").then((m) => m.mountStacks)],
    ["pong", () => import("../src/games/pong.ts").then((m) => m.mountPong)],
    ["flit", () => import("../src/games/flit.ts").then((m) => m.mountFlit)],
    ["dodge", () => import("../src/games/dodge.ts").then((m) => m.mountDodge)],
    ["peck", () => import("../src/games/peck.ts").then((m) => m.mountPeck)],
    ["catch", () => import("../src/games/catch.ts").then((m) => m.mountCatch)],
    ["hop", () => import("../src/games/hop.ts").then((m) => m.mountHop)],
    ["rocks", () => import("../src/games/rocks.ts").then((m) => m.mountRocks)],
    ["invaders", () => import("../src/games/invaders.ts").then((m) => m.mountInvaders)],
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
    await new Promise((r) => setTimeout(r, 40));
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
