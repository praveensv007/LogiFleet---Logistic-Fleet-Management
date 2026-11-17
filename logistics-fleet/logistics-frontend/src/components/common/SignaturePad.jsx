import { useEffect, useRef, useState } from 'react';
import { Button } from 'react-bootstrap';

/** Minimal canvas signature pad. getBlob() returns a PNG Blob or null when empty. */
export default function SignaturePad({ padRef }) {
  const canvas = useRef(null);
  const drawing = useRef(false);
  const [empty, setEmpty] = useState(true);

  useEffect(() => {
    const c = canvas.current;
    const ratio = window.devicePixelRatio || 1;
    c.width = c.offsetWidth * ratio;
    c.height = c.offsetHeight * ratio;
    const ctx = c.getContext('2d');
    ctx.scale(ratio, ratio);
    ctx.lineWidth = 2.2;
    ctx.lineCap = 'round';
    ctx.strokeStyle = '#1d3557';
  }, []);

  const point = (e) => {
    const rect = canvas.current.getBoundingClientRect();
    return { x: e.clientX - rect.left, y: e.clientY - rect.top };
  };

  const down = (e) => {
    drawing.current = true;
    const ctx = canvas.current.getContext('2d');
    const p = point(e);
    ctx.beginPath();
    ctx.moveTo(p.x, p.y);
    canvas.current.setPointerCapture(e.pointerId);
  };

  const move = (e) => {
    if (!drawing.current) return;
    const ctx = canvas.current.getContext('2d');
    const p = point(e);
    ctx.lineTo(p.x, p.y);
    ctx.stroke();
    setEmpty(false);
  };

  const up = () => { drawing.current = false; };

  const clear = () => {
    const c = canvas.current;
    c.getContext('2d').clearRect(0, 0, c.width, c.height);
    setEmpty(true);
  };

  if (padRef) {
    padRef.current = {
      isEmpty: () => empty,
      getBlob: () => new Promise((resolve) => {
        if (empty) resolve(null);
        else canvas.current.toBlob(resolve, 'image/png');
      }),
      clear,
    };
  }

  return (
    <div>
      <canvas ref={canvas} className="signature-pad" onPointerDown={down} onPointerMove={move} onPointerUp={up}
        onPointerLeave={up} />
      <div className="d-flex justify-content-between small mt-1">
        <span className="text-muted">{empty ? 'Ask the receiver to sign above' : 'Signature captured'}</span>
        <Button size="sm" variant="link" className="p-0" onClick={clear}>Clear</Button>
      </div>
    </div>
  );
}
