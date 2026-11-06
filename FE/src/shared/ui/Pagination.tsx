type PaginationProps = {
  page: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  disabled?: boolean;
  label?: string;
};

export function Pagination({ page, totalPages, onPageChange, disabled = false, label = '페이지 이동' }: PaginationProps) {
  if (totalPages === 0) return null;
  const current = Math.min(page, totalPages - 1);
  const start = Math.floor(current / 3) * 3;
  const pages = Array.from({ length: Math.min(3, totalPages - start) }, (_, index) => start + index);

  return <nav className="pagination" aria-label={label}>
    <button type="button" aria-label="이전 페이지" disabled={disabled || current === 0} onClick={() => onPageChange(current - 1)}>‹</button>
    {pages.map((number) => <button key={number} type="button" className={number === current ? 'active' : ''}
      aria-label={`${number + 1}페이지`} aria-current={number === current ? 'page' : undefined}
      disabled={disabled || number === current} onClick={() => onPageChange(number)}>{number + 1}</button>)}
    <button type="button" aria-label="다음 페이지" disabled={disabled || current >= totalPages - 1} onClick={() => onPageChange(current + 1)}>›</button>
  </nav>;
}
