import { Moon, Sun } from 'lucide-react';
import { useTema } from '../../theme/useTema.js';
import './BotaoTema.css';

export default function BotaoTema({ className = '' }) {
    const { tema, alternarTema } = useTema();

    return (
        <button
            type="button"
            className={`botao-tema ${className}`.trim()}
            onClick={alternarTema}
            aria-label={tema === 'escuro' ? 'Ativar tema claro' : 'Ativar tema escuro'}
            title={tema === 'escuro' ? 'Ativar tema claro' : 'Ativar tema escuro'}
        >
            {tema === 'escuro' ? <Sun size={20} /> : <Moon size={20} />}
        </button>
    );
}