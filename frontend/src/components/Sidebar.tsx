import { Link, useLocation } from 'react-router-dom';

const navigationItems = [
  { path: '/', label: 'Главная', icon: '🏠' },
  { path: '/services', label: 'Все услуги', icon: '📋' },
  { path: '/services/online', label: 'Онлайн', icon: '💻' },
  { path: '/services/offline', label: 'Офлайн', icon: '🏢' },
  { path: '/services/hybrid', label: 'Гибридные', icon: '🌐' },
];

const categoryItems = [
  { path: '/categories', label: 'Все категории', icon: '🗂️' },
  { path: '/my-services', label: 'Мои услуги', icon: '👤' },
];

export default function Sidebar() {
  const location = useLocation();

  const isActive = (path: string) => {
    if (path === '/') {
      return location.pathname === path;
    }
    return location.pathname.startsWith(path);
  };

  return (
    <aside className="sidebar">
      <div className="sidebar-section">
        <h3 className="sidebar-section-title">Навигация</h3>
        <ul className="nav-list">
          {navigationItems.map((item) => (
            <li key={item.path} className="nav-item">
              <Link
                to={item.path}
                className={`nav-link ${isActive(item.path) ? 'active' : ''}`}
              >
                <span className="nav-icon">{item.icon}</span>
                <span>{item.label}</span>
              </Link>
            </li>
          ))}
        </ul>
      </div>

      <div className="sidebar-section">
        <h3 className="sidebar-section-title">Каталог</h3>
        <ul className="nav-list">
          {categoryItems.map((item) => (
            <li key={item.path} className="nav-item">
              <Link
                to={item.path}
                className={`nav-link ${isActive(item.path) ? 'active' : ''}`}
              >
                <span className="nav-icon">{item.icon}</span>
                <span>{item.label}</span>
              </Link>
            </li>
          ))}
        </ul>
      </div>
    </aside>
  );
}
