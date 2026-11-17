import { Modal as BsModal } from 'react-bootstrap';

export default function Modal({ show, onHide, title, children, footer, size }) {
  return (
    <BsModal show={show} onHide={onHide} size={size} centered>
      <BsModal.Header closeButton>
        <BsModal.Title as="h5">{title}</BsModal.Title>
      </BsModal.Header>
      <BsModal.Body>{children}</BsModal.Body>
      {footer && <BsModal.Footer>{footer}</BsModal.Footer>}
    </BsModal>
  );
}
