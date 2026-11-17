import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  // sockjs-client expects Node's "global"
  define: { global: 'window' },
  server: { port: 5173 },
});
