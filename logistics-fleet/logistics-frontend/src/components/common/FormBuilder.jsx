import { useEffect, useState } from 'react';
import { Button, Col, Form, Row, Spinner } from 'react-bootstrap';
import { Controller, useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { toast } from 'react-toastify';
import FileUpload from './FileUpload';
import { applyServerErrors } from '../../utils/validators';
import { errorMessage, pretty } from '../../utils/formatters';
import { FaEye, FaEyeSlash } from 'react-icons/fa';

export const getPath = (obj, path) => path.split('.').reduce((o, k) => (o == null ? o : o[k]), obj);

const normaliseOptions = (options = []) =>
  options.map((o) => (typeof o === 'object' ? o : { value: o, label: pretty(o) }));

/**
 * Renders one field config with react-hook-form.
 * field: { name, label, type, options, col, required, placeholder, help, disabled, rows, category, min, max, step }
 */
export function FormField({ field, form }) {
  const { register, control, formState: { errors } } = form;
  const error = getPath(errors, field.name)?.message;
  const label = field.label && (
    <Form.Label htmlFor={field.name}>
      {field.label}{field.required && <span className="required-star">*</span>}
    </Form.Label>
  );
  const [showPassword, setShowPassword] = useState(false);
  let input;
  switch (field.type) {
    case 'select':
      input = (
        <Form.Select id={field.name} {...register(field.name)} isInvalid={!!error} disabled={field.disabled}>
          <option value="">{field.placeholder || 'Select…'}</option>
          {normaliseOptions(field.options).map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
        </Form.Select>
      );
      break;
    case 'textarea':
      input = (
        <Form.Control as="textarea" id={field.name} rows={field.rows || 3} placeholder={field.placeholder}
          {...register(field.name)} isInvalid={!!error} disabled={field.disabled} maxLength={field.maxLength} />
      );
      break;
    case 'checkbox':
      return (
        <Form.Group className="mb-3">
          <Form.Check id={field.name} type="checkbox" label={<>{field.label}{field.required && <span className="required-star">*</span>}</>}
            {...register(field.name)} isInvalid={!!error} disabled={field.disabled} />
          {error && <div className="invalid-feedback d-block">{error}</div>}
        </Form.Group>
      );
    case 'file':
      input = (
        <Controller name={field.name} control={control} render={({ field: f }) => (
          <FileUpload value={f.value} onChange={f.onChange} category={field.category} isInvalid={!!error} disabled={field.disabled} />
        )} />
      );
      break;
    case 'switch':
      return (
        <Form.Group className="mb-3">
          <Form.Check type="switch" id={field.name} label={field.label} {...register(field.name)} disabled={field.disabled} />
        </Form.Group>
      );
      case 'password':
  input = (
    <div className="position-relative">
      <Form.Control
        id={field.name}
        type={showPassword ? 'text' : 'password'}
        placeholder={field.placeholder}
        {...register(field.name)}
        isInvalid={!!error}
        disabled={field.disabled}
        autoComplete={field.autoComplete}
        style={{ paddingRight: '45px' }}
      />

      <button
        type="button"
        className="password-toggle"
        onClick={() => setShowPassword((prev) => !prev)}
        aria-label={showPassword ? 'Hide password' : 'Show password'}
      >
        {showPassword ? <FaEyeSlash /> : <FaEye />}
      </button>
    </div>
  );
  break;
    default:
      input = (
        <Form.Control id={field.name} type={field.type || 'text'} placeholder={field.placeholder}
          {...register(field.name)} isInvalid={!!error} disabled={field.disabled} readOnly={field.readOnly}
          min={field.min} max={field.max} step={field.step} autoComplete={field.autoComplete} />
      );
  }
  return (
    <Form.Group className="mb-3">
      {label}
      {input}
      {error ? <div className="invalid-feedback d-block">{error}</div>
        : field.help && <Form.Text className="text-muted">{field.help}</Form.Text>}
    </Form.Group>
  );
}

/** Lays out a list of field configs; { section: 'Title' } entries become headings. */
export function FieldGrid({ fields, form }) {
  const values = form.watch();
  return (
    <Row>
      {fields.map((f, i) => {
        if (f.section) {
          return <Col xs={12} key={`s-${i}`}><div className="form-section-title">{f.section}</div></Col>;
        }
        if (f.showIf && !f.showIf(values)) return null;
        return (
          <Col key={f.name} md={f.col || 6}>
            <FormField field={f} form={form} />
          </Col>
        );
      })}
    </Row>
  );
}

/** Wires error handling: server field errors go under inputs, everything else to a toast. */
export const submitHandler = (form, onSubmit) => async (values) => {
  try {
    await onSubmit(values, form);
  } catch (err) {
    const hasFieldErrors = applyServerErrors(err, form.setError);
    toast.error(hasFieldErrors ? 'Please fix the highlighted fields' : errorMessage(err));
  }
};

/**
 * Generic form: fields + yup schema + submit. Used by most create / edit screens.
 */
export default function FormBuilder({ fields, schema, defaultValues, onSubmit, submitLabel = 'Save', onCancel,
  children, footerExtra }) {
  const form = useForm({ resolver: schema ? yupResolver(schema) : undefined, defaultValues, mode: 'onTouched' });
  const [busy, setBusy] = useState(false);

  // Reset only when the default *content* changes, so parents may pass inline objects safely.
  const defaultsKey = JSON.stringify(defaultValues ?? null);
  useEffect(() => {
    if (defaultValues) form.reset(defaultValues);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [defaultsKey]);

  const handle = form.handleSubmit(async (values) => {
    setBusy(true);
    try {
      await submitHandler(form, onSubmit)(values);
    } finally {
      setBusy(false);
    }
  });

  return (
    <Form noValidate onSubmit={handle}>
      <FieldGrid fields={fields} form={form} />
      {typeof children === 'function' ? children(form) : children}
      <div className="d-flex gap-2 justify-content-end align-items-center mt-2">
        {footerExtra}
        {onCancel && <Button variant="light" onClick={onCancel} disabled={busy}>Cancel</Button>}
        <Button type="submit" disabled={busy}>
          {busy && <Spinner size="sm" className="me-2" />}{submitLabel}
        </Button>
      </div>
    </Form>
  );
}
