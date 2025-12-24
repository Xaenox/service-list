import { Link } from 'react-router-dom';
import { Service } from '../types';

interface ServiceCardProps {
  service: Service;
}

export default function ServiceCard({ service }: ServiceCardProps) {
  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('ru-RU', {
      day: 'numeric',
      month: 'long',
      year: 'numeric'
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
    <Link to={`/services/${service.id}`} style={{ textDecoration: 'none', color: 'inherit' }}>
      <article className="service-card">
        <div className="service-card-header">
          <div style={{ display: 'flex', alignItems: 'flex-start', gap: '1rem', flex: 1 }}>
            {service.owner.avatar && (
              <img
                src={service.owner.avatar}
                alt={service.owner.fullName}
                className="service-avatar"
              />
            )}
            <div className="service-meta">
              <div>
                <a
                  href={`/users/${service.owner.slug}`}
                  className="service-author"
                  onClick={(e) => e.stopPropagation()}
                >
                  {service.owner.fullName}
                </a>
              </div>
              <div className="service-date">
                {formatDate(service.createdAt)}
              </div>
            </div>
          </div>
          <div className="service-votes">
            <button className="vote-button" onClick={(e) => e.preventDefault()}>
              ▲
            </button>
            <div className="vote-count">0</div>
          </div>
        </div>

        <h2 className="service-title">{service.title}</h2>

        <p className="service-description">
          {service.description.length > 200
            ? `${service.description.substring(0, 200)}...`
            : service.description}
        </p>

        <div className="service-tags">
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

        <div className="service-footer">
          {service.price ? (
            <div className="service-price">
              {service.price} {service.currency || ''}
            </div>
          ) : (
            <div className="service-price">Цена не указана</div>
          )}
        </div>
      </article>
    </Link>
  );
}
