export type Dir = "up" | "down" | "left" | "right";

export const DX: Record<Dir, number> = { up: 0, down: 0, left: -1, right: 1 };
export const DY: Record<Dir, number> = { up: -1, down: 1, left: 0, right: 0 };

export function opposite(a: Dir, b: Dir): boolean {
  return DX[a] + DX[b] === 0 && DY[a] + DY[b] === 0;
}

export function dirFromSwipe(dx: number, dy: number): Dir | null {
  if (Math.abs(dx) < 24 && Math.abs(dy) < 24) return null;
  return Math.abs(dx) > Math.abs(dy) ? (dx > 0 ? "right" : "left") : dy > 0 ? "down" : "up";
}

export type GameId =
  | "snake"
  | "mines"
  | "breakout"
  | "stacks"
  | "merge"
  | "pong"
  | "invaders"
  | "flit"
  | "memory"
  | "dodge"
  | "checkers"
  | "chess"
  | "four"
  | "flip"
  | "hop"
  | "echo"
  | "rocks"
  | "peck"
  | "catch"
  | "slide"
  | "boxes";

export type GameMeta = {
  id: GameId;
  title: string;
  blurb: string;
  boardKey: string;
  lowerBetter?: boolean;
  hotSeat?: boolean;
  seatChoice?: boolean;
  glyph: string;
};

export const GAMES: GameMeta[] = [
  { id: "snake", title: "SNAKE", blurb: "Eat. Don't crash.", boardKey: "snake", seatChoice: true, glyph: "◎" },
  { id: "mines", title: "MINES", blurb: "Classic sweep.", boardKey: "mines", lowerBetter: true, glyph: "✸" },
  { id: "breakout", title: "BREAKOUT", blurb: "Keep the ball alive.", boardKey: "breakout", glyph: "▬" },
  { id: "stacks", title: "STACKS", blurb: "Clear the lines.", boardKey: "stacks", glyph: "▤" },
  { id: "merge", title: "2048", blurb: "Slide and combine.", boardKey: "merge", glyph: "▣" },
  { id: "pong", title: "PONG", blurb: "Bounce it back.", boardKey: "pong", seatChoice: true, glyph: "◍" },
  { id: "invaders", title: "INVADERS", blurb: "Shoot the sky.", boardKey: "invaders", glyph: "¤" },
  { id: "flit", title: "FLIT", blurb: "Tap to stay up.", boardKey: "flit", glyph: "✈" },
  { id: "memory", title: "MEMORY", blurb: "Match the pairs.", boardKey: "memory", lowerBetter: true, seatChoice: true, glyph: "?" },
  { id: "dodge", title: "DODGE", blurb: "Don't get hit.", boardKey: "dodge", glyph: "●" },
  { id: "checkers", title: "CHECKERS", blurb: "Hot seat. Jump them all.", boardKey: "checkers", hotSeat: true, glyph: "●" },
  { id: "chess", title: "CHESS", blurb: "Hot seat. Pass the phone.", boardKey: "chess", hotSeat: true, glyph: "♞" },
  { id: "four", title: "FOUR", blurb: "Hot seat. Four in a row.", boardKey: "four", hotSeat: true, glyph: "◉" },
  { id: "flip", title: "FLIP", blurb: "Hot seat. Reversi.", boardKey: "flip", hotSeat: true, glyph: "◐" },
  { id: "hop", title: "HOP", blurb: "Cross the garden.", boardKey: "hop", glyph: "▴" },
  { id: "echo", title: "ECHO", blurb: "Repeat the lights.", boardKey: "echo", glyph: "✦" },
  { id: "rocks", title: "ROCKS", blurb: "Split the stones.", boardKey: "rocks", glyph: "☄" },
  { id: "peck", title: "PECK", blurb: "Whack the sprouts.", boardKey: "peck", glyph: "☘" },
  { id: "catch", title: "CATCH", blurb: "Catch the berries.", boardKey: "catch", glyph: "◡" },
  { id: "slide", title: "SLIDE", blurb: "Slide into order.", boardKey: "slide", lowerBetter: true, glyph: "15" },
  { id: "boxes", title: "BOXES", blurb: "Hot seat. Close the squares.", boardKey: "boxes", hotSeat: true, glyph: "▢" },
];

export function gameById(id: string): GameMeta | undefined {
  return GAMES.find((g) => g.id === id);
}

export function scoreKeys(meta: GameMeta): string[] {
  if (meta.id === "mines") return ["mines_beginner", "mines_intermediate", "mines_expert"];
  return [meta.boardKey];
}

export function seatKey(meta: GameMeta): string {
  return `${meta.boardKey}_seat`;
}

export function formatScore(score: number, lowerBetter: boolean): string {
  if (!lowerBetter) return String(score);
  const m = Math.floor(score / 60);
  const s = score % 60;
  return `${m}:${String(s).padStart(2, "0")}`;
}
