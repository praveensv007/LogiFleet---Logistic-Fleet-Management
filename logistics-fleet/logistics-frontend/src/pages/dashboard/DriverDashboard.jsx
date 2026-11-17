import { useEffect } from 'react';
import { Card, Col, Row } from 'react-bootstrap';
import PageHeader from '../../components/common/PageHeader';
import StatCard from '../../components/common/StatCard';
import Loader, { EmptyState } from '../../components/common/Loader';
import useFetch from '../../hooks/useFetch';
import useAuth from '../../hooks/useAuth';
import { useNotifications } from '../../context/NotificationContext';
import deliveryService from '../../services/deliveryService';
import { TripCard, useTripActions } from '../deliveries/MyDeliveries';

export default function DriverDashboard() {
  const { user } = useAuth();
  const { data, loading, reload } = useFetch(() => deliveryService.my(), [], { initial: [] });
  const { onNotification } = useNotifications();
  const { accept, startReject, dialog } = useTripActions(reload);

  // New trips arrive as notifications: refresh the list immediately.
  useEffect(() => onNotification(() => reload(true)), [onNotification, reload]);

  if (loading) return <Loader />;
  const newTrips = data.filter((a) => a.status === 'ASSIGNED');
  const current = data.filter((a) => ['ACCEPTED', 'IN_PROGRESS'].includes(a.status));
  const done = data.filter((a) => a.status === 'COMPLETED');
  const onTime = done.filter((a) => !a.delayed).length;

  return (
    <>
      <PageHeader title={`Hello, ${user.fullName.split(' ')[0]}`} subtitle="Your trips for today" />
      <Row className="g-3 mb-3">
        <Col md={3} xs={6}><StatCard icon="bell" color="sand" label="New trips" value={newTrips.length} to="/deliveries/my" /></Col>
        <Col md={3} xs={6}><StatCard icon="truck" label="In progress" value={current.length} to="/deliveries/my" /></Col>
        <Col md={3} xs={6}><StatCard icon="patch-check" color="green" label="Delivered (recent)" value={done.length} /></Col>
        <Col md={3} xs={6}><StatCard icon="stopwatch" color="blue" label="On time" value={done.length ? `${Math.round((onTime / done.length) * 100)}%` : '—'} /></Col>
      </Row>
      {[...newTrips, ...current].length === 0 ? (
        <Card><EmptyState icon="cup-hot" text="No trips right now. New assignments pop up here instantly." /></Card>
      ) : (
        <Row className="g-3">
          {[...newTrips, ...current].map((a) => (
            <Col md={6} xl={4} key={a.id}><TripCard trip={a} onAccept={accept} onReject={startReject} /></Col>
          ))}
        </Row>
      )}
      {dialog}
    </>
  );
}
