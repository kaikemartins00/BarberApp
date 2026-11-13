import {useEffect, useState} from 'react';
import api from '../api/api.js';

const hora = valor => String(valor).slice(0, 5);
const dataFormatada = iso => iso.split('-').reverse().join('/');
const erroApi = (e, padrao) => e.response?.data?.message || padrao;
const doisDigitos = n => String(n).padStart(2, '0');
const diaSeguinte = iso => {
    const d = new Date(`${iso}T12:00:00`);
    d.setDate(d.getDate() + 1);
    return `${d.getFullYear()}-${doisDigitos(d.getMonth() + 1)}-${doisDigitos(d.getDate())}`;
};
const separar = dh => {
    if (Array.isArray(dh)) {
        const [a, m, d, h = 0, mi = 0] = dh;
        return {dia: `${a}-${doisDigitos(m)}-${doisDigitos(d)}`, hora: `${doisDigitos(h)}:${doisDigitos(mi)}`};
    }
    const texto = String(dh);
    return {dia: texto.slice(0, 10), hora: texto.slice(11, 16)};
};

export default function FilaEspera({barbeiro, servico, data, aoPagar}) {
    const [aberto, setAberto] = useState(false);
    const [reservaData, setReservaData] = useState('');
    const [reservaHora, setReservaHora] = useState('');
    const [livres, setLivres] = useState([]);
    const [qualquer, setQualquer] = useState(true);
    const [escolhidos, setEscolhidos] = useState([]);
    const [enviando, setEnviando] = useState(false);
    const [erro, setErro] = useState('');
    const [inscricao, setInscricao] = useState(null);
    const [reserva, setReserva] = useState(null);
    const [agora] = useState(() => Date.now());
    const [marcadoNoDia, setMarcadoNoDia] = useState(null);

    useEffect(() => {
        let ativo = true;
        api.get('/agendamentos/meus')
            .then(r => {
                const achado = (r.data || []).find(a => a.data === data && ['PENDENTE', 'CONFIRMADO'].includes(a.status));
                if (ativo) setMarcadoNoDia(achado || null);
            })
            .catch(() => {
            });
        return () => {
            ativo = false;
        };
    }, [data]);

    useEffect(() => {
        if (!reservaData) return;
        let ativo = true;
        api.get('/agendamentos/horarios-livres', {params: {barbeiroId: barbeiro.id, data: reservaData}})
            .then(r => {
                if (ativo) setLivres((r.data || []).map(hora));
            })
            .catch(() => {
                if (ativo) setLivres([]);
            });
        return () => {
            ativo = false;
        };
    }, [reservaData, barbeiro.id]);

    const ocupados = (barbeiro.horarios || [])
        .map(separar)
        .filter(h => h.dia === data && new Date(`${h.dia}T${h.hora}:00`).getTime() > agora)
        .map(h => h.hora)
        .sort();

    if (inscricao) {
        return <section className="panel">
            <h2>Fila de espera</h2>
            {inscricao.status === 'AGUARDANDO' ? <>
                <p className="alert alert--ok">Você está na fila para o dia {dataFormatada(data)}.</p>
                <p>Se alguém cancelar um desses horários
                    ({[...inscricao.horariosAceitos].map(hora).sort().join(', ')}), movemos você
                    automaticamente e avisamos por e-mail.</p>
                {reserva && <p className="muted">Enquanto isso, seu horário de reserva está agendado
                    para <strong>{dataFormatada(reserva.data)} às {hora(reserva.horario)}</strong>. Se você for
                    movido para uma vaga mais cedo, o pagamento acompanha o agendamento.</p>}
                <div style={{display: 'flex', gap: 12, flexWrap: 'wrap', marginTop: 8}}>
                    {reserva && aoPagar && <button className="btn btn--primary" onClick={() => aoPagar(reserva)}>
                        Pagar agora por Pix</button>}
                    <button className="btn btn--ghost" disabled={enviando} onClick={sair}>
                        {enviando ? 'Saindo...' : 'Sair da fila'}</button>
                </div>
            </> : <p className="muted">Você saiu da fila. Seu horário de reserva continua agendado.</p>}
            {erro && <p className="alert alert--erro">{erro}</p>}
        </section>;
    }

    if (marcadoNoDia) {
        return <section className="panel">
            <h2>Fila de espera</h2>
            <p className="muted">Você já tem um horário marcado em {dataFormatada(data)} às {hora(marcadoNoDia.horario)}.
                Não é possível entrar na fila de um dia em que você já está agendado. Se quiser outro horário,
                cancele o atual em "Meus agendamentos".</p>
        </section>;
    }

    if (ocupados.length === 0) {
        return <section className="panel">
            <h2>Fila de espera</h2>
            <p className="muted">{barbeiro.nome} não atende neste dia. Escolha outra data.</p>
        </section>;
    }

    function alternar(h) {
        setEscolhidos(lista => lista.includes(h) ? lista.filter(x => x !== h) : [...lista, h]);
    }

    function escolherDia(valor) {
        setReservaData(valor);
        setReservaHora('');
        setLivres([]);
    }

    async function entrar() {
        const aceitos = qualquer ? ocupados : escolhidos;
        if (!reservaData || !reservaHora) {
            setErro('Escolha o dia e o horário em que você quer ser atendido caso ninguém cancele.');
            return;
        }
        if (aceitos.length === 0) {
            setErro('Marque pelo menos um horário que você aceita.');
            return;
        }
        setEnviando(true);
        setErro('');
        let criado = null;
        try {
            const r = await api.post('/agendamentos', {
                barbeiroId: barbeiro.id,
                servicoId: servico.id,
                data: reservaData,
                horario: reservaHora
            });
            criado = r.data;
            setReserva(criado);
            const f = await api.post('/fila-espera', {
                agendamentoAlternativoId: criado.id,
                dataPreferida: data,
                horariosAceitos: aceitos.map(h => `${h}:00`)
            });
            setInscricao(f.data);
        } catch (e) {
            setErro(criado
                ? `Seu horário de reserva foi agendado (${dataFormatada(criado.data)} às ${hora(criado.horario)}), mas não foi possível entrar na fila: ${erroApi(e, 'tente novamente em "Meus horários".')}`
                : erroApi(e, 'Não foi possível reservar o horário e entrar na fila.'));
        } finally {
            setEnviando(false);
        }
    }

    async function sair() {
        setEnviando(true);
        setErro('');
        try {
            const r = await api.delete(`/fila-espera/${inscricao.id}`);
            setInscricao(r.data);
        } catch (e) {
            setErro(erroApi(e, 'Não foi possível sair da fila.'));
        } finally {
            setEnviando(false);
        }
    }

    return <section className="panel">
        <h2>Fila de espera</h2>
        <p>Todos os horários de <strong>{dataFormatada(data)}</strong> com {barbeiro.nome} estão ocupados.
            Entre na fila: se alguém cancelar, você é movido para a vaga automaticamente e recebe um aviso por
            e-mail.</p>

        {!aberto ? <button className="btn btn--primary" onClick={() => setAberto(true)}>Entrar na fila de
            espera</button> : <>
            <h3>1. Se ninguém cancelar, quando você quer ser atendido?</h3>
            <p className="muted">Esse horário fica agendado para você. Se uma vaga abrir em {dataFormatada(data)},
                ele é cancelado e você é puxado para a vaga.</p>
            <label className="field" style={{maxWidth: 220}}><span>Dia desejado</span>
                <input type="date" value={reservaData} min={diaSeguinte(data)}
                       onChange={e => escolherDia(e.target.value)}/></label>
            {reservaData && livres.length === 0 && <p className="muted">Sem horários livres neste dia.</p>}
            <div className="chips">{livres.map(h =>
                <button key={h} type="button" className={`chip ${reservaHora === h ? 'chip--on' : ''}`}
                        onClick={() => setReservaHora(h)}>{h}</button>
            )}</div>

            <h3>2. Quais horários de {dataFormatada(data)} você aceita?</h3>
            <div className="chips">
                <button type="button" className={`chip ${qualquer ? 'chip--on' : ''}`}
                        onClick={() => setQualquer(true)}>Qualquer horário que abrir
                </button>
                <button type="button" className={`chip ${!qualquer ? 'chip--on' : ''}`}
                        onClick={() => setQualquer(false)}>Escolher horários
                </button>
            </div>
            {!qualquer && <div className="chips" style={{marginTop: 8}}>{ocupados.map(h =>
                <button key={h} type="button" className={`chip ${escolhidos.includes(h) ? 'chip--on' : ''}`}
                        onClick={() => alternar(h)}>{h}</button>
            )}</div>}

            <button className="btn btn--primary" style={{marginTop: 14}} disabled={enviando} onClick={entrar}>
                {enviando ? 'Enviando...' : 'Reservar horário e entrar na fila'}</button>
        </>}
        {erro && <p className="alert alert--erro">{erro}</p>}
    </section>;
}
