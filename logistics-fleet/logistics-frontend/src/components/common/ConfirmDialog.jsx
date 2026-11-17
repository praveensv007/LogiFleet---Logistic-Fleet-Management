import { useEffect, useState } from 'react';
import { Button, Form, Spinner } from 'react-bootstrap';
import Modal from './Modal';

/**
 * Confirmation modal. With askReason, a reason textarea is shown and passed to onConfirm(reason).
 */
export default function ConfirmDialog({ show, title = 'Are you sure?', message, confirmLabel = 'Confirm',
  variant = 'danger', askReason = false, reasonLabel = 'Reason', onConfirm, onHide }) {
  const [reason, setReason] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (show) setReason('');
  }, [show]);

  const confirm = async () => {
    setBusy(true);
    try {
      await onConfirm(reason.trim());
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal show={show} onHide={onHide} title={title}
      footer={(
        <>
          <Button variant="light" onClick={onHide} disabled={busy}>Close</Button>
          <Button variant={variant} onClick={confirm} disabled={busy || (askReason && !reason.trim())}>
            {busy && <Spinner size="sm" className="me-2" />}{confirmLabel}
          </Button>
        </>
      )}>
      {message && <p className="mb-2">{message}</p>}
      {askReason && (
        <Form.Group>
          <Form.Label>{reasonLabel}<span className="required-star">*</span></Form.Label>
          <Form.Control as="textarea" rows={3} maxLength={255} value={reason} onChange={(e) => setReason(e.target.value)} />
        </Form.Group>
      )}
    </Modal>
  );
}
