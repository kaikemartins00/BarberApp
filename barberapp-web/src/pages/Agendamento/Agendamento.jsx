import {useEffect, useRef, useState} from 'react';
import {Link, useLocation} from 'react-router-dom';
import api from '../../api/api.js';
import Navbar from '../../components/Navbar.jsx';
import {Title} from '../LoginECadastro/Style.js';
import './PagamentoPix.css';
import NotaBarbeiro from '../../components/NotaBarbeiro.jsx';
import FilaEspera from '../../components/FilaEspera.jsx';

const hora = valor => String(valor).slice(0, 5);
const dataHoje = () => {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
};
const dataFormatada = iso => iso.split('-').reverse().join('/');
const moeda = valor => Number(valor || 0).toLocaleString('pt-BR', {style: 'currency', currency: 'BRL'});
const ROTULOS_STATUS = {
    PENDENTE: 'Aguardando forma de pagamento',
    AGUARDANDO_CONFIRMACAO: 'Aguardando pagamento do Pix',
    PAGO: 'Pago',
    CANCELADO: 'Cancelado'
};
const rotuloStatus = status => ROTULOS_STATUS[status] || status;
const ESTILOS_PAGAMENTO = `
.pag { display:flex; flex-direction:column; gap:20px; padding:28px }
.pag h2, .pag h3, .pag p { margin:0 }
.pag-resumo { display:flex; justify-content:space-between; align-items:flex-start; gap:20px; padding-bottom:20px; border-bottom:1px solid var(--line) }
.pag-resumo small { display:block; margin-bottom:6px; letter-spacing:.04em }
.pag-resumo h2 { font-size:1.35rem; margin-bottom:6px }
.pag-valor { font-size:1.5rem; color:var(--accent-strong); white-space:nowrap }
.pag-status { align-self:flex-start; padding:6px 14px; border-radius:999px; font-size:.82rem; font-weight:600; color:var(--muted); background:var(--surface-2); border:1px solid var(--line) }
.pag-status--pendente, .pag-status--aguardando_confirmacao { color:var(--accent-strong); border-color:var(--accent); background:rgba(212,162,76,.12) }
.pag-status--pago { color:var(--ok); border-color:rgba(123,216,164,.45); background:rgba(123,216,164,.12) }
.pag h3 { font-size:1.05rem }
.pag-opcoes { display:grid; grid-template-columns:1fr 1fr; gap:14px }
.pag-opcao { display:flex; flex-direction:column; gap:6px; text-align:left; font:inherit; cursor:pointer; padding:16px 18px; border-radius:12px; color:var(--text); background:var(--surface-2); border:1px solid var(--line); transition:border-color .15s, background .15s }
.pag-opcao strong { font-size:1rem }
.pag-opcao span { font-size:.86rem; line-height:1.4; color:var(--muted) }
.pag-opcao:hover { border-color:var(--accent) }
.pag-opcao--on { border-color:var(--accent); background:rgba(212,162,76,.14) }
.pag .barber-pix-box { margin-top:0; padding:28px; gap:20px }
.pag-passos { align-self:center; margin:0; padding-left:20px; font-size:.9rem; line-height:1.9; color:var(--muted) }
.pag-aguardando { display:flex; align-items:center; gap:10px; font-size:.92rem; color:var(--muted) }
.pag-ponto { width:10px; height:10px; border-radius:50%; background:var(--accent); animation:pag-pulso 1.2s ease-in-out infinite }
@keyframes pag-pulso { 0%, 100% { opacity:.25; transform:scale(.8) } 50% { opacity:1; transform:scale(1) } }
.pag-ok { display:flex; flex-direction:column; align-items:center; gap:10px; text-align:center; padding:34px 20px; border-radius:16px; background:rgba(123,216,164,.09); border:1px solid rgba(123,216,164,.3) }
.pag-ok__icone { display:inline-flex; align-items:center; justify-content:center; width:64px; height:64px; margin-bottom:6px; border-radius:50%; font-size:2rem; line-height:1; color:#fff; background:var(--ok) }
.pag-ok h3 { font-size:1.4rem }
.pag-ok p { max-width:360px; line-height:1.5 }
.pag-acoes { display:flex; flex-wrap:wrap; gap:14px; padding-top:22px; border-top:1px solid var(--line) }
.pag-acoes .btn { padding:12px 22px }
.pag a.btn, .pag a.btn:hover, .pag a.btn:focus { text-decoration:none }
.pag a.btn--primary, .pag a.btn--primary:hover, .pag a.btn--primary:focus { color:var(--accent-ink) }
.pag a.btn--ghost { color:var(--text) }
.pag a.btn--ghost:hover { color:var(--accent-strong) }
@media (max-width:560px) { .pag { padding:20px } .pag-opcoes { grid-template-columns:1fr } .pag-resumo { flex-direction:column; gap:10px } }
`;
const erroApi = (e, padrao) => e.response?.data?.message || padrao;
const horarioFuturo = (dia, h) => new Date(`${dia}T${hora(h)}:00`).getTime() > Date.now();

