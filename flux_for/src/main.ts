import { createApp } from 'vue';
import { createPinia } from 'pinia';
import router from './router';
import App from './App.vue';
import './style.css';

console.log('[main.ts] Starting application setup...');

const app = createApp(App);
console.log('[main.ts] Vue app created.');

app.use(createPinia());
console.log('[main.ts] Pinia plugin installed.');

app.use(router);
console.log('[main.ts] Vue Router plugin installed.');

app.mount('#app');
console.log('[main.ts] Vue app mounted to #app.');
