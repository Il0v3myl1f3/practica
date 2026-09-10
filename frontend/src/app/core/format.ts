const MONTHS = ['ian', 'feb', 'mar', 'apr', 'mai', 'iun', 'iul', 'aug', 'sept', 'oct', 'noi', 'dec'];

/** "2 sept 2026, 10:12" — formatul din prototip. */
export function dateLabel(iso: string | null | undefined): string {
  if (!iso) return '—';
  const d = new Date(iso);
  if (isNaN(d.getTime())) return '—';
  const pad = (x: number) => String(x).padStart(2, '0');
  return `${d.getDate()} ${MONTHS[d.getMonth()]} ${d.getFullYear()}, ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export function plural(n: number, one: string, many: string): string {
  return `${n} ${n === 1 ? one : many}`;
}

/** Numerele paginilor cu "…" acolo unde sar, ca la paginarea MUD. */
export function pagerItems(page: number, pages: number): (number | '…')[] {
  const out: (number | '…')[] = [];
  for (let i = 0; i < pages; i++) {
    if (i === 0 || i === pages - 1 || Math.abs(i - page) <= 1) {
      out.push(i);
    } else if (out[out.length - 1] !== '…') {
      out.push('…');
    }
  }
  return out;
}