export default function Agendamento() {
    const [barbeiros, setBarbeiros] = useState([]);
    const [carregando, setCarregando] = useState(true);
    const [erroBarbeiros, setErroBarbeiros] = useState('');
    const [barbeiro, setBarbeiro] = useState(null);
    const [servico, setServico] = useState(null);
    const location = useLocation();
    const [repetir] = useState(() => location.state?.repetir || null);
    const horarioPedido = useRef(repetir?.horario || null);
    const [data, setData] = useState(() => repetir?.data || dataHoje());
    const [horarios, setHorarios] = useState([]);
    const [horariosCarregados, setHorariosCarregados] = useState(false);
    const [horario, setHorario] = useState('');
    const [enviando, setEnviando] = useState(false);
    const [erro, setErro] = useState('');
    const [sucesso, setSucesso] = useState('');
    const [agendamentoCriado, setAgendamentoCriado] = useState(null);
    const [pagamento, setPagamento] = useState(null);
    const [metodo, setMetodo] = useState('PRESENCIAL');
    const [aceitouPolitica, setAceitouPolitica] = useState(false);
    const [pagando, setPagando] = useState(false);
    const [copiado, setCopiado] = useState(false);
    const [pix, setPix] = useState(null);
    const [confirmacaoExtra, setConfirmacaoExtra] = useState(null);
    useEffect(() => {
        api.get('/barbeiros')
            .then(r => {
                const lista = r.data || [];
                setBarbeiros(lista);
                const escolhido = repetir ? lista.find(b => b.id === repetir.barbeiroId) : null;
                if (escolhido) {
                    setBarbeiro(escolhido);
                    setServico((escolhido.servicos || []).find(s => s.id === repetir.servicoId) || null);
                }
            })
            .catch(() => setErroBarbeiros('Não foi possível carregar os barbeiros.'))
            .finally(() => setCarregando(false));
    }, [repetir]);

    useEffect(() => {
        if (!barbeiro || !data || agendamentoCriado) return;
        let ativo = true;
        setHorario('');
        setHorarios([]);
        setHorariosCarregados(false);
        api.get('/agendamentos/horarios-livres', {params: {barbeiroId: barbeiro.id, data}})
            .then(r => {
                if (ativo) {
                    const livres = (r.data || []).map(hora).filter(h => horarioFuturo(data, h));
                    setHorarios(livres);
                    setHorariosCarregados(true);
                    // veio do chat com dia e horário escolhidos: já deixa selecionado (só na primeira vez)
                    if (horarioPedido.current && livres.includes(horarioPedido.current)) setHorario(horarioPedido.current);
                    horarioPedido.current = null;
                }
            })
            .catch(() => {
                if (ativo) setHorarios([]);
            });
        return () => {
            ativo = false;
        };
    }, [barbeiro, data, agendamentoCriado]);

    function escolherBarbeiro(b) {
        setBarbeiro(b);
        setServico(null);
        setHorario('');
        setErro('');
        setSucesso('');
    }

    async function confirmar() {
        if (enviando) return;
        if (confirmacaoExtra) return;
        if (!barbeiro || !servico || !horario || !horarioFuturo(data, horario)) {
            setErro('Escolha um horário futuro válido.');
            return;
        }
        setEnviando(true);
        setErro('');
        try {
            const resposta = await api.get('/agendamentos/meus');
            const existentes = (resposta.data || []).filter(a =>
                ['PENDENTE', 'CONFIRMADO'].includes(a.status) && horarioFuturo(a.data, a.horario)
            );
            if (existentes.length) {
                setConfirmacaoExtra(existentes);
                return;
            }
            await criarAgendamento();
        } catch (e) {
            setErro(erroApi(e, 'Não foi possível verificar seus agendamentos. Tente novamente.'));
        } finally {
            setEnviando(false);
        }
    }

    async function confirmarNovoAgendamento() {
        if (enviando) return;
        setConfirmacaoExtra(null);
        setEnviando(true);
        try {
            await criarAgendamento();
        } finally {
            setEnviando(false);
        }
    }

    async function criarAgendamento() {
        if (!barbeiro || !servico || !horario || !horarioFuturo(data, horario)) {
            setErro('Escolha um horário futuro válido.');
            return;
        }
        setErro('');
        setSucesso('');
        try {
            const r = await api.post('/agendamentos', {
                barbeiroId: barbeiro.id,
                servicoId: servico.id,
                data,
                horario
            });
            if (!r.data?.id) throw new Error('Agendamento criado sem identificador.');
            setAgendamentoCriado(r.data);
            setHorarios(lista => lista.filter(h => h !== horario));
            try {
                const p = await api.get(`/pagamentos/agendamentos/${r.data.id}`);
                setPagamento(p.data);
                setMetodo(p.data.metodo || 'PRESENCIAL');
            } catch {
                setErro('Agendamento criado, mas não foi possível consultar o pagamento. Você pode tentar novamente abaixo.');
            }
        } catch (e) {
            setErro(erroApi(e, 'Não foi possível agendar. Tente outro horário.'));
        }
    }

    async function abrirPagamento(reserva) {
        setErro('');
        setSucesso('');
        setAgendamentoCriado(reserva);
        try {
            const p = await api.get(`/pagamentos/agendamentos/${reserva.id}`);
            setPagamento(p.data);
            setMetodo(p.data.metodo || 'PRESENCIAL');
        } catch {
            setErro('Não foi possível consultar o pagamento. Você pode tentar novamente abaixo.');
        }
    }

    async function consultarPagamento() {
        if (!agendamentoCriado) return;
        setErro('');
        try {
            const r = await api.get(`/pagamentos/agendamentos/${agendamentoCriado.id}`);
            setPagamento(r.data);
            setMetodo(r.data.metodo || 'PRESENCIAL');
        } catch (e) {
            setErro(erroApi(e, 'Não foi possível consultar o pagamento.'));
        }
    }

    async function salvarMetodo() {
        if (!agendamentoCriado) return;
        setPagando(true);
        setErro('');
        setSucesso('');
        try {
            const r = await api.put(`/pagamentos/agendamentos/${agendamentoCriado.id}/metodo`, {metodo});
            setPagamento(r.data);
            if (metodo === 'PIX') {
                const cobranca = await api.get(`/pagamentos/pix/agendamento/${agendamentoCriado.id}`);
                setPix(cobranca.data);
                const atual = await api.get(`/pagamentos/agendamentos/${agendamentoCriado.id}`);
                setPagamento(atual.data);
            } else {
                setPix(null);
                setSucesso('Pagamento no atendimento selecionado. O barbeiro confirmará o recebimento.');
            }
        } catch (e) {
            setErro(erroApi(e, 'Não foi possível salvar a forma de pagamento.'));
        } finally {
            setPagando(false);
        }
    }

    function copiarPix() {
        navigator.clipboard.writeText(pix.pixCopiaECola)
            .then(() => {
                setCopiado(true);
                setTimeout(() => setCopiado(false), 2000);
            })
            .catch(() => setCopiado(false));
    }

    useEffect(() => {
        if (!agendamentoCriado || !pagamento || pagamento.metodo !== 'PIX' || pagamento.status === 'PAGO') return;
        const atualizar = async () => {
            try {
                const r = await api.get(`/pagamentos/agendamentos/${agendamentoCriado.id}`);
                setPagamento(r.data);
                if (r.data.status === 'PAGO') {
                    setPix(null);
                    setSucesso('');
                }
            } catch {
            }
        };
        const timer = setInterval(atualizar, 5000);
        return () => clearInterval(timer);
    }, [agendamentoCriado, pagamento?.metodo, pagamento?.status]);

    const servicos = barbeiro?.servicos || [];
    const pronto = barbeiro && servico && data && horario && horarioFuturo(data, horario);

    return <>
        <Navbar/>
        <main className="page">
            <header className="page__head">
                <Title>{agendamentoCriado ? 'Pagamento do agendamento' : 'Agende seu horário'}</Title>
                <p className="muted">{agendamentoCriado ? 'Escolha como deseja pagar seu atendimento.' : 'Escolha o barbeiro, o serviço, o dia e o horário.'}</p>
            </header>
            {!agendamentoCriado ? <>
                <section className="panel">
                    <h2>1. Barbeiro</h2>
                    {carregando && <p className="muted">Carregando...</p>}
                    {erroBarbeiros && <p className="alert alert--erro">{erroBarbeiros}</p>}
                    {!carregando && !erroBarbeiros && barbeiros.length === 0 &&
                        <p className="muted">Nenhum barbeiro disponível no momento.</p>}
                    <div className="escolhas">{barbeiros.map(b =>
                        <button key={b.id} type="button"
                                className={`escolha ${barbeiro?.id === b.id ? 'escolha--on' : ''}`}
                                onClick={() => escolherBarbeiro(b)}>
                            {b.fotoUrl ? <img className="avatar avatar--foto" src={b.fotoUrl} alt=""/> :
                                <span className="avatar">{b.nome.charAt(0).toUpperCase()}</span>}
                            <span className="lista__info"><strong>{b.nome}</strong><span
                                className="muted small">{b.servicos?.length ?? 0} serviços</span><NotaBarbeiro
                                barbeiroId={b.id}/></span>
                        </button>
                    )}</div>
                </section>
                {barbeiro && <section className="panel">
                    <h2>2. Serviço</h2>
                    {servicos.length === 0 &&
                        <p className="muted">Este barbeiro ainda não tem serviços cadastrados.</p>}
                    <div className="chips">{servicos.map(s =>
                        <button key={s.id} type="button" className={`chip ${servico?.id === s.id ? 'chip--on' : ''}`}
                                onClick={() => setServico(s)}>{s.nome} · {moeda(s.preco)}</button>
                    )}</div>
                </section>}
                {barbeiro && servico && <section className="panel">
                    <h2>3. Dia e horário</h2>
                    <label className="field" style={{maxWidth: 220}}><span>Dia</span><input type="date" value={data}
                                                                                            min={dataHoje()}
                                                                                            onChange={e => {
                                                                                                setData(e.target.value);
                                                                                                setSucesso('');
                                                                                            }}/></label>
                    <h3>Horários de {barbeiro.nome}</h3>
                    {horarios.length === 0 && <p className="muted">Sem horários livres neste dia.</p>}
                    <div className="chips">{horarios.map(h =>
                        <button key={h} type="button" className={`chip ${horario === h ? 'chip--on' : ''}`}
                                onClick={() => setHorario(h)}>{h}</button>
                    )}</div>
                </section>}
                {barbeiro && servico && horariosCarregados && horarios.length === 0 &&
                    <FilaEspera key={`${barbeiro.id}-${servico.id}-${data}`} barbeiro={barbeiro} servico={servico} data={data}
                                aoPagar={abrirPagamento}/>}
                {pronto && <section className="panel resumo"><h2>Confirmar</h2>
                    <p>
                        <strong>{servico.nome}</strong> com <strong>{barbeiro.nome}</strong><br/>{dataFormatada(data)} às {horario} · {moeda(servico.preco)}
                    </p>
                    <label style={{display: 'flex', gap: 10, alignItems: 'flex-start', margin: '4px 0 14px', fontSize: 14, lineHeight: 1.5}}>
                        <input type="checkbox" checked={aceitouPolitica} style={{marginTop: 4}}
                               onChange={e => setAceitouPolitica(e.target.checked)}/>
                        <span>Li e concordo com a <Link to="/reembolso" target="_blank">Política de Cancelamento e
                            Reembolso</Link>: cancelamento com 2 horas ou mais de antecedência tem reembolso do Pix;
                            com menos de 2 horas, não; limite de 3 cancelamentos por mês.</span>
                    </label>
                    <button className="btn btn--primary" onClick={confirmar}
                            disabled={enviando || !aceitouPolitica}>{enviando ? 'Agendando...' : 'Confirmar agendamento'}</button>
                </section>}
            </> : <section className="panel pag">
                <style>{ESTILOS_PAGAMENTO}</style>
                <div className="pag-resumo">
                    <div>
                        <small className="muted">Agendamento #{agendamentoCriado.id}</small>
                        <h2>{agendamentoCriado.servicoNome ?? servico?.nome}</h2>
                        <p className="muted">com {agendamentoCriado.barbeiroNome ?? barbeiro?.nome} · {dataFormatada(agendamentoCriado.data ?? data)} às {hora(agendamentoCriado.horario ?? horario)}</p>
                    </div>
                    <strong className="pag-valor">{moeda(pagamento?.valor ?? servico?.preco)}</strong>
                </div>

                {pagamento && <span className={`pag-status pag-status--${String(pagamento.status).toLowerCase()}`}>
                    {rotuloStatus(pagamento.status)}</span>}

                {!pagamento &&
                    <button className="btn btn--ghost" onClick={consultarPagamento}>Tentar consultar pagamento</button>}

                {pagamento?.status === 'PAGO' && <div className="pag-ok">
                    <span className="pag-ok__icone">✓</span>
                    <h3>Pagamento aprovado!</h3>
                    <p className="muted">Seu horário está confirmado. Enviamos a confirmação para o seu e-mail.</p>
                </div>}

                {pagamento?.status === 'PENDENTE' && <>
                    <h3>Como você prefere pagar?</h3>
                    <div className="pag-opcoes">
                        <button type="button" className={`pag-opcao ${metodo === 'PRESENCIAL' ? 'pag-opcao--on' : ''}`}
                                onClick={() => setMetodo('PRESENCIAL')}>
                            <strong>Pagar no atendimento</strong>
                            <span>Pague direto ao barbeiro no dia.</span>
                        </button>
                        <button type="button" className={`pag-opcao ${metodo === 'PIX' ? 'pag-opcao--on' : ''}`}
                                onClick={() => setMetodo('PIX')}>
                            <strong>Pagar agora por Pix</strong>
                            <span>Aprovação automática, sem enviar comprovante.</span>
                        </button>
                    </div>
                    <button className="btn btn--primary btn--block" disabled={pagando}
                            onClick={salvarMetodo}>{pagando ? 'Aguarde...' : metodo === 'PIX' ? 'Gerar QR Code Pix' : 'Confirmar pagamento no atendimento'}</button>
                </>}

                {pagamento?.metodo === 'PIX' && pagamento.status !== 'PAGO' && pix && <div className="barber-pix-box">
                    <img src={`data:image/png;base64,${pix.qrCodeBase64}`} alt="QR Code Pix"/>
                    <ol className="pag-passos">
                        <li>Abra o app do seu banco e escolha Pix.</li>
                        <li>Leia o QR Code ou use o Pix Copia e Cola.</li>
                        <li>Confirme o pagamento. Esta tela atualiza sozinha.</li>
                    </ol>
                    <textarea readOnly value={pix.pixCopiaECola} rows={3} aria-label="Pix Copia e Cola"/>
                    <button className="btn btn--ghost" type="button" onClick={copiarPix}>
                        {copiado ? 'Código copiado!' : 'Copiar código Pix'}
                    </button>
                    <p className="pag-aguardando"><span className="pag-ponto"/>Aguardando o pagamento...</p>
                </div>}

                <div className="pag-acoes">
                    <Link to="/home" className={`btn ${pagamento?.status === 'PAGO' ? 'btn--primary' : 'btn--ghost'}`}>
                        Voltar ao início</Link>
                    <Link to="/histAgendamento" className="btn btn--ghost">Ver meus agendamentos</Link>
                </div>
            </section>}
            {confirmacaoExtra && !agendamentoCriado && <div role="presentation" style={{
                position: 'fixed',
                inset: 0,
                zIndex: 9999,
                background: 'rgba(0,0,0,.78)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                padding: 20
            }}>
                <section role="dialog" aria-modal="true" aria-labelledby="titulo-confirmacao-extra" className="panel"
                         style={{width: '100%', maxWidth: 500, boxShadow: '0 20px 70px rgba(0,0,0,.5)'}}>
                    <h2 id="titulo-confirmacao-extra">Você já possui agendamento</h2>
                    <p>Você já
                        tem {confirmacaoExtra.length === 1 ? 'um horário marcado' : `${confirmacaoExtra.length} horários marcados`}:</p>
                    {confirmacaoExtra.map(a => <p key={a.id} className="muted">
                        <strong>{a.servicoNome}</strong> com {a.barbeiroNome},
                        dia {dataFormatada(a.data)} às {hora(a.horario)}</p>)}
                    <p><strong>Deseja realmente marcar outro agendamento?</strong> O agendamento anterior será mantido.
                    </p>
                    <div style={{display: 'flex', gap: 12, flexWrap: 'wrap', marginTop: 18}}>
                        <button type="button" className="btn btn--ghost" onClick={() => setConfirmacaoExtra(null)}
                                disabled={enviando}>Cancelar
                        </button>
                        <button type="button" className="btn btn--primary" onClick={confirmarNovoAgendamento}
                                disabled={enviando}>{enviando ? 'Agendando...' : 'Confirmar novo agendamento'}</button>
                    </div>
                </section>
            </div>}
            {erro && <p className="alert alert--erro">{erro}</p>}
            {sucesso && <p className="alert alert--ok">{sucesso}</p>}
        </main>
    </>;
}
