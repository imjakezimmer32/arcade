/** Logic checks ported from Kotlin engines (no DOM). */
function assert(cond: unknown, msg: string): asserts cond {
  if (!cond) throw new Error(msg);
}

// --- merge slide ---
type Dir = "up" | "down" | "left" | "right";
function slide(cells: number[], dir: Dir): { next: number[]; gained: number } {
  const next = Array(16).fill(0) as number[];
  let gained = 0;
  const vertical = dir === "up" || dir === "down";
  const reverse = dir === "right" || dir === "down";
  for (let line = 0; line < 4; line++) {
    const vals: number[] = [];
    for (let i = 0; i < 4; i++) {
      const idx = vertical ? (reverse ? 3 - i : i) * 4 + line : line * 4 + (reverse ? 3 - i : i);
      vals.push(cells[idx]!);
    }
    const compact = vals.filter((v) => v !== 0);
    const merged: number[] = [];
    let i = 0;
    while (i < compact.length) {
      if (i + 1 < compact.length && compact[i] === compact[i + 1]) {
        const v = compact[i]! * 2;
        merged.push(v);
        gained += v;
        i += 2;
      } else {
        merged.push(compact[i]!);
        i += 1;
      }
    }
    while (merged.length < 4) merged.push(0);
    for (let j = 0; j < 4; j++) {
      const idx = vertical ? (reverse ? 3 - j : j) * 4 + line : line * 4 + (reverse ? 3 - j : j);
      next[idx] = merged[j]!;
    }
  }
  return { next, gained };
}

{
  const cells = [2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0];
  const { next, gained } = slide(cells, "left");
  assert(next[0] === 4 && gained === 4, "merge left 2+2");
}

// --- four line ---
const COLS = 7;
const ROWS = 6;
function fourLine(board: number[], col: number, row: number, piece: number): number[] {
  const dirs = [[1, 0], [0, 1], [1, 1], [1, -1]];
  for (const [dx, dy] of dirs) {
    const cells = [row * COLS + col];
    let c = col + dx!;
    let r = row + dy!;
    while (c >= 0 && c < COLS && r >= 0 && r < ROWS && board[r * COLS + c] === piece) {
      cells.push(r * COLS + c);
      c += dx!;
      r += dy!;
    }
    c = col - dx!;
    r = row - dy!;
    while (c >= 0 && c < COLS && r >= 0 && r < ROWS && board[r * COLS + c] === piece) {
      cells.push(r * COLS + c);
      c -= dx!;
      r -= dy!;
    }
    if (cells.length >= 4) return cells;
  }
  return [];
}
{
  const b = Array(COLS * ROWS).fill(0);
  for (let c = 0; c < 4; c++) b[(ROWS - 1) * COLS + c] = 1;
  const line = fourLine(b, 3, ROWS - 1, 1);
  assert(line.length >= 4, "four horizontal win");
}

// --- flip flips ---
function flips(board: number[], at: number, side: number): number[] {
  const opp = side === 1 ? 2 : 1;
  const c0 = at % 8;
  const r0 = (at / 8) | 0;
  const out: number[] = [];
  const dirs = [[1,0],[-1,0],[0,1],[0,-1],[1,1],[1,-1],[-1,1],[-1,-1]];
  for (const [dx, dy] of dirs) {
    const run: number[] = [];
    let c = c0 + dx!;
    let r = r0 + dy!;
    while (c >= 0 && c <= 7 && r >= 0 && r <= 7) {
      const i = r * 8 + c;
      const p = board[i]!;
      if (p === opp) run.push(i);
      else if (p === side && run.length) {
        out.push(...run);
        break;
      } else break;
      c += dx!;
      r += dy!;
    }
  }
  return out;
}
{
  const b = Array(64).fill(0);
  b[3 * 8 + 3] = 2;
  b[3 * 8 + 4] = 1;
  b[4 * 8 + 3] = 1;
  b[4 * 8 + 4] = 2;
  const f = flips(b, 2 * 8 + 3, 1); // d3 classic opening for black/leaf
  assert(f.length > 0, "flip opening has flips");
}

// --- boxes settle ---
{
  const h = Array(20).fill(false);
  const v = Array(20).fill(false);
  // close box at 0,0
  h[0] = true;
  h[4] = true;
  v[0] = true;
  v[1] = true;
  let gained = 0;
  const owner = Array(16).fill(0);
  for (let r = 0; r < 4; r++)
    for (let c = 0; c < 4; c++) {
      const idx = r * 4 + c;
      if (owner[idx]) continue;
      if (h[r * 4 + c] && h[(r + 1) * 4 + c] && v[r * 5 + c] && v[r * 5 + c + 1]) {
        owner[idx] = 1;
        gained++;
      }
    }
  assert(gained === 1 && owner[0] === 1, "boxes closes one square");
}

console.log("logic fidelity checks passed");
