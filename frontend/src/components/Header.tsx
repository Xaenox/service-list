import { Link } from 'react-router-dom';
import { User } from '../types';

interface HeaderProps {
  user?: User | null;
  onLogin: () => void;
  onLogout: () => void;
}

export default function Header({ user, onLogin, onLogout }: HeaderProps) {
  return (
    <header className="header">
      <Link to="/" className="header-logo">
        <span className="header-logo-icon">✕</span>
        <span>Community Services</span>
      </Link>

      <div className="header-actions">
        <div className="search-box">
          <span className="search-icon">🔍</span>
          <input type="text" placeholder="Поиск..." />
        </div>

        {user ? (
          <>
            <Link to="/services/new" className="btn btn-primary">
              ✏️ Создать
            </Link>
            <div className="user-badge" onClick={onLogout}>
              {user.avatar && (
                <img src={user.avatar} alt={user.fullName} className="user-avatar" />
              )}
              <span className="user-name">{user.fullName}</span>
            </div>
          </>
        ) : (
          <button onClick={onLogin} className="btn btn-accent">
            Войти
          </button>
        )}
      </div>
    </header>
  );
}
