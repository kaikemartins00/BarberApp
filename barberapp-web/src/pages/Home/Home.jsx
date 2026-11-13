
import { useEffect, useState } from 'react';
import { Link, Navigate } from 'react-router-dom';
import api from '../../api/api.js';
import { lerSessao } from '../../auth/session.js';
import Navbar from '../../components/Navbar.jsx';
import { dadosRepetir, dataBr, hora, plural, resumirCliente } from '../../utils/resumoCliente.js';

const ESTILOS = `
.dash-acoes { display:flex; flex-wrap:wrap; align-items:center; gap:10px; margin-top:12px }
.dash-acoes a:not(.dashboard-botao) { padding:8px 14px; border-radius:10px; font-weight:600; text-decoration:none; color:var(--text, #fff); border:1px solid var(--line, #2a2a31) }
.dash-acoes a:not(.dashboard-botao):hover { border-color:var(--accent, #d4a24c) }
.dash-lista { display:flex; flex-direction:column; margin:8px 0 0; padding:0; list-style:none }
.dash-lista li { display:flex; justify-content:space-between; align-items:center; gap:12px; padding:9px 0; border-bottom:1px solid var(--line, #2a2a31) }
.dash-lista li:last-child { border-bottom:0 }
.dash-lista small { color:var(--muted, #a1a1aa) }
`;

