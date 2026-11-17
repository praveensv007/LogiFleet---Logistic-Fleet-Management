import { Table } from 'react-bootstrap';
import Loader, { EmptyState } from './Loader';

/**
 * columns: [{ key, label, render?(row), className? }]
 */
export default function DataTable({ columns, rows, loading, emptyText = 'No records found', onRowClick, rowKey = 'id' }) {
  if (loading) return <Loader />;
  if (!rows || rows.length === 0) return <EmptyState text={emptyText} />;
  return (
    <div className="table-responsive">
      <Table hover className={onRowClick ? 'table-clickable' : ''}>
        <thead>
          <tr>{columns.map((c) => <th key={c.key} className={c.className}>{c.label}</th>)}</tr>
        </thead>
        <tbody>
          {rows.map((row, i) => (
            <tr key={row[rowKey] ?? i} onClick={onRowClick ? () => onRowClick(row) : undefined}>
              {columns.map((c) => (
                <td key={c.key} className={c.className}>{c.render ? c.render(row) : row[c.key] ?? '—'}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </Table>
    </div>
  );
}
