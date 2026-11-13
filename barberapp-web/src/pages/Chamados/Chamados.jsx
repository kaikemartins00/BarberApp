
import { useCallback, useEffect, useState } from 'react';
import {
    Headphones,
    RefreshCw,
    Inbox,
    CheckCircle2,
    Clock3,
    Send,
    UserRound,
    CalendarDays
} from 'lucide-react';
import api from '../../api/api.js';
import { lerSessao } from '../../auth/session.js';
import './Chamados.css';

const FILTROS = [
    { id: 'abertos', rotulo: 'Abertos' },
    { id: 'finalizados', rotulo: 'Finalizados' },
    { id: 'todos', rotulo: 'Todos' }
];

const ROTULO_PRIORIDADE = {ALTA: 'Prioridade alta', MEDIA: 'Prioridade média', BAIXA: 'Prioridade baixa'};

const ROTULO_STATUS = {
    ABERTO: 'Aberto',
    EM_ANDAMENTO: 'Em andamento',
    FINALIZADO: 'Finalizado'
};

export default function Chamados() {
    const ehAdmin = lerSessao()?.roles?.includes('ROLE_ADMIN');

    const [chamados, setChamados] = useState([]);
    const [filtro, setFiltro] = useState('abertos');
    const [respostas, setRespostas] = useState({});
    const [salvando, setSalvando] = useState(null);
    const [carregando, setCarregando] = useState(true);
    const [erro, setErro] = useState('');

    const recarregar = useCallback(async (mostrarCarregamento = false) => {
        if (mostrarCarregamento) {
            setCarregando(true);
        }

        try {
            const resposta = await api.get('/chamados');
            setChamados(Array.isArray(resposta.data) ? resposta.data : []);
            setErro('');
            window.dispatchEvent(new Event('barberapp:chamados-atualizados'));
        } catch {
            setErro('Não foi possível carregar os chamados.');
        } finally {
            if (mostrarCarregamento) {
                setCarregando(false);
            }
        }
    }, []);

    useEffect(() => {
        recarregar(true);

        const timer = setInterval(() => {
            recarregar();
        }, 10000);

        return () => clearInterval(timer);
    }, [recarregar]);

    const substituir = novo => {
        setChamados(lista =>
            lista.map(chamado =>
                chamado.id === novo.id ? novo : chamado
            )
        );
    };

    async function assumir(chamado) {
        setErro('');
        setSalvando(chamado.id);

        try {
            const resposta = await api.post(
                `/chamados/${chamado.id}/assumir`
            );

            substituir(resposta.data);
        } catch (error) {
            setErro(
                error.response?.data?.message ||
                'Não foi possível assumir o chamado.'
            );
            await recarregar();
        } finally {
            setSalvando(null);
        }
    }

    async function responder(chamado) {
        const texto = (respostas[chamado.id] || '').trim();

        if (!texto) return;

        setErro('');
        setSalvando(chamado.id);

        try {
            const resposta = await api.post(
                `/chamados/${chamado.id}/respostas`,
                { respostaMsg: texto }
            );

            substituir(resposta.data);

            setRespostas(atual => ({
                ...atual,
                [chamado.id]: ''
            }));
        } catch (error) {
            setErro(
                error.response?.data?.message ||
                'Não foi possível enviar a resposta.'
            );
            await recarregar();
        } finally {
            setSalvando(null);
        }
    }

    // exceção aprovada pela administração: devolve o Pix de um cancelamento fora do prazo
    async function reembolsar(chamado) {
        if (!window.confirm('Aprovar o reembolso do Pix deste agendamento? O valor será devolvido ao cliente e esse cancelamento não contará no limite mensal.')) return;
        setErro('');
        setSalvando(chamado.id);

        try {
            const resposta = await api.post(`/chamados/${chamado.id}/reembolsar`);
            substituir(resposta.data);
        } catch (error) {
            setErro(error.response?.data?.message || 'Não foi possível reembolsar.');
            await recarregar();
        } finally {
            setSalvando(null);
        }
    }

    async function finalizar(chamado) {
        setErro('');
        setSalvando(chamado.id);

        try {
            const resposta = await api.put(
                `/chamados/${chamado.id}/finalizar`
            );

            substituir(resposta.data);
        } catch (error) {
            setErro(
                error.response?.data?.message ||
                'Não foi possível finalizar o chamado.'
            );
            await recarregar();
        } finally {
            setSalvando(null);
        }
    }

    const abertos = chamados.filter(
        chamado => chamado.status !== 'FINALIZADO'
    ).length;

    const finalizados = chamados.filter(
        chamado => chamado.status === 'FINALIZADO'
    ).length;

    const visiveis = chamados.filter(chamado => {
        if (filtro === 'todos') return true;
        if (filtro === 'finalizados') {
            return chamado.status === 'FINALIZADO';
        }
        return chamado.status !== 'FINALIZADO';
    });

    return (
        <div className="chamados-page">
            <div className="chamados-header">
                <div>
                    <span className="chamados-eyebrow">
                        BARBERAPP / {ehAdmin ? 'ADMINISTRAÇÃO' : 'BARBEIRO'}
                    </span>

                    <h1>Central de chamados</h1>

                    <p>
                        Acompanhe solicitações, responda clientes e
                        gerencie os atendimentos de suporte.
                    </p>
                </div>

                <button
                    type="button"
                    className="chamados-button chamados-button--secondary"
                    onClick={() => recarregar(true)}
                    disabled={carregando}
                >
                    <RefreshCw size={16} />
                    Atualizar
                </button>
            </div>

            <div className="chamados-indicadores">
                <div className="chamados-indicador">
                    <div className="chamados-indicador-topo">
                        <span>Total de chamados</span>
                        <Headphones size={19} />
                    </div>
                    <strong>{chamados.length}</strong>
                    <small>Solicitações registradas</small>
                </div>

                <div className="chamados-indicador">
                    <div className="chamados-indicador-topo">
                        <span>Em aberto</span>
                        <Clock3 size={19} />
                    </div>
                    <strong>{abertos}</strong>
                    <small>Aguardando resolução</small>
                </div>

                <div className="chamados-indicador">
                    <div className="chamados-indicador-topo">
                        <span>Finalizados</span>
                        <CheckCircle2 size={19} />
                    </div>
                    <strong>{finalizados}</strong>
                    <small>Solicitações resolvidas</small>
                </div>
            </div>

            <section className="chamados-listagem">
                <div className="chamados-listagem-header">
                    <div>
                        <h2>Solicitações</h2>
                        <p>Gerencie os chamados recebidos.</p>
                    </div>

                    <span className="chamados-contador">
                        {visiveis.length} chamado(s)
                    </span>
                </div>

                <div className="chamados-filtros">
                    {FILTROS.map(item => (
                        <button
                            key={item.id}
                            type="button"
                            className={`chamados-filtro ${
                                filtro === item.id ? 'ativo' : ''
                            }`}
                            onClick={() => setFiltro(item.id)}
                        >
                            {item.rotulo}
                            <span>
                                {item.id === 'abertos'
                                    ? abertos
                                    : item.id === 'finalizados'
                                        ? finalizados
                                        : chamados.length}
                            </span>
                        </button>
                    ))}
                </div>

                {erro && (
                    <div className="chamados-erro" role="alert">
                        {erro}
                    </div>
                )}

                {carregando ? (
                    <div className="chamados-vazio">
                        Carregando chamados...
                    </div>
                ) : visiveis.length === 0 ? (
                    <div className="chamados-vazio">
                        <Inbox size={38} />
                        <strong>Nenhum chamado encontrado</strong>
                        <p>
                            Não existem chamados nesta categoria no momento.
                        </p>
                    </div>
                ) : (
                    <div className="chamados-cards">
                        {visiveis.map(chamado => (
                            <article
                                key={chamado.id}
                                className="chamados-card"
                            >
                                <div className="chamados-card-topo">
                                    <div>
                                        <span className="chamados-protocolo">
                                            CHAMADO #{chamado.id}
                                        </span>
                                        <h3>{chamado.titulo}</h3>
                                    </div>

                                    {chamado.status !== 'FINALIZADO' && chamado.prioridade && (
                                        <span className={`chamados-prioridade chamados-prioridade--${chamado.prioridade.toLowerCase()}`}>
                                            {ROTULO_PRIORIDADE[chamado.prioridade]}
                                        </span>
                                    )}

                                    <span
                                        className={`chamados-status chamados-status--${chamado.status?.toLowerCase()}`}
                                    >
                                        {ROTULO_STATUS[chamado.status] ??
                                            chamado.status}
                                    </span>
                                </div>

                                <div className="chamados-meta">
                                    <span>
                                        <UserRound size={15} />
                                        {chamado.clienteNome}
                                    </span>

                                    {chamado.clienteEmail && (
                                        <span>{chamado.clienteEmail}</span>
                                    )}

                                    {chamado.chamadoDate && (
                                        <span>
                                            <CalendarDays size={15} />
                                            {new Date(
                                                chamado.chamadoDate
                                            ).toLocaleString('pt-BR')}
                                        </span>
                                    )}
                                </div>

                                <p className="chamados-descricao">
                                    {chamado.descricao}
                                </p>

                                {chamado.agendamentoId && chamado.status !== 'FINALIZADO' && (
                                    <div className="chamados-responsavel">
                                        Pedido de análise de um agendamento cancelado (nº {chamado.agendamentoId}).
                                        {ehAdmin && chamado.reembolsavel && chamado.podeAtender && (
                                            <div className="chamados-botoes" style={{ marginTop: 8 }}>
                                                <button
                                                    type="button"
                                                    className="chamados-button chamados-button--primary"
                                                    disabled={salvando === chamado.id}
                                                    onClick={() => reembolsar(chamado)}
                                                >
                                                    Aprovar reembolso do Pix
                                                </button>
                                            </div>
                                        )}
                                        {!ehAdmin && chamado.reembolsavel && ' Só a administração decide sobre reembolsos.'}
                                        {ehAdmin && !chamado.reembolsavel && ' Não há Pix pago a reembolsar neste agendamento.'}
                                    </div>
                                )}

                                {chamado.status !== 'FINALIZADO' && (
                                    <div className="chamados-responsavel">
                                        {!chamado.responsavelNome
                                            ? 'Aguardando um responsável.'
                                            : chamado.podeAtender
                                                ? 'Você está atendendo este chamado.'
                                                : `Em atendimento por ${chamado.responsavelNome}.`}
                                    </div>
                                )}

                                {Array.isArray(chamado.respostas) &&
                                    chamado.respostas.length > 0 && (
                                        <div className="chamados-respostas">
                                            <h4>Histórico de respostas</h4>

                                            {chamado.respostas.map(resposta => (
                                                <div
                                                    key={resposta.id}
                                                    className="chamados-resposta"
                                                >
                                                    <strong>
                                                        {resposta.autorNome}
                                                    </strong>
                                                    <p>
                                                        {resposta.respostaMsg}
                                                    </p>
                                                </div>
                                            ))}
                                        </div>
                                    )}

                                {chamado.status !== 'FINALIZADO' &&
                                    chamado.podeAtender && (
                                        <div className="chamados-acoes">
                                            <label htmlFor={`resposta-${chamado.id}`}>
                                                Sua resposta
                                            </label>

                                            <textarea
                                                id={`resposta-${chamado.id}`}
                                                rows={3}
                                                placeholder="Escreva sua resposta ao cliente..."
                                                value={respostas[chamado.id] || ''}
                                                onChange={event =>
                                                    setRespostas(atual => ({
                                                        ...atual,
                                                        [chamado.id]: event.target.value
                                                    }))
                                                }
                                            />

                                            <div className="chamados-botoes">
                                                <button
                                                    type="button"
                                                    className="chamados-button chamados-button--primary"
                                                    disabled={
                                                        salvando === chamado.id ||
                                                        !(respostas[chamado.id] || '').trim()
                                                    }
                                                    onClick={() => responder(chamado)}
                                                >
                                                    <Send size={15} />
                                                    Responder
                                                </button>

                                                {!chamado.responsavelNome && (
                                                    <button
                                                        type="button"
                                                        className="chamados-button chamados-button--secondary"
                                                        disabled={salvando === chamado.id}
                                                        onClick={() => assumir(chamado)}
                                                    >
                                                        Assumir chamado
                                                    </button>
                                                )}

                                                <button
                                                    type="button"
                                                    className="chamados-button chamados-button--secondary"
                                                    disabled={salvando === chamado.id}
                                                    onClick={() => finalizar(chamado)}
                                                >
                                                    <CheckCircle2 size={15} />
                                                    Finalizar
                                                </button>
                                            </div>
                                        </div>
                                    )}

                                {chamado.status !== 'FINALIZADO' &&
                                    !chamado.podeAtender &&
                                    ehAdmin && (
                                        <div className="chamados-botoes">
                                            <button
                                                type="button"
                                                className="chamados-button chamados-button--primary"
                                                disabled={salvando === chamado.id}
                                                onClick={() => assumir(chamado)}
                                            >
                                                Assumir este chamado
                                            </button>
                                        </div>
                                    )}
                            </article>
                        ))}
                    </div>
                )}
            </section>
        </div>
    );
}
