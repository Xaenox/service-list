import { ReactNode } from 'react';
import Header from './Header';
import Sidebar from './Sidebar';
import { User } from '../types';

interface LayoutProps {
  children: ReactNode;
  user?: User | null;
  onLogin: () => void;
  onLogout: () => void;
}

export default function Layout({ children, user, onLogin, onLogout }: LayoutProps) {
  return (
    <>
      <Header user={user} onLogin={onLogin} onLogout={onLogout} />
      <div className="app-container">
        <Sidebar />
        <main className="main-content">
          {children}
        </main>
      </div>
    </>
  );
}
