import { Service } from '../types';
import ServiceCard from './ServiceCard';

interface ServiceListProps {
  services: Service[];
  loading?: boolean;
}

export default function ServiceList({ services, loading }: ServiceListProps) {
  if (loading) {
    return <div className="loading">Загрузка...</div>;
  }

  if (services.length === 0) {
    return (
      <div className="empty-state">
        <div className="empty-state-icon">📭</div>
        <h3 className="empty-state-title">Услуг пока нет</h3>
        <p>Будьте первым, кто добавит свою услугу!</p>
      </div>
    );
  }

  return (
    <div>
      {services.map((service) => (
        <ServiceCard key={service.id} service={service} />
      ))}
    </div>
  );
}
