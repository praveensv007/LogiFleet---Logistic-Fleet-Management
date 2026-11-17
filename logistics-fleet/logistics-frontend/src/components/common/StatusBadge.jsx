import { Badge } from 'react-bootstrap';
import { STATUS_VARIANT } from '../../utils/constants';
import { pretty } from '../../utils/formatters';

export default function StatusBadge({ status, className = '' }) {
  const value = status || 'AWAITING_PICKUP';
  const variant = STATUS_VARIANT[value] || 'secondary';
  return (
    <Badge bg={variant} text={variant === 'warning' || variant === 'light' ? 'dark' : undefined} className={className}>
      {pretty(value)}
    </Badge>
  );
}
