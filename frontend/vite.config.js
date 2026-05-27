import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
export default defineConfig({
    base: '/',
    plugins: [react()],
    server: {
        proxy: {
            '/MovieNight/': {
                target: 'http://localhost:8181',
                changeOrigin: true,
            },
        },
    },
    build: {
        outDir: '../backend/src/main/resources/static/frontend',
        emptyOutDir: true,
    },
});
