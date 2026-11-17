import { Button, Card, Col, Row } from 'react-bootstrap';
import { useNavigate, useParams } from 'react-router-dom';
import { toast } from 'react-toastify';
import PageHeader from '../../components/common/PageHeader';
import FormBuilder from '../../components/common/FormBuilder';
import { ProblemType } from '../../utils/constants';
import deliveryService from '../../services/deliveryService';
import { getPosition } from '../../utils/geo';
import { clean } from '../../utils/formatters';
import { v, yup } from '../../utils/validators';

const schema = yup.object({
  problemType: v.select('Problem type'),
  description: v.req('Description').max(500),
  photoUrl: yup.string().nullable(),
  latitude: v.num('Latitude').nullable(),
  longitude: v.num('Longitude').nullable(),
});

export default function ReportProblem() {
  const { id } = useParams();
  const navigate = useNavigate();
  return (
    <>
      <PageHeader title="Report a problem" subtitle="The fleet manager is alerted immediately" />
      <Row>
        <Col lg={8}>
          <Card>
            <Card.Body>
              <FormBuilder schema={schema} submitLabel="Report problem" onCancel={() => navigate(-1)}
                defaultValues={{ problemType: '', description: '', photoUrl: '', latitude: '', longitude: '' }}
                fields={[
                  { name: 'problemType', label: 'Problem type', type: 'select', options: ProblemType, required: true },
                  { name: 'description', label: 'Description', type: 'textarea', required: true, col: 12, maxLength: 500 },
                  { name: 'photoUrl', label: 'Photo', type: 'file', category: 'photo', col: 12 },
                  { name: 'latitude', label: 'Latitude', type: 'number', step: 'any' },
                  { name: 'longitude', label: 'Longitude', type: 'number', step: 'any' },
                ]}
                onSubmit={async (values) => {
                  await deliveryService.reportProblem(id, clean(values));
                  toast.success('Problem reported. Fleet manager alerted.');
                  navigate(`/deliveries/${id}`);
                }}>
                {(form) => (
                  <Button size="sm" variant="outline-secondary" className="mb-3" onClick={async () => {
                    try {
                      const p = await getPosition();
                      form.setValue('latitude', p.latitude);
                      form.setValue('longitude', p.longitude);
                    } catch (e) {
                      toast.error(e.message);
                    }
                  }}><i className="bi bi-crosshair me-1" />Use my GPS location</Button>
                )}
              </FormBuilder>
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </>
  );
}
