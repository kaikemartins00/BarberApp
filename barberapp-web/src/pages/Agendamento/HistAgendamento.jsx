import FichaCliente from '../../components/FichaCliente.jsx';
import {lerSessao} from '../../auth/session.js';
import {useEffect, useState} from 'react';
import {Link} from 'react-router-dom';
import api from '../../api/api.js';
import Navbar from '../../components/Navbar.jsx';
import {Title} from '../LoginECadastro/Style.js';
import FormAvaliacao from '../../components/FormAvaliacao.jsx';
import ConfirmDialog from '../../components/ConfirmDialog.jsx';

const formatarData = iso => String(iso || '').split('-').reverse().join('/');
const formatarDataHora = iso => String(iso || '').slice(0, 10).split('-').reverse().join('/');

function HistAgendamento() {
    const [agendamentos, setAgendamentos] = useState([]);
    const [avaliados, setAvaliados] = useState({});
    const [carregando, setCarregando] = useState(true);
    const [erro, setErro] = useState('');
    const [resumo, setResumo] = useState(null);
    const [alvo, setAlvo] = useState(null);
    const [cancelando, setCancelando] = useState(false);
    const [aceitou, setAceitou] = useState(false);

    useEffect(() => {

        api.get('/agendamentos/meus').then(r =>
            setAgendamentos([...r.data].sort((a, b) =>
                `${b.data} ${b.horario}`.localeCompare(`${a.data} ${a.horario}`))))
            .catch(() => setErro('Não foi possível carregar seus agendamentos.'))
            .finally(() => setCarregando(false));

    }, []);

    useEffect(() => {
        let ativo = true;
        api.get('/agendamentos/meus/cancelamentos')
            .then(r => {
                if (ativo) setResumo(r.data);
            })
            .catch(() => {
                if (ativo) setResumo(null);
            });
        return () => {
            ativo = false;
        };
    }, []);

    function pedirCancelamento(a) {
        const inicio = new Date(`${a.data}T${String(a.horario).slice(0, 5)}:00`).getTime();
        const limite = new Date(inicio - 2 * 60 * 60 * 1000);
        const doisDigitos = n => String(n).padStart(2, '0');
        setAceitou(false);
        setAlvo({
            ...a,
            semReembolso: inicio - new Date().getTime() < 2 * 60 * 60 * 1000,
            limiteReembolso: `${doisDigitos(limite.getDate())}/${doisDigitos(limite.getMonth() + 1)}/${limite.getFullYear()} às ${doisDigitos(limite.getHours())}:${doisDigitos(limite.getMinutes())}`
        });
    }

    async function cancelar() {
        if (!alvo) return;
        setCancelando(true);
        setErro('');
        try {
            const r = await api.put(`/agendamentos/${alvo.id}/status`, null, {params: {status: 'CANCELADO'}});
            setAgendamentos(lista => lista.map(a => a.id === alvo.id ? r.data : a));
            const novo = await api.get('/agendamentos/meus/cancelamentos');
            setResumo(novo.data);
        } catch (e) {
            setErro(e.response?.data?.message || 'Não foi possível cancelar.');
        } finally {
            setCancelando(false);
            setAlvo(null);
        }
    }

    const limiteAtingido = resumo ? resumo.usados >= resumo.limite : false;
    const ultimoCancelamento = resumo ? resumo.usados + 1 >= resumo.limite : false;

    return <><Navbar/>
        <main className="page">
            <header className="page__head"><Title>Meus agendamentos</Title><p className="muted">Acompanhe seus horários
                e avalie os atendimentos concluídos.</p></header>
            {erro && <p className="alert alert--erro">{erro}</p>}
            {resumo?.bloqueadoAte && <p className="alert alert--erro">Você não pode fazer novos agendamentos
                até {formatarDataHora(resumo.bloqueadoAte)} por excesso de cancelamentos neste mês.</p>}
            {resumo && !resumo.bloqueadoAte && resumo.usados > 0 && <p className="muted small">Cancelamentos
                neste mês: {resumo.usados} de {resumo.limite}.</p>}
            {carregando && <p className="muted">Carregando...</p>}
            {!carregando && !agendamentos.length &&
                <section className="panel"><p className="muted">Você ainda não tem agendamentos.</p><Link
                    className="btn btn--primary" to="/agendamento">Agendar agora</Link></section>}
            {agendamentos.length > 0 && <section className="panel">
                <ul>{agendamentos.map(a => <li key={a.id} className="historico-item">
                    <div className="lista__item">
                        <div className="avatar">{formatarData(a.data).slice(0, 2)}</div>
                        <div className="lista__info"><strong>{a.servicoNome} com {a.barbeiroNome}</strong><span
                            className="muted small">{formatarData(a.data)} às {String(a.horario).slice(0, 5)}</span>
                        </div>
                        <span className="badge">{a.status}</span>{['PENDENTE', 'CONFIRMADO'].includes(a.status) &&
                        <button className="btn btn--ghost" onClick={() => pedirCancelamento(a)}>Cancelar</button>}</div>
                    {a.status === 'CONCLUIDO' && !avaliados[a.id] && <FormAvaliacao agendamentoId={a.id}
                                                                                    onAvaliada={() => setAvaliados(old => ({
                                                                                        ...old,
                                                                                        [a.id]: true
                                                                                    }))}/>}
                    {avaliados[a.id] && <p className="alert alert--ok">Avaliação enviada. Obrigado!</p>}
                </li>)}</ul>
            </section>}
            {lerSessao()?.userId && <FichaCliente clienteId={lerSessao().userId}/>}
        </main>
        <ConfirmDialog aberto={!!alvo} titulo="Cancelar agendamento?" confirmar="Cancelar agendamento"
                       cancelar="Manter horário" perigo carregando={cancelando}
                       desabilitarConfirmar={limiteAtingido || !aceitou}
                       aoConfirmar={cancelar} aoFechar={() => setAlvo(null)}>
            {alvo && <p><strong>{alvo.servicoNome}</strong> com {alvo.barbeiroNome}<br/>
                {formatarData(alvo.data)} às {String(alvo.horario).slice(0, 5)}</p>}
            {resumo && <p>Você já cancelou <strong>{resumo.usados} de {resumo.limite}</strong> horários neste mês.</p>}
            {limiteAtingido && <p className="dlg-aviso">Você atingiu o limite de cancelamentos deste mês.
                Entre em contato com a barbearia.</p>}
            {!limiteAtingido && ultimoCancelamento && <p className="dlg-aviso">Este é o seu último cancelamento
                do mês. Depois dele, você ficará {resumo.diasBloqueio} dias sem poder fazer novos agendamentos.</p>}
            {alvo && !alvo.semReembolso && <p>Se você já pagou por Pix, o reembolso é solicitado
                automaticamente assim que você cancelar, e o valor volta para a conta que fez o pagamento. O prazo
                para cancelar com reembolso vai até <strong>{alvo.limiteReembolso}</strong> (2 horas antes do
                horário).</p>}
            {alvo?.semReembolso && <p className="dlg-aviso">O prazo para cancelar com reembolso terminou
                em {alvo.limiteReembolso} (2 horas antes do horário). Se você já pagou por Pix, o valor não será
                devolvido.</p>}
            <p>Teve um imprevisto grave (acidente, emergência)? Cancele e peça uma análise em
                {' '}<Link to="/problemas" target="_blank">Problemas</Link>.</p>
            <label style={{display: 'flex', gap: 10, alignItems: 'flex-start', fontSize: 14, lineHeight: 1.5}}>
                <input type="checkbox" checked={aceitou} style={{marginTop: 4}}
                       onChange={e => setAceitou(e.target.checked)}/>
                <span>Li e concordo com a <Link to="/reembolso" target="_blank">Política de Cancelamento e
                    Reembolso</Link>.</span>
            </label>
        </ConfirmDialog>
    </>;
}

export default HistAgendamento;
