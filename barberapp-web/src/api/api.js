import axios from 'axios';
import { sair } from '../auth/session.js';

const api = axios.create({
    baseURL: 'http://localhost:8080/',
    timeout: 15000,   // 15 segundos
});

// Só estas rotas dispensam token. "/auth/cadastro-barbeiro" NÃO está aqui:
// ela exige ADMIN, e antes o interceptor tirava o token dela.
const ROTAS_PUBLICAS = ['/auth/login', '/auth/cadastro', '/auth/validar-codigo', '/auth/reenviar-codigo'];
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