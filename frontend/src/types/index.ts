export enum ServiceType {
  ONLINE = 'ONLINE',
  OFFLINE = 'OFFLINE',
  HYBRID = 'HYBRID'
}

export enum ServiceStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  DELETED = 'DELETED'
}

export enum UserRole {
  USER = 'USER',
  MODERATOR = 'MODERATOR',
  ADMIN = 'ADMIN'
}

export interface User {
  id: number;
  slug: string;
  fullName: string;
  email?: string;
  avatar?: string;
  bio?: string;
  country?: string;
  city?: string;
  role: UserRole;
  createdAt: string;
  updatedAt: string;
}

export interface Category {
  id: number;
  name: string;
  slug: string;
  description?: string;
  icon?: string;
  parentId?: number;
  createdAt: string;
}

export interface Service {
  id: number;
  title: string;
  description: string;
  type: ServiceType;
  status: ServiceStatus;
  price?: string;
  currency?: string;
  country?: string;
  city?: string;
  contactInfo?: string;
  categories: Category[];
  owner: User;
  createdAt: string;
  updatedAt: string;
}

export interface ServiceListResponse {
  items: Service[];
  total: number;
  page: number;
  limit: number;
  totalPages: number;
}

export interface ServiceFilters {
  page?: number;
  limit?: number;
  type?: ServiceType;
  status?: ServiceStatus;
  category?: string;
  country?: string;
  city?: string;
  search?: string;
}
