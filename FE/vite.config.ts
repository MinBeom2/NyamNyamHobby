import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '.', '');
  const apiProxyTarget = env.VITE_API_PROXY_TARGET || 'http://localhost:8080';

  return {
    plugins: [react()],
    server: {
      proxy: {
        '/api': {
          target: apiProxyTarget,
          changeOrigin: true,
        },
        '/oauth2': {
          target: apiProxyTarget,
          changeOrigin: true,
        },
        '/login/oauth2': {
          target: apiProxyTarget,
          changeOrigin: true,
        },
      },
    },
  };
});
