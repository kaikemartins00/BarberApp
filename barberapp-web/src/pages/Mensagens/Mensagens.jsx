import { useEffect, useRef, useState } from 'react';
import api from '../../api/api.js';
import Navbar from '../../components/Navbar.jsx';
import { Title } from '../LoginECadastro/Style.js';
import { lerSessao } from '../../auth/session.js';

// "2026-10-25T14:30:12.123" -> "14:30"
const hora = (dh) => String(dh).slice(11, 16);

function Mensagens() {
    const sessao = lerSessao();
    const ehCliente = sessao?.roles?.includes('ROLE_CLIENTE');

    const [conversas, setConversas] = useState([]);
    const [barbeiros, setBarbeiros] = useState([]);
    const [conversaId, setConversaId] = useState(null);
    const [mensagens, setMensagens] = useState([]);
    const [texto, setTexto] = useState('');
    const [enviando, setEnviando] = useState(false);
    const [erro, setErro] = useState('');
    const ultimoId = useRef(0);
    const corpo = useRef(null);

    const conversaAtual = conversas.find((c) => c.id === conversaId);

    async function carregarConversas() {
        try {
            const r = await api.get('/conversas');
            setConversas(r.data);
        } catch {
            setErro('Não foi possível carregar as conversas.');
        }
    }

    // lista de conversas (atualiza a cada 8s)
    useEffect(() => {
        carregarConversas();
        if (ehCliente) {
            api.get('/barbeiros').then((r) => setBarbeiros(r.data)).catch(() => {});
        }
        const timer = setInterval(carregarConversas, 8000);
        return () => clearInterval(timer);
    }, []);

    // mensagens da conversa aberta (atualiza a cada 4s)
    useEffect(() => {
        if (!conversaId) return;
        let ativo = true;
        ultimoId.current = 0;
        setMensagens([]);

        async function buscar() {
            try {
                const r = await api.get(`/conversas/${conversaId}/mensagens`,
                    { params: { depois: ultimoId.current } });
                if (!ativo || r.data.length === 0) return;

                ultimoId.current = r.data[r.data.length - 1].id;
                setMensagens((atual) => {
                    const ids = new Set(atual.map((m) => m.id));
                    return [...atual, ...r.data.filter((m) => !ids.has(m.id))];
                });

                if (r.data.some((m) => !m.minha)) {
                    api.put(`/conversas/${conversaId}/lidas`).then(carregarConversas).catch(() => {});
                }
            } catch {
                // tenta de novo no próximo ciclo
            }
        }

        buscar();
        const timer = setInterval(buscar, 4000);
        return () => { ativo = false; clearInterval(timer); };
    }, [conversaId]);

    // rola só a caixa de mensagens, sem mexer na rolagem da página
    useEffect(() => {
        const el = corpo.current;
        if (el) el.scrollTo({ top: el.scrollHeight, behavior: 'smooth' });
    }, [mensagens]);

    async function iniciar(barbeiroId) {
        setErro('');
        try {
            const r = await api.post('/conversas', { barbeiroId });
            await carregarConversas();
            setConversaId(r.data.id);
        } catch (error) {
            setErro(error.response?.data?.message || 'Não foi possível iniciar a conversa.');
        }
    }

    async function enviar(event) {
        event.preventDefault();
        const conteudo = texto.trim();
        if (!conteudo || enviando) return;

        setErro('');
        setEnviando(true);
        try {
            const r = await api.post(`/conversas/${conversaId}/mensagens`, { texto: conteudo });
            setMensagens((atual) => (atual.some((m) => m.id === r.data.id) ? atual : [...atual, r.data]));
            setTexto('');
            carregarConversas();
        } catch (error) {
            setErro(error.response?.data?.message || 'Não foi possível enviar a mensagem.');
            carregarConversas();
        } finally {
            setEnviando(false);
        }
    }

    async function finalizar() {
        if (!window.confirm('Finalizar esta conversa? Ninguém poderá enviar novas mensagens, mas o histórico continua aqui.')) return;
        setErro('');
        try {
            await api.put(`/conversas/${conversaId}/finalizar`);
            await carregarConversas();
        } catch (error) {
            setErro(error.response?.data?.message || 'Não foi possível finalizar a conversa.');
            carregarConversas();
        }
    }

    async function reabrir() {
        setErro('');
        try {
            await api.put(`/conversas/${conversaId}/reabrir`);
            await carregarConversas();
        } catch (error) {
            setErro(error.response?.data?.message || 'Não foi possível reabrir a conversa.');
        }
    }

    function aoDigitar(event) {
        if (event.key === 'Enter' && !event.shiftKey) enviar(event);
    }

    return (
        <>
            <Navbar />
            <main className="page">
                <header className="page__head">
                    <Title>Mensagens</Title>
                    <p className="muted">
                        {ehCliente
                            ? 'Converse com o barbeiro sobre horários e serviços.'
                            : 'Conversas com os clientes.'}
                    </p>
                </header>

                {erro && <p className="alert alert--erro">{erro}</p>}

                <div className="msgs">
                    <aside className="panel msgs__lista">
                        <h2>Conversas</h2>
                        {conversas.length === 0 && <p className="muted small">Nenhuma conversa ainda.</p>}
                        {conversas.map((c) => (
                            <button key={c.id} type="button"
                                    className={`msgs__item ${c.id === conversaId ? 'msgs__item--on' : ''} ${c.encerrada ? 'msgs__item--fim' : ''}`}
                                    onClick={() => setConversaId(c.id)}>
                                <span className="lista__info">
                                    <strong>{c.interlocutorNome}</strong>
                                    <span className="muted small msgs__previa">
                                        {c.encerrada ? 'Finalizada · ' : ''}{c.ultimaMensagem || 'Sem mensagens'}
                                    </span>
                                </span>
                                {c.naoLidas > 0 && <span className="badge">{c.naoLidas}</span>}
                            </button>
                        ))}

                        {ehCliente && (
                            <>
                                <h3>Nova conversa</h3>
                                <div className="chips">
                                    {barbeiros.map((b) => (
                                        <button key={b.id} type="button" className="chip"
                                                onClick={() => iniciar(b.id)}>
                                            {b.nome}
                                        </button>
                                    ))}
                                </div>
                            </>
                        )}
                    </aside>

                    <section className="panel msgs__chat">
                        {!conversaAtual && (
                            <p className="muted">Escolha uma conversa para ver as mensagens.</p>
                        )}

                        {conversaAtual && (
                            <>
                                <div className="row row--between">
                                    <h2>{conversaAtual.interlocutorNome}</h2>
                                    {!conversaAtual.encerrada && (
                                        <button type="button" className="btn btn--ghost" onClick={finalizar}>
                                            Finalizar conversa
                                        </button>
                                    )}
                                </div>

                                <div className="msgs__corpo" ref={corpo}>
                                    {mensagens.length === 0 && (
                                        <p className="muted small">Nenhuma mensagem. Comece a conversa!</p>
                                    )}
                                    {mensagens.map((m) => (
                                        <div key={m.id} className={`balao ${m.minha ? 'balao--minha' : ''}`}>
                                            {m.texto}
                                            <span className="balao__hora">{hora(m.enviadaEm)}</span>
                                        </div>
                                    ))}
                                </div>

                                {conversaAtual.encerrada ? (
                                    <div className="msgs__aviso">
                                        Esta conversa foi finalizada.
                                        {ehCliente ? (
                                            <div>
                                                <button type="button" className="btn btn--primary" onClick={reabrir}>
                                                    Reabrir conversa
                                                </button>
                                            </div>
                                        ) : (
                                            <div className="small">O cliente pode reabri-la se precisar.</div>
                                        )}
                                    </div>
                                ) : (
                                    <form className="msgs__form" onSubmit={enviar}>
                                        <textarea rows={1} value={texto} placeholder="Escreva sua mensagem"
                                                  maxLength={1000}
                                                  onChange={(e) => setTexto(e.target.value)} onKeyDown={aoDigitar} />
                                        <button type="submit" className="btn btn--primary"
                                                disabled={enviando || !texto.trim()}>
                                            Enviar
                                        </button>
                                    </form>
                                )}
                            </>
                        )}
                    </section>
                </div>
            </main>
        </>
    );
}

export default Mensagens;