import { useRef, useState } from 'react';
import { Button, Form, Spinner } from 'react-bootstrap';
import { toast } from 'react-toastify';
import authService from '../../services/authService';
import { errorMessage, fileUrl } from '../../utils/formatters';

/**
 * Uploads immediately to POST /files/upload and reports the stored URL through onChange(url).
 * category "photo": JPG/PNG up to 2 MB; otherwise PDF/JPG/PNG up to 5 MB.
 */
export default function FileUpload({ value, onChange, category = 'document', isInvalid, disabled }) {
  const input = useRef(null);
  const [busy, setBusy] = useState(false);
  const photo = category === 'photo';
  const accept = photo ? '.jpg,.jpeg,.png' : '.pdf,.jpg,.jpeg,.png';
  const maxMb = photo ? 2 : 5;

  const pick = async (e) => {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;
    if (file.size > maxMb * 1024 * 1024) {
      toast.error(`File must be ${maxMb} MB or smaller`);
      return;
    }
    setBusy(true);
    try {
      const res = await authService.uploadFile(file, category);
      onChange(res.url);
    } catch (err) {
      toast.error(errorMessage(err, 'Upload failed'));
    } finally {
      setBusy(false);
    }
  };

  const isImage = value && /\.(png|jpe?g)$/i.test(value);

  return (
    <div className={`d-flex align-items-center gap-2 flex-wrap ${isInvalid ? 'is-invalid' : ''}`}>
      <Form.Control ref={input} type="file" accept={accept} onChange={pick} className="d-none" />
      <Button size="sm" variant={isInvalid ? 'outline-danger' : 'outline-secondary'} disabled={busy || disabled}
        onClick={() => input.current?.click()}>
        {busy ? <Spinner size="sm" /> : <i className="bi bi-upload me-1" />} {value ? 'Replace' : 'Upload'}
      </Button>
      {value && (
        <>
          {isImage
            ? <img src={fileUrl(value)} alt="" style={{ height: 38, borderRadius: 6 }} />
            : <a href={fileUrl(value)} target="_blank" rel="noreferrer" className="small"><i className="bi bi-file-earmark-pdf me-1" />View file</a>}
          {!disabled && <Button size="sm" variant="link" className="text-danger p-0" onClick={() => onChange('')}>Remove</Button>}
        </>
      )}
      <span className="small text-muted">{photo ? 'JPG/PNG' : 'PDF/JPG/PNG'} ≤ {maxMb} MB</span>
    </div>
  );
}
