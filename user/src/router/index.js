import { createRouter, createWebHistory } from 'vue-router'
import PortalView from '../views/PortalView.vue'
import { useUserStore } from '../stores/user'
import { getToken } from '../utils/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'portal',
      component: PortalView,
    },
    {
      path: '/home',
      name: 'home',
      component: () => import('../views/HomeView.vue'),
      meta: { homeView: 'discover' },
    },
    {
      path: '/notifications',
      name: 'notifications',
      component: () => import('../views/HomeView.vue'),
      meta: { homeView: 'notification' },
    },
    {
      path: '/me',
      name: 'me',
      component: () => import('../views/HomeView.vue'),
      meta: { homeView: 'me' },
    },
    {
      path: '/search',
      name: 'search',
      component: () => import('../views/HomeView.vue'),
      meta: { homeView: 'search' },
    },
    {
      path: '/publish',
      name: 'publish',
      component: () => import('../views/PublishView.vue'),
    },
    {
      path: '/ai-chat',
      name: 'ai-chat',
      component: () => import('../views/AiChatView.vue'),
    },
    {
      path: '/note/:id',
      name: 'note-detail',
      component: () => import('../views/HomeView.vue'),
      meta: { homeView: 'note' },
    },
    {
      path: '/user/:actorId',
      name: 'user-profile',
      component: () => import('../views/UserProfileView.vue'),
    },
    {
      path: '/rag-references/:refKey',
      name: 'rag-references',
      component: () => import('../views/RagReferencesView.vue'),
    },
  ],
})

router.beforeEach((to) => {
  if (to.name === 'publish' && (!getToken() || !useUserStore().isLoggedIn)) {
    return { name: 'home', query: { login: '1' } }
  }
})

export default router
