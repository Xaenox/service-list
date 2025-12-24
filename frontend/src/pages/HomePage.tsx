import { useEffect, useState } from 'react';
import { servicesApi } from '../services/api';
import { Service } from '../types';
import ServiceList from '../components/ServiceList';

export default function HomePage() {
  const [services, setServices] = useState<Service[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchServices = async () => {
      try {
        setLoading(true);
        const response = await servicesApi.list({ limit: 20, page: 1 });
        setServices(response.items);
      } catch (err) {
        setError('Не удалось загрузить услуги');
        console.error('Error fetching services:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchServices();
  }, []);

  return (
    <div>
      <div className="content-header">
        <h1>Маркетплейс услуг сообщества</h1>
        <p style={{ color: 'var(--color-text-secondary)' }}>
          Найдите услуги от членов нашего сообщества или предложите свои
        </p>
      </div>

      <div className="content-tabs">
        <button className="tab active">Все</button>
        <button className="tab">Новые</button>
        <button className="tab">Популярные</button>
      </div>

      {error && <div className="error">{error}</div>}

      <ServiceList services={services} loading={loading} />
    </div>
  );
}
