import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { servicesApi } from '../services/api';
import { Service } from '../types';

export default function ServiceDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [service, setService] = useState<Service | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchService = async () => {
      if (!id) return;

      try {
        setLoading(true);
        const data = await servicesApi.get(parseInt(id));
        setService(data);
      } catch (err) {
        setError('Не удалось загрузить услугу');
        console.error('Error fetching service:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchService();
  }, [id]);

  if (loading) {
    return <div className="loading">Загрузка...</div>;
  }

  if (error || !service) {
    return (
      <div className="error">
        {error || 'Услуга не найдена'}
        <button onClick={() => navigate('/')} className="btn btn-primary" style={{ marginTop: '1rem' }}>
          Вернуться на главную
        </button>
      </div>
    );
  }

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('ru-RU', {
      day: 'numeric',
      month: 'long',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const getTypeLabel = (type: string) => {
    const labels: Record<string, string> = {
      ONLINE: 'Онлайн',
      OFFLINE: 'Офлайн',
      HYBRID: 'Гибрид'
    };
    return labels[type] || type;
  };

  return (
    <article>
      <div style={{ marginBottom: '2rem' }}>
        <button onClick={() => navigate(-1)} className="btn btn-outline">
          ← Назад
        </button>
      </div>

      <div className="service-card" style={{ cursor: 'default' }}>
        <div className="service-card-header">
          <div style={{ display: 'flex', alignItems: 'flex-start', gap: '1rem', flex: 1 }}>
            {service.owner.avatar && (
              <img
                src={service.owner.avatar}
                alt={service.owner.fullName}
                className="service-avatar"
                style={{ width: '64px', height: '64px' }}
              />
            )}
            <div className="service-meta">
              <div>
                <a href={`/users/${service.owner.slug}`} className="service-author">
                  {service.owner.fullName}
                </a>
              </div>
              {service.owner.bio && (
                <div style={{ fontSize: '0.875rem', color: 'var(--color-text-secondary)' }}>
                  {service.owner.bio}
                </div>
              )}
              <div className="service-date">
                Опубликовано: {formatDate(service.createdAt)}
              </div>
            </div>
          </div>
          <div className="service-votes">
            <button className="vote-button">▲</button>
            <div className="vote-count">0</div>
          </div>
        </div>

        <h1 className="service-title" style={{ fontSize: '2rem', marginTop: '1rem' }}>
          {service.title}
        </h1>

        <div className="service-tags" style={{ marginBottom: '1.5rem' }}>
          <span className="tag type">{getTypeLabel(service.type)}</span>
          {service.categories.map((category) => (
            <span key={category.id} className="tag">
              {category.name}
            </span>
          ))}
          {service.city && (
            <span className="tag location">
              📍 {service.city}
              {service.country && `, ${service.country}`}
            </span>
          )}
        </div>

        <div style={{ fontSize: '1.1rem', lineHeight: '1.8', marginBottom: '2rem', whiteSpace: 'pre-wrap' }}>
          {service.description}
        </div>

        {service.price && (
          <div style={{
            padding: '1.5rem',
            backgroundColor: 'var(--color-hover)',
            borderRadius: '8px',
            marginBottom: '1.5rem'
          }}>
            <div style={{ fontSize: '0.875rem', color: 'var(--color-text-secondary)', marginBottom: '0.25rem' }}>
              Стоимость
            </div>
            <div className="service-price" style={{ fontSize: '1.5rem' }}>
              {service.price} {service.currency || ''}
            </div>
          </div>
        )}

        {service.contactInfo && (
          <div style={{
            padding: '1.5rem',
            backgroundColor: 'var(--color-hover)',
            borderRadius: '8px',
            marginBottom: '1.5rem'
          }}>
            <div style={{ fontSize: '0.875rem', color: 'var(--color-text-secondary)', marginBottom: '0.5rem' }}>
              Контактная информация
            </div>
            <div style={{ whiteSpace: 'pre-wrap' }}>
              {service.contactInfo}
            </div>
          </div>
        )}

        <div style={{
          borderTop: '1px solid var(--color-border)',
          paddingTop: '1rem',
          marginTop: '2rem',
          fontSize: '0.875rem',
          color: 'var(--color-text-secondary)'
        }}>
          Последнее обновление: {formatDate(service.updatedAt)}
        </div>
      </div>
    </article>
  );
}
