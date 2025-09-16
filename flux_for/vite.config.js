import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue';
import { resolve } from 'path';
// https://vite.dev/config/
export default defineConfig(({ mode }) => {
    // Load env file based on `mode` in the current working directory.
    const env = loadEnv(mode, process.cwd(), '');
    const isProduction = mode === 'production';
    return {
        plugins: [vue()],
        // Build configuration
        build: {
            // Production-specific build settings
            minify: isProduction ? 'terser' : false,
            sourcemap: !isProduction,
            // Report compressed file size
            reportCompressedSize: isProduction,
            // Chunk size warning limit
            chunkSizeWarningLimit: 1000,
            rollupOptions: {
                output: {
                    // Manual chunk division
                    manualChunks: {
                        // Split vendor libraries
                        vendor: ['vue', 'vue-router', 'pinia'],
                        // Split UI library
                        'element-plus': ['element-plus'],
                        // Split utility libraries
                        utils: ['lodash-es', 'axios']
                    },
                    // Asset file names
                    assetFileNames: (assetInfo) => {
                        if (!assetInfo.name)
                            return 'assets/[name]-[hash][extname]';
                        const info = assetInfo.name.split('.');
                        const extType = info[info.length - 1];
                        if (/\.(mp4|webm|ogg|mp3|wav|flac|aac)(\?.*)?$/i.test(assetInfo.name)) {
                            return `media/[name]-[hash][extname]`;
                        }
                        if (/\.(png|jpe?g|gif|svg)(\?.*)?$/i.test(assetInfo.name)) {
                            return `img/[name]-[hash][extname]`;
                        }
                        if (/\.(woff2?|eot|ttf|otf)(\?.*)?$/i.test(assetInfo.name)) {
                            return `fonts/[name]-[hash][extname]`;
                        }
                        if (extType === 'css') {
                            return `css/[name]-[hash][extname]`;
                        }
                        return `assets/[name]-[hash][extname]`;
                    },
                    // Chunk file names
                    chunkFileNames: 'js/[name]-[hash].js',
                    // Entry file names
                    entryFileNames: 'js/[name]-[hash].js'
                }
            },
            // Terser configuration for production
            terserOptions: isProduction ? {
                compress: {
                    drop_console: true,
                    drop_debugger: true
                }
            } : undefined
        },
        // Server configuration (development)
        server: {
            host: true,
            port: 3000,
            proxy: {
                // Proxy API requests to the backend server
                '/api': {
                    target: env.VITE_API_BASE_URL || 'http://localhost:8080',
                    changeOrigin: true,
                    secure: true,
                    rewrite: (path) => path
                },
                // Proxy WebSocket connections
                '/ws': {
                    target: env.VITE_WS_URL || 'ws://localhost:8080',
                    ws: true,
                    changeOrigin: true
                },
            },
        },
        // CSS configuration
        css: {
            // CSS modules configuration
            modules: {
                localsConvention: 'camelCase'
            },
            // CSS preprocessor options
            preprocessorOptions: {
                scss: {
                    additionalData: `@import "@/styles/variables.scss";`
                }
            }
        },
        // Resolve configuration
        resolve: {
            alias: {
                '@': resolve(__dirname, 'src'),
                '@components': resolve(__dirname, 'src/components'),
                '@views': resolve(__dirname, 'src/views'),
                '@store': resolve(__dirname, 'src/store'),
                '@api': resolve(__dirname, 'src/api'),
                '@composables': resolve(__dirname, 'src/composables'),
                '@assets': resolve(__dirname, 'src/assets')
            },
            extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json', '.vue']
        },
        // Define global constants
        define: {
            __VUE_OPTIONS_API__: true,
            __VUE_PROD_DEVTOOLS__: false,
            __APP_ENV__: JSON.stringify(env.VITE_APP_ENV || 'development')
        },
        // Optimize dependencies
        optimizeDeps: {
            include: ['vue', 'vue-router', 'pinia', 'axios', 'lodash-es'],
            exclude: []
        },
        // Security headers for dev server
        preview: {
            headers: {
                'Content-Security-Policy': "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; connect-src 'self' wss:; font-src 'self';"
            }
        }
    };
});
