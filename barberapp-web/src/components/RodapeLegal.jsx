import { Link, useLocation } from 'react-router-dom';
import { EMPRESA, DATA_ATUALIZACAO } from '../legal/dadosLegais.js';
import './RodapeLegal.css';

export function LinksLegais() {
    return (
        <nav className="rodape-legal__links" aria-label="Documentos legais">
            <Link to="/termos">Termos de Uso</Link>
            <Link to="/regras-de-agendamento">Regras de agendamento</Link>
            <Link to="/reembolso">Cancelamento e reembolso</Link>
            <Link to="/comunicacao">Chat e chamados</Link>
            <Link to="/privacidade">Privacidade</Link>
            <Link to="/cookies">Cookies</Link>
        </nav>
    );
}

// rodapé das páginas públicas e do cliente; os painéis (admin/barbeiro) e o login não usam
const SEM_RODAPE = ['/admin', '/barbeiro', '/chamados', '/login', '/cadastro', '/verificar-conta', '/verificarConta', '/esqueci-senha'];

export default function RodapeLegal() {
    const { pathname } = useLocation();
    if (SEM_RODAPE.includes(pathname)) return null;
    return (
        <footer className="rodape-legal">
            <LinksLegais />
            <small>{EMPRESA.nomeFantasia} · Documentos atualizados em {DATA_ATUALIZACAO}</small>
        </footer>
    );
}