function Home() {
    const sessao = lerSessao();
    const roles = sessao?.roles || [];
    const cliente = roles.includes('ROLE_CLIENTE');
    const barbeiro = roles.includes('ROLE_BARBEIRO');
    const admin = roles.includes('ROLE_ADMIN');

    const [agendamentos, setAgendamentos] = useState([]);
    const [barbeiros, setBarbeiros] = useState([]);
    const [cancelamentos, setCancelamentos] = useState(null);
    const [notas, setNotas] = useState({});
    const [populares, setPopulares] = useState([]);
    const [melhorBarbeiro, setMelhorBarbeiro] = useState(null);
    const [carregando, setCarregando] = useState(true);
    const [erro, setErro] = useState('');

    useEffect(() => {
        if (!cliente || barbeiro || admin) {
            setCarregando(false);
            return;
        }

        let ativo = true;

        Promise.all([
            api.get('/agendamentos/meus'),
            api.get('/barbeiros'),
            api.get('/agendamentos/meus/cancelamentos', { semRedirect: true }).catch(() => ({ data: null })),
            api.get('/cliente/painel/avaliacoes', { semRedirect: true }).catch(() => ({ data: [] })),
            api.get('/cliente/painel/servicos-populares', { semRedirect: true }).catch(() => ({ data: [] }))
        ])
            .then(([respostaAgendamentos, respostaBarbeiros, respostaCancelamentos, respostaNotas, respostaPopulares]) => {
                if (!ativo) return;

                setAgendamentos(respostaAgendamentos.data);
                setBarbeiros(respostaBarbeiros.data);
                setCancelamentos(respostaCancelamentos.data);
                setPopulares(respostaPopulares.data || []);

                const mapa = {};
                (respostaNotas.data || []).forEach((n) => {
                    mapa[n.agendamentoId] = n.nota;
                });
                setNotas(mapa);

                Promise.all((respostaBarbeiros.data || []).map((b) =>
                    api.get(`/avaliacoes/barbeiros/${b.id}/resumo`, { semRedirect: true })
                        .then((r) => ({ nome: b.nome, media: Number(r.data.media), quantidade: r.data.quantidade }))
                        .catch(() => null)
                )).then((lista) => {
                    if (!ativo) return;

                    const avaliados = lista
                        .filter((item) => item && item.quantidade > 0)
                        .sort((a, b) => b.media - a.media || b.quantidade - a.quantidade);

                    setMelhorBarbeiro(avaliados[0] || null);
                });
            })
            .catch(() => {
                if (ativo) {
                    setErro('Não foi possível carregar todos os dados.');
                }
            })
            .finally(() => {
                if (ativo) setCarregando(false);
            });

        return () => {
            ativo = false;
        };
    }, [cliente, barbeiro, admin]);

    if (barbeiro && !admin) {
        return <Navigate to="/barbeiro" replace />;
    }

    if (admin) {
        return <Navigate to="/admin" replace />;
    }

    const agora = new Date();

    const proximos = agendamentos
        .filter((agendamento) => {
            if (!['PENDENTE', 'CONFIRMADO'].includes(agendamento.status)) {
                return false;
            }

            return new Date(
                `${agendamento.data}T${String(agendamento.horario).slice(0, 5)}:00`
            ) >= agora;
        })
        .sort((a, b) =>
            `${a.data}T${a.horario}`.localeCompare(`${b.data}T${b.horario}`)
        );

    const proximo = proximos[0];

    const concluidos = agendamentos.filter(
        (agendamento) => agendamento.status === 'CONCLUIDO'
    );

    const resumo = resumirCliente(agendamentos);
    const ultimo = resumo.ultimo;
    const repetirUltimo = ultimo ? dadosRepetir(ultimo, barbeiros) : null;

    const servicosSugeridos = populares.length
        ? populares.map((p) => ({
            nome: p.servico,
            detalhe: plural(p.quantidade, 'agendamento', 'agendamentos')
        }))
        : [...new Set(barbeiros.flatMap((b) => (b.servicos || []).map((s) => s.nome)))]
            .slice(0, 3)
            .map((nome) => ({ nome, detalhe: '' }));

    function dataFormatada(data) {
        if (!data) return '';

        return new Date(`${data}T12:00:00`).toLocaleDateString('pt-BR', {
            weekday: 'short',
            day: '2-digit',
            month: 'short'
        });
    }

    return (
        <>
            <Navbar />

            <main className="dashboard-cliente">
                <style>{ESTILOS}</style>
                <div className="dashboard-topo">
                    <div>
                        <h1>
                            Olá! <span>Bora dar um trato?</span>
                        </h1>

                        <p>
                            Seu horário, do seu jeito — sem fila e sem ligação.
                        </p>
                    </div>

                    {cliente && (
                        <Link to="/agendamento" className="dashboard-botao">
                            + Agendar horário
                        </Link>
                    )}
                </div>

                {!cliente && (
                    <section className="dashboard-acoes-gerais">
                        <Link to="/login" className="dashboard-card">
                            <h3>Entrar</h3>
                            <p>Acesse sua conta.</p>
                            <span>Abrir →</span>
                        </Link>

                        <Link to="/cadastro" className="dashboard-card">
                            <h3>Cadastrar</h3>
                            <p>Crie sua conta.</p>
                            <span>Abrir →</span>
                        </Link>

                        <Link to="/como-agendar" className="dashboard-card">
                            <h3>Como agendar</h3>
                            <p>Saiba como funciona.</p>
                            <span>Abrir →</span>
                        </Link>
                    </section>
                )}

                {cliente && (
                    <>
                        {erro && (
                            <p className="alert alert--erro">{erro}</p>
                        )}

                        <section className="dashboard-proximo">
                            {carregando ? (
                                <p>Carregando próximo horário...</p>
                            ) : proximo ? (
                                <>
                                    <div className="dashboard-data">
                                        <strong>
                                            {new Date(`${proximo.data}T12:00:00`).getDate()}
                                        </strong>

                                        <span>
                                            {new Date(`${proximo.data}T12:00:00`)
                                                .toLocaleDateString('pt-BR', {
                                                    month: 'short'
                                                })}
                                        </span>
                                    </div>

                                    <div className="dashboard-proximo-info">
                                        <small>
                                            PRÓXIMO HORÁRIO · {dataFormatada(proximo.data)}
                                        </small>

                                        <h3>
                                            {String(proximo.horario).slice(0, 5)}
                                            {' · '}
                                            {proximo.servicoNome}
                                        </h3>

                                        <p>com {proximo.barbeiroNome}</p>
                                    </div>

                                    <div className="dashboard-proximo-acoes">
                                        <span>{proximo.status}</span>

                                        <Link to="/histAgendamento">
                                            Gerenciar
                                        </Link>
                                    </div>
                                </>
                            ) : (
                                <>
                                    <div className="dashboard-proximo-info">
                                        <small>PRÓXIMO HORÁRIO</small>
                                        <h3>Nenhum horário marcado</h3>
                                        <p>
                                            {ultimo
                                                ? `Seu último foi ${ultimo.servicoNome} com ${ultimo.barbeiroNome}, em ${dataBr(ultimo.data)}.`
                                                : 'Agende seu próximo atendimento.'}
                                        </p>
                                    </div>

                                    <div className="dash-acoes">
                                        {repetirUltimo && (
                                            <Link to="/agendamento" state={repetirUltimo} className="dashboard-botao">
                                                Repetir último
                                            </Link>
                                        )}

                                        <Link to="/agendamento" className="dashboard-botao">
                                            Agendar
                                        </Link>
                                    </div>
                                </>
                            )}
                        </section>

                        <section className="dashboard-estatisticas">
                            <article className="dashboard-card destaque-dourado">
                                <span>Próximos</span>
                                <strong>{proximos.length}</strong>
                                <small>agendamentos futuros</small>
                            </article>

                            <article className="dashboard-card destaque-verde">
                                <span>Cortes feitos</span>
                                <strong>{concluidos.length}</strong>
                                <small>atendimentos concluídos</small>
                            </article>

                            <article className="dashboard-card destaque-azul">
                                {resumo.barbeiroFavorito ? (
                                    <>
                                        <span>Seu barbeiro</span>
                                        <strong>{resumo.barbeiroFavorito.nome}</strong>
                                        <small>
                                            {plural(resumo.barbeiroFavorito.concluidos, 'concluído', 'concluídos')}
                                            {' e '}
                                            {plural(resumo.barbeiroFavorito.cancelados, 'cancelado', 'cancelados')}
                                        </small>
                                    </>
                                ) : melhorBarbeiro ? (
                                    <>
                                        <span>Mais bem avaliado</span>
                                        <strong>{melhorBarbeiro.nome}</strong>
                                        <small>
                                            ★ {melhorBarbeiro.media.toFixed(1)} · {plural(melhorBarbeiro.quantidade, 'avaliação', 'avaliações')}
                                        </small>
                                    </>
                                ) : (
                                    <>
                                        <span>Barbeiros</span>
                                        <strong>{barbeiros.length}</strong>
                                        <small>disponíveis para agendar</small>
                                    </>
                                )}
                            </article>

                            <article className="dashboard-card destaque-dourado">
                                <span>Cancelamentos</span>
                                <strong>
                                    {cancelamentos ? `${cancelamentos.usados} de ${cancelamentos.limite}` : '—'}
                                </strong>
                                <small>
                                    {cancelamentos?.bloqueadoAte
                                        ? `Bloqueado até ${dataBr(cancelamentos.bloqueadoAte)}`
                                        : 'usados neste mês'}
                                </small>
                            </article>
                        </section>

                        <div className="dashboard-duas-colunas">
                            <section className="dashboard-card">
                                <h3>Atalhos</h3>

                                <div className="dashboard-atalhos">
                                    <Link to="/agendamento">
                                        <span>＋</span>
                                        <strong>Novo horário</strong>
                                        <small>Agende seu corte</small>
                                    </Link>

                                    <Link to="/histAgendamento">
                                        <span>↻</span>
                                        <strong>Meus horários</strong>
                                        <small>Consulte e gerencie</small>
                                    </Link>

                                    <Link to="/chat">
                                        <span>?</span>
                                        <strong>Precisa de ajuda?</strong>
                                        <small>Assistente e dúvidas</small>
                                    </Link>

                                    {agendamentos.some(a => a.status !== 'CANCELADO') ? (
                                        <Link to="/mensagens">
                                            <span>✉</span>
                                            <strong>Falar com o barbeiro</strong>
                                            <small>Quem tem horário com você</small>
                                        </Link>
                                    ) : (
                                        <a aria-disabled="true" style={{opacity: .5, cursor: 'not-allowed'}}
                                           onClick={e => e.preventDefault()}>
                                            <span>✉</span>
                                            <strong>Falar com o barbeiro</strong>
                                            <small>Disponível após agendar</small>
                                        </a>
                                    )}

                                    <Link to="/problemas">
                                        <span>⚑</span>
                                        <strong>Abrir chamado</strong>
                                        <small>Relate um problema</small>
                                    </Link>
                                </div>
                            </section>

                            <section className="dashboard-card">
                                {ultimo ? (
                                    <>
                                        <h3>Seu último atendimento</h3>
                                        <p>
                                            <strong>{ultimo.servicoNome}</strong> com {ultimo.barbeiroNome}
                                        </p>
                                        <small>
                                            {dataBr(ultimo.data)} às {hora(ultimo.horario)}
                                        </small>

                                        <div className="dash-acoes">
                                            {notas[ultimo.id] ? (
                                                <span>Você avaliou: ★ {notas[ultimo.id]} de 5</span>
                                            ) : (
                                                <Link to="/histAgendamento" className="dashboard-botao">
                                                    Avaliar atendimento
                                                </Link>
                                            )}

                                            {repetirUltimo && (
                                                <Link to="/agendamento" state={repetirUltimo}>Repetir</Link>
                                            )}
                                        </div>
                                    </>
                                ) : (
                                    <>
                                        <h3>Comece por aqui</h3>
                                        <p>
                                            {populares.length
                                                ? 'Serviços mais pedidos na barbearia'
                                                : 'Serviços disponíveis'}
                                        </p>

                                        <ul className="dash-lista">
                                            {servicosSugeridos.map((servico) => (
                                                <li key={servico.nome}>
                                                    <span>{servico.nome}</span>
                                                    <small>{servico.detalhe}</small>
                                                </li>
                                            ))}
                                        </ul>

                                        <div className="dash-acoes">
                                            <Link to="/agendamento" className="dashboard-botao">
                                                Agendar meu primeiro horário
                                            </Link>
                                        </div>
                                    </>
                                )}
                            </section>
                        </div>

                        <section className="dashboard-card dashboard-barbeiros">
                            <h3>Nossos barbeiros</h3>
                            <p>Escolha com quem quer cortar</p>

                            <div className="dashboard-barbeiros-lista">
                                {barbeiros.map((b) => (
                                    <article key={b.id}>
                                        <div className="dashboard-avatar" style={{position: 'relative', overflow: 'hidden'}}>
                                            {b.nome?.slice(0, 2).toUpperCase()}
                                            {b.fotoUrl && (
                                                <img src={b.fotoUrl} alt=""
                                                     onError={(e) => e.currentTarget.remove()}
                                                     style={{position: 'absolute', inset: 0, width: '100%', height: '100%', objectFit: 'cover'}}/>
                                            )}
                                        </div>

                                        <div>
                                            <strong>{b.nome}</strong>
                                            <small>
                                                {b.servicos?.length || 0} serviços
                                            </small>
                                        </div>

                                        <Link to="/agendamento">
                                            Horários
                                        </Link>
                                    </article>
                                ))}
                            </div>
                        </section>
                    </>
                )}
            </main>
        </>
    );
}

export default Home;
