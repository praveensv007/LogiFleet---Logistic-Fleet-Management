import { Pagination as BsPagination } from 'react-bootstrap';

/** page is 0-based like Spring Data. */
export default function Pagination({ page, totalPages, totalElements, onChange }) {
  if (!totalPages || totalPages <= 1) {
    return totalElements ? <div className="small text-muted px-3 py-2">{totalElements} record(s)</div> : null;
  }
  const start = Math.max(0, Math.min(page - 2, totalPages - 5));
  const pages = Array.from({ length: Math.min(5, totalPages) }, (_, i) => start + i);
  return (
    <div className="d-flex justify-content-between align-items-center px-3 py-2 flex-wrap gap-2">
      <span className="small text-muted">{totalElements} record(s) · page {page + 1} of {totalPages}</span>
      <BsPagination size="sm" className="mb-0">
        <BsPagination.First disabled={page === 0} onClick={() => onChange(0)} />
        <BsPagination.Prev disabled={page === 0} onClick={() => onChange(page - 1)} />
        {pages.map((p) => (
          <BsPagination.Item key={p} active={p === page} onClick={() => onChange(p)}>{p + 1}</BsPagination.Item>
        ))}
        <BsPagination.Next disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)} />
        <BsPagination.Last disabled={page >= totalPages - 1} onClick={() => onChange(totalPages - 1)} />
      </BsPagination>
    </div>
  );
}
