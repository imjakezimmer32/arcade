export type ScoreRow = {
  boardKey: string;
  name: string;
  score: number;
  at: number;
  win: boolean;
};

type Snapshot = {
  lastName: string;
  rows: ScoreRow[];
};

const KEY = "arcade_web_scores_v1";
const NAME_MAX = 12;
const PODIUM = 3;

function read(): Snapshot {
  try {
    const raw = localStorage.getItem(KEY);
    if (!raw) return { lastName: "", rows: [] };
    const parsed = JSON.parse(raw) as Snapshot;
    return {
      lastName: parsed.lastName ?? "",
      rows: Array.isArray(parsed.rows) ? parsed.rows : [],
    };
  } catch {
    return { lastName: "", rows: [] };
  }
}

function write(snap: Snapshot) {
  localStorage.setItem(KEY, JSON.stringify(snap));
}

export const scores = {
  get lastName() {
    return read().lastName;
  },
  set lastName(value: string) {
    const snap = read();
    snap.lastName = value.trim().slice(0, NAME_MAX);
    write(snap);
  },
  list(boardKey: string) {
    return read().rows.filter((r) => r.boardKey === boardKey);
  },
  top(boardKey: string, lowerBetter: boolean, limit = PODIUM) {
    const pool = this.list(boardKey).filter((r) => (lowerBetter ? r.win : true));
    return sort(pool, lowerBetter).slice(0, limit);
  },
  history(boardKey: string) {
    return this.list(boardKey).slice().sort((a, b) => b.at - a.at);
  },
  best(boardKey: string, lowerBetter: boolean) {
    return this.top(boardKey, lowerBetter, 1)[0] ?? null;
  },
  beatsBest(boardKey: string, score: number, lowerBetter: boolean, win = true) {
    if (lowerBetter && !win) return false;
    const champ = this.best(boardKey, lowerBetter);
    if (!champ) return true;
    return lowerBetter ? score < champ.score : score > champ.score;
  },
  knownNames() {
    const latest = new Map<string, number>();
    for (const row of read().rows) {
      const prev = latest.get(row.name);
      if (prev == null || row.at > prev) latest.set(row.name, row.at);
    }
    if (this.lastName) latest.set(this.lastName, Number.MAX_SAFE_INTEGER);
    return [...latest.entries()].sort((a, b) => b[1] - a[1]).map(([n]) => n);
  },
  submit(boardKey: string, name: string, score: number, win = true): ScoreRow {
    const cleaned = (name.trim() || "PLAYER").slice(0, NAME_MAX).toUpperCase();
    const row: ScoreRow = { boardKey, name: cleaned, score, at: Date.now(), win };
    const snap = read();
    snap.lastName = cleaned;
    snap.rows.push(row);
    write(snap);
    return row;
  },
};

function sort(rows: ScoreRow[], lowerBetter: boolean) {
  return rows.slice().sort((a, b) => {
    if (a.score !== b.score) return lowerBetter ? a.score - b.score : b.score - a.score;
    return a.at - b.at;
  });
}

export { NAME_MAX, PODIUM };
