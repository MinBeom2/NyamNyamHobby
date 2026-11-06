// URL과 화면은 1부터, 백엔드 요청은 0부터 센다.
export function readPageNumber(value: string | null): number {
  if (!value || !/^\d+$/.test(value)) return 0;
  const number = Number(value);
  return Number.isSafeInteger(number) && number >= 1 && number <= 2_147_483_647 ? number - 1 : 0;
}

export function withPageNumber(params: URLSearchParams, page: number, name = 'page'): URLSearchParams {
  const next = new URLSearchParams(params);
  if (page === 0) next.delete(name);
  else next.set(name, String(page + 1));
  return next;
}
