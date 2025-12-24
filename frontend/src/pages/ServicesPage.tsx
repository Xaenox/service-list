import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { servicesApi } from '../services/api';
import { Service, ServiceType } from '../types';
import ServiceList from '../components/ServiceList';

export default function ServicesPage() {
  const { type } = useParams<{ type?: string }>();
  const [services, setServices] = useState<Service[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchServices = async () => {
      try {
        setLoading(true);
        const filters: any = { limit: 20, page: 1 };

        if (type && ['online', 'offline', 'hybrid'].includes(type)) {
          filters.type = type.toUpperCase() as ServiceType;
        }

        const response = await servicesApi.list(filters);
        setServices(response.items);
      } catch (err) {
        setError('Не удалось загрузить услуги');
        console.error('Error fetching services:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchServices();
  }, [type]);

  const getTitle = () => {
    if (!type) return 'Все услуги';
    const titles: Record<string, string> = {
      online: 'Онлайн услуги',
      offline: 'Офлайн услуги',
      hybrid: 'Гибридные услуги'
    };
    return titles[type] || 'Услуги';
  };

  return (
    <div>
      <div className="content-header">
        <h1>{getTitle()}</h1>
      </div>

      {error && <div className="error">{error}</div>}

      <ServiceList services={services} loading={loading} />
    </div>
  );
}
