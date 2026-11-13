import { Link } from 'react-router-dom';
import Navbar from '../../components/Navbar.jsx';
import { DATA_ATUALIZACAO, VERSAO_TERMOS } from '../../legal/dadosLegais.js';
import './Legal.css';

const ABAS = [
    { para: '/termos', rotulo: 'Termos de Uso' },
    { para: '/regras-de-agendamento', rotulo: 'Regras de agendamento' },
    { para: '/reembolso', rotulo: 'Cancelamento e Reembolso' },
    { para: '/comunicacao', rotulo: 'Chat e Chamados' },
    { para: '/privacidade', rotulo: 'Política de Privacidade' },
    { para: '/cookies', rotulo: 'Política de Cookies' },
];

// secoes: [{ id, titulo, conteudo: ReactNode }]
export default function PaginaLegal({ atual, titulo, resumo, secoes, extra }) {
    return (
        <>
            <Navbar />
            <main className="legal">
                <header className="legal__topo">
                    <h1>{titulo}</h1>
                    <p>{resumo}</p>
                    <p>Última atualização: {DATA_ATUALIZACAO} (versão {VERSAO_TERMOS})</p>
                </header>

                <nav className="legal__abas" aria-label="Documentos legais">
                    {ABAS.map((a) => (
                        <Link key={a.para} to={a.para} aria-current={a.para === atual ? 'page' : undefined}>{a.rotulo}</Link>
                    ))}
                </nav>

                <div className="legal__indice">
                    <strong>Neste documento</strong>
                    <ol>
                        {secoes.map((s) => <li key={s.id}><a href={`#${s.id}`}>{s.titulo}</a></li>)}
                    </ol>
                </div>

                {secoes.map((s, i) => (
                    <section key={s.id} id={s.id} className="legal__secao">
                        <h2>{i + 1}. {s.titulo}</h2>
                        {s.conteudo}
                    </section>
                ))}

                {extra}

                <p className="legal__aviso">
                    Este documento foi elaborado para a operação do BarberApp, em conformidade com a Lei Geral de Proteção
                    de Dados (Lei 13.709/2018), o Marco Civil da Internet (Lei 12.965/2014) e o Código de Defesa do
                    Consumidor (Lei 8.078/1990). Ele pode ser atualizado; mudanças relevantes serão avisadas no site.
                </p>
            </main>
        </>
    );
}
