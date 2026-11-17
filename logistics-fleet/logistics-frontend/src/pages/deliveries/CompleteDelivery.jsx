import { useEffect, useMemo, useRef, useState } from 'react';
import { Alert, Button, Card, Col, Form, Row, Spinner } from 'react-bootstrap';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { toast } from 'react-toastify';
import PageHeader from '../../components/common/PageHeader';
import { FieldGrid, submitHandler } from '../../components/common/FormBuilder';
import SignaturePad from '../../components/common/SignaturePad';
import Loader, { ErrorAlert } from '../../components/common/Loader';
import useFetch from '../../hooks/useFetch';
import deliveryService from '../../services/deliveryService';
import authService from '../../services/authService';
import { clean, errorMessage, formatDateTime, formatMoney } from '../../utils/formatters';
import { PATTERNS, v, yup } from '../../utils/validators';

/** OTP-verified delivery with signature and / or POD photo, plus COD collection. */
export default function CompleteDelivery() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { data: d, loading, error, reload } = useFetch(() => deliveryService.get(id), [id]);
  const [otpInfo, setOtpInfo] = useState(null);
  const [sending, setSending] = useState(false);
  const [busy, setBusy] = useState(false);
  const pad = useRef(null);
  const isCod = d?.paymentMode === 'COD';

  // isCod / codDue travel as hidden form values so the schema can stay static.
  // const schema = useMemo(() => yup.object({
  //   receiverName: v.req('Receiver name').max(100),
  //   receiverPhone: v.phone(),
  //   otp: yup.string().trim().required('Delivery OTP is required').matches(PATTERNS.otp, 'OTP is 6 digits'),
  //   podPhotoUrl: yup.string().nullable(),
  //   isCod: yup.boolean().strip(),
  //   codDue: yup.mixed().strip(),
  //   codAmountCollected: yup.mixed().when('isCod', {
  //     is: true,
  //     then: () => v.num('COD amount').required('COD amount is required').test('due', 'Collect the full COD amount',
  //       function due(x) { return x >= Number(this.parent.codDue || 0); }),
  //     otherwise: (s) => s.strip(),
  //   }),
  // }), []);
