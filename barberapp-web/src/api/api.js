import axios from 'axios';
import { sair } from '../auth/session.js';

const api = axios.create({
  baseURL: 'http://localhost:8080/',
});

// Envia o token em toda requisição, menos nas rotas de /auth
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  const rotaDeAuth = config.url?.startsWith('/auth/');

  if (token && !rotaDeAuth) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Token expirado: limpa a sessão e volta para o login
api.interceptors.response.use(
    (resposta) => resposta,
    (error) => {
      const rotaDeAuth = error.config?.url?.startsWith('/auth/');
      if (error.response?.status === 401 && !rotaDeAuth) {
        sair();
        window.location.href = '/login';
      }
      return Promise.reject(error);
    }
);

export default api;