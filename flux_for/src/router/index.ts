import { createRouter, createWebHistory } from 'vue-router';
import FluxView from '../views/FluxView.vue';
import InitialPositionTest from '../views/InitialPositionTest.vue';

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'flux',
      component: FluxView,
    },
    {
      path: '/test',
      name: 'test',
      component: InitialPositionTest,
    },
  ],
});

export default router;
