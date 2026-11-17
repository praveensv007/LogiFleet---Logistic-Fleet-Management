import { Card } from 'react-bootstrap';
import { Link } from 'react-router-dom';

const COLORS = {
  navy: ['#e7edf6', '#1d3557'],
  sand: ['#fdf0e5', '#c66a1d'],
  green: ['#e6f6ec', '#1e7d45'],
  red: ['#fdeaea', '#b42318'],
  blue: ['#e8f1fd', '#2a5a9e'],
  purple: ['#f0ebfb', '#6941c6'],
};

export default function StatCard({ icon, label, value, color = 'navy', to }) {
  const [bg, fg] = COLORS[color] || COLORS.navy;
  const body = (
    <Card className="h-100">
      <div className="stat-card">
        <div className="icon" style={{ background: bg, color: fg }}><i className={`bi bi-${icon}`} /></div>
        <div>
          <div className="value">{value ?? '—'}</div>
          <div className="label">{label}</div>
        </div>
      </div>
    </Card>
  );
  return to ? <Link to={to} className="text-decoration-none text-reset">{body}</Link> : body;
}
