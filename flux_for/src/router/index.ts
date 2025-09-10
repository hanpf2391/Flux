import { createRouter, createWebHistory } from 'vue-router';
import FluxView from '../views/FluxView.vue';

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'flux',
      component: FluxView,
    },
  ],
});

export default router;
