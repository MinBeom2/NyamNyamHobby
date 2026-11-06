import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import '@fontsource/noto-sans-kr/korean-400.css';
import '@fontsource/noto-sans-kr/korean-500.css';
import '@fontsource/noto-sans-kr/korean-600.css';
import '@fontsource/noto-sans-kr/korean-700.css';
import { App } from './app/App';
import './shared/styles/global.css';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: 1, refetchOnWindowFocus: false },
  },
});

const router = createBrowserRouter([{ path: '*', element: <App /> }]);

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  </StrictMode>,
);
