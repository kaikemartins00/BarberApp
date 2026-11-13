import { VERSAO_TERMOS } from '../legal/dadosLegais.js';

const CHAVE = 'barberapp-consentimento';

// Lê a escolha do cliente. Retorna null se ele ainda não escolheu (ou se os textos mudaram de versão).
export function lerConsentimento() {
    try {
        const bruto = localStorage.getItem(CHAVE);
        if (!bruto) return null;
        const dados = JSON.parse(bruto);
        return dados?.versao === VERSAO_TERMOS ? dados : null;
    } catch {
        return null;
    }
}

export function gravarConsentimento(preferencias) {
    const dados = { versao: VERSAO_TERMOS, preferencias: !!preferencias, data: new Date().toISOString() };
    try {
        localStorage.setItem(CHAVE, JSON.stringify(dados));
        if (preferencias) {
            const tema = document.documentElement.getAttribute('data-tema');
            if (tema) localStorage.setItem('barberapp-tema', tema);
        } else {
            localStorage.removeItem('barberapp-tema');
        }
    } catch {
        // sem armazenamento disponível: o site continua funcionando
    }
    window.dispatchEvent(new Event('barberapp:consentimento'));
    return dados;
}

// O tema (claro/escuro) só é lembrado se o cliente permitiu "preferências".
// Sem escolha ainda, não grava nada: o banner pergunta antes.
export function permitePreferencias() {
    return lerConsentimento()?.preferencias === true;
}
