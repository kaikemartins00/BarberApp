import { useEffect, useState } from 'react';
import api from '../../api/api.js';
import Navbar from '../../components/Navbar.jsx';
import { Title } from '../LoginECadastro/Style.js';

const ASSUNTOS = ['Não consegui agendar', 'Problema com o código de verificação', 'Erro no site', 'Outro'];
const ROTULO_STATUS = { ABERTO: 'Aberto', EM_ANDAMENTO: 'Em andamento', FINALIZADO: 'Finalizado' };

function Problemas() {
    const [titulo, setTitulo] = useState(ASSUNTOS[0]);
    const [descricao, setDescricao] = useState('');
    const [meus, setMeus] = useState([]);
    const [enviando, setEnviando] = useState(false);
    const [erro, setErro] = useState('');
    const [sucesso, setSucesso] = useState('');

    const carregar = () => api.get('/chamados/meus').then((r) => setMeus(r.data)).catch(() => {});

    useEffect(() => { carregar(); }, []);

    async function enviar(event) {
        event.preventDefault();
        setErro('');
        setSucesso('');
        setEnviando(true);
        try {
            await api.post('/chamados', { titulo, descricao });
            setSucesso('Chamado enviado! Assim que responderem, a resposta aparece aqui embaixo.');
            setDescricao('');
            carregar();
        } catch (error) {
            setErro(error.response?.data?.message || 'Não foi possível enviar o chamado.');
        } finally {
            setEnviando(false);
        }
    }

    return (
        <>
            <Navbar />
            <main className="page">
                <header className="page__head">
                    <Title>Problemas</Title>
                    <p className="muted">Abra um chamado e acompanhe a resposta aqui.</p>
                </header>

                <section className="panel">
                    <form className="form" style={{ marginTop: 0 }} onSubmit={enviar}>
                        <label className="field">
                            <span>Assunto</span>
                            <select value={titulo} onChange={(e) => setTitulo(e.target.value)}>
                                {ASSUNTOS.map((a) => <option key={a} value={a}>{a}</option>)}
                            </select>
                        </label>

                        <label className="field">
                            <span>O que aconteceu?</span>
                            <textarea rows={5} placeholder="Descreva o problema com o máximo de detalhes"
                                      value={descricao} onChange={(e) => setDescricao(e.target.value)} required />
                        </label>

                        {erro && <p className="alert alert--erro">{erro}</p>}
                        {sucesso && <p className="alert alert--ok">{sucesso}</p>}

                        <button type="submit" className="btn btn--primary" disabled={enviando}>
                            {enviando ? 'Enviando...' : 'Enviar chamado'}
                        </button>
                    </form>
                </section>

                {meus.length > 0 && (
                    <section className="panel">
                        <h2>Meus chamados</h2>
                        {meus.map((c) => (
                            <article key={c.id} className="chamado">
                                <div className="chamado__topo">
                                    <strong>{c.titulo}</strong>
                                    <span className="badge">{ROTULO_STATUS[c.status] ?? c.status}</span>
                                </div>
                                <p>{c.descricao}</p>
                                {c.respostas.map((r) => (
                                    <p key={r.id} className="chamado__resposta">
                                        <strong>{r.autorNome}:</strong> {r.respostaMsg}
                                    </p>
                                ))}
                            </article>
                        ))}
                    </section>
                )}
            </main>
        </>
    );
}

export default Problemas;