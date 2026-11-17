import { useEffect, useState } from 'react';
import { Col, Form, InputGroup, Row } from 'react-bootstrap';
import { pretty } from '../../utils/formatters';

/**
 * Search box + optional dropdown / date filters.
 * filters: [{ name, label, options: [..] | [{value,label}], type: 'select' | 'date' }]
 * Calls onChange({ search, ...filters }) with a 350ms debounce on the text box.
 */
export default function SearchFilter({ value, onChange, filters = [], placeholder = 'Search…', showSearch = true }) {
  const [text, setText] = useState(value?.search || '');

  useEffect(() => {
    if (!showSearch) return undefined;
    const t = setTimeout(() => {
      if ((value?.search || '') !== text) onChange({ ...value, search: text });
    }, 350);
    return () => clearTimeout(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [text]);

  return (
    <Row className="g-2 p-3 border-bottom">
      {showSearch && (
        <Col md={4}>
          <InputGroup size="sm">
            <InputGroup.Text><i className="bi bi-search" /></InputGroup.Text>
            <Form.Control value={text} placeholder={placeholder} onChange={(e) => setText(e.target.value)} />
          </InputGroup>
        </Col>
      )}
      {filters.map((f) => (
        <Col key={f.name} md={f.md || 2} xs={6}>
          {f.type === 'date' ? (
            <Form.Control size="sm" type="date" value={value?.[f.name] || ''} title={f.label}
              onChange={(e) => onChange({ ...value, [f.name]: e.target.value })} />
          ) : (
            <Form.Select size="sm" value={value?.[f.name] ?? ''}
              onChange={(e) => onChange({ ...value, [f.name]: e.target.value })}>
              <option value="">{f.label}: all</option>
              {f.options.map((o) => {
                const opt = typeof o === 'object' ? o : { value: o, label: pretty(o) };
                return <option key={opt.value} value={opt.value}>{opt.label}</option>;
              })}
            </Form.Select>
          )}
        </Col>
      ))}
    </Row>
  );
}
