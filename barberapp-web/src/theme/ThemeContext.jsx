import { useEffect, useState } from 'react';
import { ThemeContext } from './useTema.js';
import { permitePreferencias } from '../utils/consentimento.js';

const CHAVE = 'barberapp-tema';

function lerTemaSalvo() {
    try {
        // sessionStorage vale só enquanto a aba estiver aberta; localStorage só com permissão do cliente
        return sessionStorage.getItem(CHAVE) || localStorage.getItem(CHAVE);
    } catch {
        return null;
    }
}

export function ThemeProvider({ children }) {
    const [tema, setTema] = useState(() => {
        const salvo = lerTemaSalvo();
        // 'light' era gravado pelo ThemeToggle antigo; o CSS só entende claro/escuro
        return salvo === 'claro' || salvo === 'light' ? 'claro' : 'escuro';
    });

    useEffect(() => {
        document.documentElement.setAttribute('data-tema', tema);
        try {
            sessionStorage.setItem(CHAVE, tema);
            // só lembra o tema de forma permanente se o cliente permitiu "preferências" no aviso de cookies
            if (permitePreferencias()) localStorage.setItem(CHAVE, tema);
        } catch {
            // sem armazenamento disponível: o tema só vale nesta visita
        }
    }, [tema]);

    function alternarTema() {
        setTema(atual => atual === 'escuro' ? 'claro' : 'escuro');
    }

    return (
        <ThemeContext.Provider value={{ tema, alternarTema }}>
            {children}
        </ThemeContext.Provider>
    );
}