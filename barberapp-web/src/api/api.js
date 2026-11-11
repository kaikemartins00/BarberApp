import axios from 'axios';
import { sair } from '../auth/session.js';

const api = axios.create({
    baseURL: import.meta.env.VITE_API_URL || 'https://d1yeyagfzydmbx.cloudfront.net',
    timeout: 15000
});

const ROTAS_PUBLICAS = [
    '/auth/login',
    '/auth/cadastro',
    '/auth/validar-codigo',
    '/auth/reenviar-codigo',
    '/auth/esqueci-senha',
    '/auth/redefinir-senha',
];
const ehPublica = (url) => ROTAS_PUBLICAS.includes(url);

api.interceptors.request.use((config) => {
    const token = localStorage.getItem('token');
    if (token && !ehPublica(config.url)) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

api.interceptors.response.use(
    (resposta) => resposta,
    (error) => {
        if (error.response?.status === 401 && !ehPublica(error.config?.url)) {
            sair();
            window.location.href = '/login';
        }
        return Promise.reject(error);
    }
);

export default api;