const schema = useMemo(() => yup.object({
    receiverName: v.req('Receiver name').max(100),

    receiverPhone: v.phone(),

    otp: yup.string()
        .trim()
        .required('Delivery OTP is required')
        .matches(PATTERNS.otp, 'OTP is 6 digits'),

    podPhotoUrl: yup.string().nullable(),

    codAmountCollected: isCod
        ? v.num('COD amount')
            .required('COD amount is required')
            .min(
                Number(d?.codAmountDue || 0),
                `Collect at least ${formatMoney(d?.codAmountDue)}`
            )
        : yup.mixed().strip(),

}), [isCod, d?.codAmountDue]);

  const form = useForm({ resolver: yupResolver(schema), mode: 'onTouched' });
  useEffect(() => {
    if (!d) return;
    // form.reset({
    //   receiverName: d.deliveryAddress?.contactName || '', receiverPhone: d.deliveryAddress?.contactPhone || '',
    //   otp: '', podPhotoUrl: '', isCod: d.paymentMode === 'COD', codDue: d.codAmountDue,
    //   codAmountCollected: d.paymentMode === 'COD' ? d.codAmountDue : '',
    // });
    form.reset({
    receiverName: d.deliveryAddress?.contactName || '',
    receiverPhone: d.deliveryAddress?.contactPhone || '',
    otp: '',
    podPhotoUrl: '',
    codAmountCollected:
        d.paymentMode === 'COD'
            ? Number(d.codAmountDue)
            : '',
});
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [d?.id]);

  if (loading) return <Loader />;
  if (error) return <ErrorAlert error={error} onRetry={reload} />;
  if (d.currentStatus !== 'OUT_FOR_DELIVERY') {
    return <Alert variant="warning">This delivery is {d.currentStatus || 'not picked up yet'}. <Link to={`/deliveries/${id}`}>Back to trip</Link></Alert>;
  }

  const sendOtp = async () => {
    setSending(true);
    try {
      const res = await deliveryService.sendOtp(id);
      setOtpInfo({ ...res.data, at: new Date() });
      toast.success(res.message);
    } catch (e) {
      toast.error(errorMessage(e));
    } finally {
      setSending(false);
    }
  };

  const onSubmit = form.handleSubmit(async (values) => {
    setBusy(true);
    await submitHandler(form, async (vals) => {
      let signatureUrl = null;
      const blob = await pad.current?.getBlob();
      if (blob) {
        const up = await authService.uploadFile(new File([blob], 'signature.png', { type: 'image/png' }), 'photo');
        signatureUrl = up.url;
      }
      if (!signatureUrl && !vals.podPhotoUrl) {
        toast.error('Capture a signature or upload a POD photo');
        return;
      }
      // await deliveryService.complete(id, clean({ ...vals, signatureUrl }));
      await deliveryService.complete(id, clean({
    receiverName: vals.receiverName,
    receiverPhone: vals.receiverPhone,
    otp: vals.otp,
    signatureUrl,
    podPhotoUrl: vals.podPhotoUrl,
    codAmountCollected: isCod
        ? Number(vals.codAmountCollected)
        : null,
}));
      toast.success('Delivered! Customer and fleet manager notified.');
      navigate('/deliveries/my');
    })(values);
    setBusy(false);
  });

  return (
    <>
      <PageHeader title="Complete delivery" subtitle={<><span className="mono">{d.trackingNumber}</span> · {d.deliveryAddress?.addressLine1}, {d.deliveryAddress?.city}</>} />
      <Form noValidate onSubmit={onSubmit}>
        <Row className="g-3">
          <Col lg={7}>
            <Card className="mb-3">
              <Card.Header>1 · Send OTP to the receiver</Card.Header>
              <Card.Body>
                <div className="d-flex align-items-center gap-3 flex-wrap">
                  <Button variant="outline-primary" onClick={sendOtp} disabled={sending}>
                    {sending ? <Spinner size="sm" /> : <i className="bi bi-send me-1" />} {otpInfo || d.otpSent ? 'Resend OTP' : 'Send OTP'}
                  </Button>
                  {otpInfo && <span className="small text-muted">Sent to {otpInfo.sentTo} at {formatDateTime(otpInfo.at)}</span>}
                </div>
                {otpInfo?.devOtp && (
                  <Alert variant="secondary" className="mt-3 mb-0 small">
                    Demo mode (no SMS gateway): the OTP is <strong className="mono fs-6">{otpInfo.devOtp}</strong>
                  </Alert>
                )}
              </Card.Body>
            </Card>
            <Card>
              <Card.Header>2 · Receiver &amp; OTP</Card.Header>
              <Card.Body>
                <FieldGrid form={form} fields={[
                  { name: 'receiverName', label: 'Receiver name', required: true },
                  { name: 'receiverPhone', label: 'Receiver phone', required: true },
                  { name: 'otp', label: 'Delivery OTP', required: true, placeholder: '6 digits', autoComplete: 'one-time-code' },
                  ...(isCod ? [{ name: 'codAmountCollected', label: `COD amount collected (due ${formatMoney(d.codAmountDue)})`, type: 'number', step: '0.01', required: true }] : []),
                ]} />
                <div className="small text-muted">Delivered time is recorded automatically.</div>
              </Card.Body>
            </Card>
          </Col>
          <Col lg={5}>
            <Card>
              <Card.Header>3 · Proof of delivery</Card.Header>
              <Card.Body>
                <Form.Label>Signature</Form.Label>
                <SignaturePad padRef={pad} />
                <div className="my-3 text-center small text-muted">and / or</div>
                <FieldGrid form={form} fields={[{ name: 'podPhotoUrl', label: 'POD photo', type: 'file', category: 'photo', col: 12 }]} />
                <Button type="submit" variant="success" size="lg" className="w-100" disabled={busy}>
                  {busy && <Spinner size="sm" className="me-2" />}Confirm delivery
                </Button>
              </Card.Body>
            </Card>
          </Col>
        </Row>
      </Form>
    </>
  );
}
