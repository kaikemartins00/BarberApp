const TOKEN_KEY = "token";
const SESSAO_KEY = "sessao_usuario";

// 1. SALVAR SESSÃO: Salva o token limpo e as informações paralelas que o Java enviou
export function salvarSessao(data, email) {
    const sessao = {
        email: email,
        roles: data.roles || [], // Captura o array de strings (Ex: ["ROLE_ADMIN"]) vindo do Java
        userId: data.userId || null // Captura o id numérico vindo do Java
    };

    localStorage.setItem(TOKEN_KEY, data.token);
    localStorage.setItem(SESSAO_KEY, JSON.stringify(sessao));
    return sessao;
}

// 2. LER SESSÃO: Lê o objeto de sessão limpo direto do LocalStorage
export function lerSessao() {
    const sessaoGravada = localStorage.getItem(SESSAO_KEY);

    if (!sessaoGravada) return null;

    try {
        return JSON.parse(sessaoGravada);
    } catch (erro) {
        return null;
    }
}

// 3. FAZER LOGOUT
export function sair() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(SESSAO_KEY);
}

// 4. REDIRECIONAMENTO APÓS LOGIN (Mapeado com o prefixo oficial do seu Enum)
export function telaInicial(sessao) {
    if (!sessao || !sessao.roles) return '/home';

    if (sessao.roles.includes('ROLE_ADMIN')) return '/admin';
    if (sessao.roles.includes('ROLE_BARBEIRO')) return '/barbeiro';

    return '/home';
}