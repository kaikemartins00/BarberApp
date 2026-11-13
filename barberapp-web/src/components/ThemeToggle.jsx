import { useEffect, useState } from 'react';
import { Moon, Sun } from 'lucide-react';

const CHAVE = 'barberapp-tema';

function lerTema() {
    if (typeof window === 'undefined') return 'dark';
    const salvo = localStorage.getItem(CHAVE);
    return salvo === 'light' ? 'light' : 'dark';
}

export function aplicarTema(tema) {
    const valor = tema === 'light' ? 'light' : 'dark';
    document.documentElement.dataset.tema = valor;
    document.documentElement.style.colorScheme = valor;
    localStorage.setItem(CHAVE, valor);
    window.dispatchEvent(new CustomEvent('barberapp:tema', { detail: valor }));
}

export default function ThemeToggle() {
    const [tema, setTema] = useState(lerTema);

    useEffect(() => {
        aplicarTema(lerTema());
        const sincronizar = (evento) => setTema(evento.detail);
        window.addEventListener('barberapp:tema', sincronizar);
        return () => window.removeEventListener('barberapp:tema', sincronizar);
    }, []);

    function alternar() {
        const proximo = tema === 'dark' ? 'light' : 'dark';
        aplicarTema(proximo);
        setTema(proximo);
    }

    return (
        <button className="barber-theme-toggle" type="button" onClick={alternar} aria-label={tema === 'dark' ? 'Ativar tema claro' : 'Ativar tema escuro'}>
            {tema === 'dark' ? <Sun size={17} /> : <Moon size={17} />}
            <span>{tema === 'dark' ? 'Tema claro' : 'Tema escuro'}</span>
        </button>
    );
}
