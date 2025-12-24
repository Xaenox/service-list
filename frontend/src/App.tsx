import { useEffect, useState } from 'react';
import { BrowserRouter, Routes, Route, useSearchParams } from 'react-router-dom';
import Layout from './components/Layout';
import HomePage from './pages/HomePage';
import ServicesPage from './pages/ServicesPage';
import ServiceDetailPage from './pages/ServiceDetailPage';
import { authApi } from './services/api';
import { User } from './types';

function AuthCallback() {
  const [searchParams] = useSearchParams();

  useEffect(() => {
    const token = searchParams.get('token');
    if (token) {
      localStorage.setItem('jwt_token', token);
      window.location.href = '/';
    }
  }, [searchParams]);

  return <div className="loading">Авторизация...</div>;
}

function App() {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchUser = async () => {
      const token = localStorage.getItem('jwt_token');
      if (token) {
        try {
          const userData = await authApi.getCurrentUser();
          setUser(userData);
        } catch (error) {
          console.error('Error fetching user:', error);
          localStorage.removeItem('jwt_token');
        }
      }
      setLoading(false);
    };

    fetchUser();
  }, []);

  const handleLogin = () => {
    authApi.login();
  };

  const handleLogout = () => {
    authApi.logout();
    setUser(null);
  };

  if (loading) {
    return <div className="loading">Загрузка...</div>;
  }

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/auth/callback" element={<AuthCallback />} />
        <Route
          path="/"
          element={
            <Layout user={user} onLogin={handleLogin} onLogout={handleLogout}>
              <HomePage />
            </Layout>
          }
        />
        <Route
          path="/services"
          element={
            <Layout user={user} onLogin={handleLogin} onLogout={handleLogout}>
              <ServicesPage />
            </Layout>
          }
        />
        <Route
          path="/services/:type"
          element={
            <Layout user={user} onLogin={handleLogin} onLogout={handleLogout}>
              <ServicesPage />
            </Layout>
          }
        />
        <Route
          path="/services/:id"
          element={
            <Layout user={user} onLogin={handleLogin} onLogout={handleLogout}>
              <ServiceDetailPage />
            </Layout>
          }
        />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
