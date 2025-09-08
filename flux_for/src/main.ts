import { createApp } from 'vue';
import { createPinia } from 'pinia';
import router from './router';
import App from './App.vue';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import './style.css';

console.log('[main.ts] Starting application setup...');

const app = createApp(App);
console.log('[main.ts] Vue app created.');

app.use(createPinia());
console.log('[main.ts] Pinia plugin installed.');

app.use(router);
console.log('[main.ts] Vue Router plugin installed.');

app.use(ElementPlus);
console.log('[main.ts] ElementPlus plugin installed.');

app.mount('#app');
console.log('[main.ts] Vue app mounted to #app.');
