import { useNavigate } from 'react-router-dom';
import Navbar from '../../components/Navbar.jsx';
import { BarberPole } from '../../components/Visuais.jsx';
import { lerSessao } from '../../auth/session.js';

const ACOES_CLIENTE = [
    { titulo: 'Agendar', texto: 'Escolha o barbeiro, o serviço e o horário.', rota: '/agendamento', destaque: true },
    { titulo: 'Meus agendamentos', texto: 'Acompanhe e cancele seus horários.', rota: '/histAgendamento' },
    { titulo: 'Como agendar', texto: 'Um passo a passo rápido.', rota: '/como-agendar' },
    { titulo: 'Dúvidas', texto: 'Perguntas frequentes.', rota: '/duvidas' },
    { titulo: 'Mensagens', texto: 'Converse com o barbeiro.', rota: '/mensagens' },
    { titulo: 'Problemas', texto: 'Fale com a gente.', rota: '/problemas' },
    { titulo: 'Assistente', texto: 'Converse com a IA da barbearia.', rota: '/chat' },
];

const ACOES_BARBEIRO = [
    { titulo: 'Minha agenda', texto: 'Veja seus serviços e horários.', rota: '/barbeiro' },
    { titulo: 'Mensagens', texto: 'Converse com os clientes.', rota: '/mensagens' },
    { titulo: 'Chamados', texto: 'Responda os problemas dos clientes.', rota: '/chamados' },
];

const ACOES_ADMIN = [
    { titulo: 'Painel Admin', texto: 'Gerenciar barbeiros, serviços e horários.', rota: '/admin' },
    { titulo: 'Chamados', texto: 'Responda os problemas dos clientes.', rota: '/chamados' },
];

function Home() {
    const navigate = useNavigate();
    const sessao = lerSessao();

    const isAdmin = sessao?.roles?.includes('ROLE_ADMIN');
    const isBarbeiro = sessao?.roles?.includes('ROLE_BARBEIRO');

    const acoesVisiveis = isAdmin ? ACOES_ADMIN : isBarbeiro ? ACOES_BARBEIRO : ACOES_CLIENTE;

    return (
        <>
            <Navbar />
            <main className="page">
                <section className="hero">
                    <div>
                        <h1>Seu horário, do seu <em>jeito</em>.</h1>
                        <p>Agende em segundos com o barbeiro que você prefere. Sem fila e sem ligação.</p>
                        {!isAdmin && !isBarbeiro && (
                            <button className="btn btn--primary" onClick={() => navigate('/agendamento')}>
                                Agendar agora
                            </button>
                        )}
                    </div>
                    <BarberPole />
                </section>

                <section className="acoes">
                    {acoesVisiveis.map((acao) => (
                        <button
                            key={acao.titulo}
                            className={`acao ${acao.destaque ? 'acao--destaque' : ''}`}
                            disabled={!acao.rota}
                            onClick={() => acao.rota && navigate(acao.rota)}
                        >
                            <strong>{acao.titulo}</strong>
                            <span>{acao.texto}</span>
                            <span className="acao__seta">{acao.rota ? 'Abrir →' : 'Em breve'}</span>
                        </button>
                    ))}
                </section>
            </main>
        </>
    );
}

export default Home;