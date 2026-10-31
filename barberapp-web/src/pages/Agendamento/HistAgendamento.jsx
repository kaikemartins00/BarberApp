import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../../api/api.js';
import Navbar from '../../components/Navbar.jsx';
import { Title } from '../LoginECadastro/Style.js';

// GET -> lista dos agendamentos do cliente logado
const ENDPOINT_MEUS = '/agendamentos/meus';

// Aceita tanto { barbeiro: { nome } } quanto { barbeiroNome }
const nomeDe = (objeto, plano) => (typeof objeto === 'object' ? objeto?.nome : objeto) ?? plano ?? '—';

function formatarData(iso) {
    const [ano, mes, dia] = String(iso).split('-');
    return `${dia}/${mes}/${ano}`;
}

function HistAgendamento() {
    const [agendamentos, setAgendamentos] = useState([]);
    const [carregando, setCarregando] = useState(true);
    const [erro, setErro] = useState('');

    useEffect(() => {
        api.get(ENDPOINT_MEUS)
            .then((r) => {
                const ordenados = [...r.data].sort((a, b) =>
                    `${b.data} ${b.horario}`.localeCompare(`${a.data} ${a.horario}`));
                setAgendamentos(ordenados);
            })
            .catch(() => setErro('Não foi possível carregar seus agendamentos.'))
            .finally(() => setCarregando(false));
    }, []);

    async function cancelar(id) {
        if (!window.confirm('Cancelar este agendamento?')) return;
        setErro('');
        try {
            const r = await api.put(`/agendamentos/${id}/status`, null, { params: { status: 'CANCELADO' } });
            setAgendamentos((lista) => lista.map((a) => (a.id === id ? r.data : a)));
        } catch (error) {
            setErro(error.response?.data?.message || 'Não foi possível cancelar.');
        }
    }

    return (
        <>
            <Navbar />
            <main className="page">
                <header className="page__head">
                    <Title>Meus agendamentos</Title>
                    <p className="muted">Seus horários marcados, do mais recente ao mais antigo.</p>
                </header>

                {erro && <p className="alert alert--erro">{erro}</p>}
                {carregando && !erro && <p className="muted">Carregando...</p>}

                {!carregando && !erro && agendamentos.length === 0 && (
                    <section className="panel">
                        <p className="muted">Você ainda não tem agendamentos.</p>
                        <Link to="/agendamento" className="btn btn--primary">Agendar agora</Link>
                    </section>
                )}

                {agendamentos.length > 0 && (
                    <section className="panel">
                        <ul>
                            {agendamentos.map((a) => (
                                <li key={a.id} className="lista__item">
                                    <div className="avatar">{formatarData(a.data).slice(0, 2)}</div>
                                    <div className="lista__info">
                                        <strong>
                                            {nomeDe(a.servico, a.servicoNome)} com {nomeDe(a.barbeiro, a.barbeiroNome)}
                                        </strong>
                                        <span className="muted small">
                                            {formatarData(a.data)} às {String(a.horario).slice(0, 5)}
                                        </span>
                                    </div>
                                    {a.status && <span className="badge">{a.status}</span>}
                                    {(a.status === 'PENDENTE' || a.status === 'CONFIRMADO') && (
                                        <button className="btn btn--ghost" onClick={() => cancelar(a.id)}>Cancelar</button>
                                    )}
                                </li>
                            ))}
                        </ul>
                    </section>
                )}
            </main>
        </>
    );
}

export default HistAgendamento;