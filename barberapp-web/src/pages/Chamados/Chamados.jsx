import { useEffect, useState } from 'react';
import api from '../../api/api.js';
import Navbar from '../../components/Navbar.jsx';
import { Title } from '../LoginECadastro/Style.js';
import { lerSessao } from '../../auth/session.js';

const FILTROS = [
    { id: 'abertos', rotulo: 'Abertos' },
    { id: 'finalizados', rotulo: 'Finalizados' },
    { id: 'todos', rotulo: 'Todos' },
];
const ROTULO_STATUS = { ABERTO: 'Aberto', EM_ANDAMENTO: 'Em andamento', FINALIZADO: 'Finalizado' };

function Chamados() {
    const ehAdmin = lerSessao()?.roles?.includes('ROLE_ADMIN');

    const [chamados, setChamados] = useState([]);
    const [filtro, setFiltro] = useState('abertos');
    const [respostas, setRespostas] = useState({});
    const [salvando, setSalvando] = useState(null);
    const [carregando, setCarregando] = useState(true);
    const [erro, setErro] = useState('');

    const recarregar = () =>
        api.get('/chamados').then((r) => setChamados(r.data)).catch(() => {});

    useEffect(() => {
        api.get('/chamados')
            .then((r) => setChamados(r.data))
            .catch(() => setErro('Não foi possível carregar os chamados.'))
            .finally(() => setCarregando(false));

        // atualiza a lista para mostrar quando outra pessoa assume um chamado
        const timer = setInterval(recarregar, 10000);
        return () => clearInterval(timer);
    }, []);

    const substituir = (novo) =>
        setChamados((lista) => lista.map((c) => (c.id === novo.id ? novo : c)));

    async function assumir(chamado) {
        setErro('');
        try {
            const r = await api.post(`/chamados/${chamado.id}/assumir`);
            substituir(r.data);
        } catch (error) {
            setErro(error.response?.data?.message || 'Não foi possível assumir o chamado.');
            recarregar();
        }
    }

    async function responder(chamado) {
        const texto = (respostas[chamado.id] || '').trim();
        if (!texto) return;
        setErro('');
        setSalvando(chamado.id);
        try {
            const r = await api.post(`/chamados/${chamado.id}/respostas`, { respostaMsg: texto });
            substituir(r.data);
            setRespostas((atual) => ({ ...atual, [chamado.id]: '' }));
        } catch (error) {
            setErro(error.response?.data?.message || 'Não foi possível enviar a resposta.');
            recarregar();
        } finally {
            setSalvando(null);
        }
    }

    async function finalizar(chamado) {
        setErro('');
        try {
            const r = await api.put(`/chamados/${chamado.id}/finalizar`);
            substituir(r.data);
        } catch (error) {
            setErro(error.response?.data?.message || 'Não foi possível finalizar.');
            recarregar();
        }
    }

    const visiveis = chamados.filter((c) =>
        filtro === 'todos' ? true
            : filtro === 'finalizados' ? c.status === 'FINALIZADO'
                : c.status !== 'FINALIZADO');

    return (
        <>
            <Navbar />
            <main className="page">
                <header className="page__head">
                    <Title>Chamados</Title>
                    <p className="muted">Problemas enviados pelos clientes.</p>
                </header>

                <div className="chips">
                    {FILTROS.map((f) => (
                        <button key={f.id} type="button"
                                className={`chip ${filtro === f.id ? 'chip--on' : ''}`}
                                onClick={() => setFiltro(f.id)}>
                            {f.rotulo}
                        </button>
                    ))}
                </div>

                {erro && <p className="alert alert--erro">{erro}</p>}
                {carregando && !erro && <p className="muted">Carregando...</p>}
                {!carregando && visiveis.length === 0 && (
                    <section className="panel"><p className="muted">Nenhum chamado aqui.</p></section>
                )}

                {visiveis.map((c) => (
                    <section key={c.id} className="panel chamado">
                        <div className="chamado__topo">
                            <strong>{c.titulo}</strong>
                            <span className="badge">{ROTULO_STATUS[c.status] ?? c.status}</span>
                        </div>
                        <p className="muted small">
                            {c.clienteNome} · {c.clienteEmail}
                            {c.chamadoDate ? ` · ${new Date(c.chamadoDate).toLocaleString('pt-BR')}` : ''}
                        </p>
                        <p>{c.descricao}</p>

                        {c.status !== 'FINALIZADO' && (
                            <p className="muted small">
                                {!c.responsavelNome && 'Ninguém assumiu este chamado ainda.'}
                                {c.responsavelNome && c.podeAtender && 'Você está atendendo este chamado.'}
                                {c.responsavelNome && !c.podeAtender && `Em atendimento por ${c.responsavelNome}.`}
                            </p>
                        )}

                        {c.respostas.map((r) => (
                            <p key={r.id} className="chamado__resposta">
                                <strong>{r.autorNome}:</strong> {r.respostaMsg}
                            </p>
                        ))}

                        {c.status !== 'FINALIZADO' && c.podeAtender && (
                            <div className="form" style={{ marginTop: 8 }}>
                                <label className="field">
                                    <span>Sua resposta</span>
                                    <textarea rows={3} value={respostas[c.id] || ''}
                                              onChange={(e) => setRespostas({ ...respostas, [c.id]: e.target.value })} />
                                </label>
                                <div className="row">
                                    <button className="btn btn--primary" onClick={() => responder(c)}
                                            disabled={salvando === c.id || !(respostas[c.id] || '').trim()}>
                                        {salvando === c.id ? 'Enviando...' : 'Responder'}
                                    </button>
                                    {!c.responsavelNome && (
                                        <button className="btn btn--ghost" onClick={() => assumir(c)}>
                                            Assumir chamado
                                        </button>
                                    )}
                                    <button className="btn btn--ghost" onClick={() => finalizar(c)}>
                                        Finalizar chamado
                                    </button>
                                </div>
                            </div>
                        )}

                        {c.status !== 'FINALIZADO' && !c.podeAtender && ehAdmin && (
                            <div style={{ marginTop: 8 }}>
                                <button className="btn btn--ghost" onClick={() => assumir(c)}>
                                    Assumir este chamado
                                </button>
                            </div>
                        )}
                    </section>
                ))}
            </main>
        </>
    );
}

export default Chamados;