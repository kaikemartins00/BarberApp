import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../../api/api.js';
import Navbar from '../../components/Navbar.jsx';
import { Title } from '../LoginECadastro/Style.js';

// POST { barbeiroId, servicoId, data: "2026-10-20", horario: "14:00" }
const ENDPOINT_AGENDAR = '/agendamentos';

const hora = (h) => String(h).slice(0, 5); // "09:00:00" -> "09:00"

// O backend usa @Future na data, então hoje não é aceito: o primeiro dia é amanhã.
function amanha() {
    const d = new Date();
    d.setDate(d.getDate() + 1);
    const mes = String(d.getMonth() + 1).padStart(2, '0');
    const dia = String(d.getDate()).padStart(2, '0');
    return `${d.getFullYear()}-${mes}-${dia}`;
}

function formatarData(iso) {
    const [ano, mes, dia] = iso.split('-');
    return `${dia}/${mes}/${ano}`;
}

function Agendamento() {
    const [barbeiros, setBarbeiros] = useState([]);
    const [carregando, setCarregando] = useState(true);
    const [erroBarbeiros, setErroBarbeiros] = useState('');
    const [barbeiro, setBarbeiro] = useState(null);
    const [servico, setServico] = useState(null);
    const [data, setData] = useState(amanha());
    const [horarios, setHorarios] = useState([]);
    const [horario, setHorario] = useState('');
    const [enviando, setEnviando] = useState(false);
    const [erro, setErro] = useState('');
    const [sucesso, setSucesso] = useState('');

    // Carrega os barbeiros ao abrir a tela
    useEffect(() => {
        api.get('/barbeiros')
            .then((r) => setBarbeiros(r.data))
            .catch(() => setErroBarbeiros('Não foi possível carregar os barbeiros.'))
            .finally(() => setCarregando(false));
    }, []);

    // Carrega os horários livres quando muda o barbeiro ou o dia
    useEffect(() => {
        if (!barbeiro || !data) return;
        let ativo = true;                       // evita resposta antiga sobrescrever a nova
        setHorario('');
        api.get('/agendamentos/horarios-livres', { params: { barbeiroId: barbeiro.id, data } })
            .then((r) => { if (ativo) setHorarios(r.data.map(hora)); })
            .catch(() => { if (ativo) setHorarios([]); });
        return () => { ativo = false; };
    }, [barbeiro, data]);

    function escolherBarbeiro(b) {
        setBarbeiro(b);
        setServico(null);
        setHorario('');
        setErro('');
        setSucesso('');
    }

    const servicos = barbeiro?.servicos || [];
    const pronto = barbeiro && servico && data && horario;

    async function confirmar() {
        setErro('');
        setSucesso('');
        setEnviando(true);

        try {
            await api.post(ENDPOINT_AGENDAR, {
                barbeiroId: barbeiro.id,
                servicoId: servico.id,
                data,
                horario,
            });
            setSucesso(`Agendado! ${servico.nome} com ${barbeiro.nome}, dia ${formatarData(data)} às ${horario}.`);
            setHorarios((lista) => lista.filter((h) => h !== horario));
            setHorario('');
        } catch (error) {
            setErro(error.response?.data?.message || 'Não foi possível agendar. Tente outro horário.');
        } finally {
            setEnviando(false);
        }
    }

    return (
        <>
            <Navbar />
            <main className="page">
                <header className="page__head">
                    <Title>Agende seu horário</Title>
                    <p className="muted">Escolha o barbeiro, o serviço, o dia e o horário.</p>
                </header>

                <section className="panel">
                    <h2>1. Barbeiro</h2>
                    {carregando && <p className="muted">Carregando...</p>}
                    {erroBarbeiros && <p className="alert alert--erro">{erroBarbeiros}</p>}
                    {!carregando && !erroBarbeiros && barbeiros.length === 0 && (
                        <p className="muted">Nenhum barbeiro disponível no momento.</p>
                    )}
                    <div className="escolhas">
                        {barbeiros.map((b) => (
                            <button key={b.id} type="button"
                                    className={`escolha ${barbeiro?.id === b.id ? 'escolha--on' : ''}`}
                                    onClick={() => escolherBarbeiro(b)}>
                                {b.fotoUrl ? (
                                    <img className="avatar avatar--foto" src={b.fotoUrl} alt="" />
                                ) : (
                                    <span className="avatar">{b.nome.charAt(0).toUpperCase()}</span>
                                )}
                                <span className="lista__info">
                                    <strong>{b.nome}</strong>
                                    <span className="muted small">{b.servicos?.length ?? 0} serviços</span>
                                </span>
                            </button>
                        ))}
                    </div>
                </section>

                {barbeiro && (
                    <section className="panel">
                        <h2>2. Serviço</h2>
                        {servicos.length === 0 && (
                            <p className="muted">Este barbeiro ainda não tem serviços cadastrados.</p>
                        )}
                        <div className="chips">
                            {servicos.map((s) => (
                                <button key={s.id} type="button"
                                        className={`chip ${servico?.id === s.id ? 'chip--on' : ''}`}
                                        onClick={() => setServico(s)}>
                                    {s.nome} · R$ {Number(s.preco).toFixed(2)}
                                </button>
                            ))}
                        </div>
                    </section>
                )}

                {barbeiro && servico && (
                    <section className="panel">
                        <h2>3. Dia e horário</h2>
                        <label className="field" style={{ maxWidth: 220 }}>
                            <span>Dia</span>
                            <input type="date" value={data} min={amanha()}
                                   onChange={(e) => { setData(e.target.value); setSucesso(''); }} />
                        </label>

                        <h3>Horários de {barbeiro.nome}</h3>
                        {horarios.length === 0 && (
                            <p className="muted">Sem horários livres neste dia.</p>
                        )}
                        <div className="chips">
                            {horarios.map((h) => (
                                <button key={h} type="button"
                                        className={`chip ${horario === h ? 'chip--on' : ''}`}
                                        onClick={() => setHorario(h)}>
                                    {h}
                                </button>
                            ))}
                        </div>
                    </section>
                )}

                {pronto && (
                    <section className="panel resumo">
                        <h2>Confirmar</h2>
                        <p>
                            <strong>{servico.nome}</strong> com <strong>{barbeiro.nome}</strong><br />
                            {formatarData(data)} às {horario} · R$ {Number(servico.preco).toFixed(2)}
                        </p>
                        <button className="btn btn--primary" onClick={confirmar} disabled={enviando}>
                            {enviando ? 'Agendando...' : 'Confirmar agendamento'}
                        </button>
                    </section>
                )}

                {erro && <p className="alert alert--erro">{erro}</p>}
                {sucesso && (
                    <p className="alert alert--ok">
                        {sucesso} <Link to="/histAgendamento">Ver meus agendamentos</Link>
                    </p>
                )}
            </main>
        </>
    );
}

export default Agendamento;