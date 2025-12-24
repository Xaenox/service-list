import axios from 'axios';
import type { User, Service, ServiceListResponse, ServiceFilters, Category } from '../types';

const api = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add JWT token to requests if available
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('jwt_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export const authApi = {
  login: (returnUrl: string = window.location.origin) => {
    window.location.href = `/api/v1/auth/login?return_url=${encodeURIComponent(returnUrl)}`;
  },

  getCurrentUser: async (): Promise<User> => {
    const response = await api.get<User>('/auth/me');
    return response.data;
  },

  logout: () => {
    localStorage.removeItem('jwt_token');
    window.location.href = '/';
  }
};

export const servicesApi = {
  list: async (filters?: ServiceFilters): Promise<ServiceListResponse> => {
    const response = await api.get<ServiceListResponse>('/services', { params: filters });
    return response.data;
  },

  get: async (id: number): Promise<Service> => {
    const response = await api.get<Service>(`/services/${id}`);
    return response.data;
  },

  create: async (service: Partial<Service>): Promise<Service> => {
    const response = await api.post<Service>('/services', service);
    return response.data;
  },

  update: async (id: number, service: Partial<Service>): Promise<Service> => {
    const response = await api.put<Service>(`/services/${id}`, service);
    return response.data;
  },

  delete: async (id: number): Promise<void> => {
    await api.delete(`/services/${id}`);
  }
};

export const categoriesApi = {
  list: async (): Promise<Category[]> => {
    const response = await api.get<Category[]>('/categories');
    return response.data;
  },

  get: async (id: number): Promise<Category> => {
    const response = await api.get<Category>(`/categories/${id}`);
    return response.data;
  }
};

export default api;
