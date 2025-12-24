# Community Services Marketplace - Frontend

A modern React-based frontend for the Community Services Marketplace, designed with inspiration from the vas3k.club community interface.

## Features

- **Modern UI**: Clean, responsive interface inspired by vas3k.club design
- **Service Browsing**: Browse services by type (Online, Offline, Hybrid)
- **Search & Filters**: Find services by category, location, and keywords
- **User Authentication**: OAuth2 integration with vas3k.club
- **Service Management**: Create, edit, and manage your own services
- **Responsive Design**: Works seamlessly on desktop and mobile devices

## Tech Stack

- **React 18** - UI library
- **TypeScript** - Type safety
- **Vite** - Build tool and dev server
- **React Router** - Client-side routing
- **Axios** - HTTP client
- **CSS3** - Custom styling (no framework dependencies)

## Getting Started

### Prerequisites

- Node.js 18+
- npm or yarn
- Backend API running on `http://localhost:8080`

### Installation

1. **Install dependencies**:
   ```bash
   cd frontend
   npm install
   ```

2. **Start the development server**:
   ```bash
   npm run dev
   ```

   The app will be available at `http://localhost:3000`

3. **Build for production**:
   ```bash
   npm run build
   ```

   The built files will be in the `dist` directory.

4. **Preview production build**:
   ```bash
   npm run preview
   ```

## Project Structure

```
frontend/
├── src/
│   ├── assets/
│   │   └── styles/
│   │       └── main.css          # Global styles
│   ├── components/
│   │   ├── Header.tsx             # Top navigation bar
│   │   ├── Sidebar.tsx            # Left sidebar navigation
│   │   ├── Layout.tsx             # Main layout wrapper
│   │   ├── ServiceCard.tsx        # Service preview card
│   │   └── ServiceList.tsx        # List of services
│   ├── pages/
│   │   ├── HomePage.tsx           # Main landing page
│   │   ├── ServicesPage.tsx       # Services listing page
│   │   └── ServiceDetailPage.tsx  # Single service view
│   ├── services/
│   │   └── api.ts                 # API client and endpoints
│   ├── types/
│   │   └── index.ts               # TypeScript type definitions
│   ├── App.tsx                    # Root component with routing
│   ├── main.tsx                   # Application entry point
│   └── vite-env.d.ts              # Vite type definitions
├── index.html                     # HTML template
├── package.json                   # Dependencies and scripts
├── tsconfig.json                  # TypeScript configuration
├── vite.config.ts                 # Vite configuration
└── README.md                      # This file
```

## Available Scripts

- `npm run dev` - Start development server
- `npm run build` - Build for production
- `npm run preview` - Preview production build
- `npm run lint` - Run ESLint

## Features & Pages

### Home Page (`/`)
- Featured services
- Latest services
- Popular services tabs

### Services Listing (`/services`)
- Browse all services
- Filter by type: `/services/online`, `/services/offline`, `/services/hybrid`

### Service Detail (`/services/:id`)
- Full service description
- Provider information
- Contact details
- Pricing information

### Authentication
- OAuth2 login via vas3k.club
- Protected routes for authenticated users
- User profile display

## API Integration

The frontend communicates with the backend API at `/api/v1`:

- **Authentication**: `/api/v1/auth/*`
- **Services**: `/api/v1/services/*`
- **Categories**: `/api/v1/categories/*`

The Vite dev server proxies API requests to `http://localhost:8080`.

## Styling

The application uses custom CSS with a design system inspired by vas3k.club:

- **Color Palette**: Primary, secondary, accent colors
- **Typography**: System fonts for performance
- **Components**: Card-based layout with consistent spacing
- **Responsive**: Mobile-first approach

## Configuration

### Backend API URL

For production, update the API base URL in `src/services/api.ts`:

```typescript
const api = axios.create({
  baseURL: 'https://your-api-domain.com/api/v1',
  // ...
});
```

Or configure the Vite proxy in `vite.config.ts` for development.

## Authentication Flow

1. User clicks "Войти" (Login) button
2. Redirected to `/api/v1/auth/login`
3. Backend redirects to vas3k.club OAuth
4. After authentication, redirected back to `/auth/callback?token=JWT`
5. JWT token stored in localStorage
6. User data fetched and displayed

## Development

### Adding a New Page

1. Create a new component in `src/pages/`
2. Add route in `src/App.tsx`
3. Update navigation in `src/components/Sidebar.tsx` if needed

### Adding a New Component

1. Create component in `src/components/`
2. Export and import where needed
3. Follow existing patterns for props and styling

### API Integration

All API calls go through `src/services/api.ts`. To add a new endpoint:

```typescript
export const myApi = {
  myMethod: async (): Promise<MyType> => {
    const response = await api.get<MyType>('/my-endpoint');
    return response.data;
  }
};
```

## Deployment

### Static Hosting (Netlify, Vercel, etc.)

1. Build the project:
   ```bash
   npm run build
   ```

2. Deploy the `dist` directory

3. Configure rewrites for SPA routing:
   - Netlify: Add `_redirects` file
   - Vercel: Add `vercel.json`

Example `_redirects` for Netlify:
```
/*    /index.html   200
```

### Docker

Create a `Dockerfile`:
```dockerfile
FROM node:18-alpine as build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

## Browser Support

- Chrome/Edge (last 2 versions)
- Firefox (last 2 versions)
- Safari (last 2 versions)

## Contributing

1. Create a feature branch
2. Make your changes
3. Test thoroughly
4. Submit a pull request

## License

[Add your license here]

## Support

For issues and questions, create an issue in the repository.

---

**Built with React + TypeScript + Vite**
