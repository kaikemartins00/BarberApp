import {useCallback, useEffect, useState} from 'react';
import {Link, useSearchParams} from 'react-router-dom';
import {LayoutDashboard, CalendarDays, Scissors, Wallet, Star, RefreshCw, Users} from 'lucide-react';
import api from '../../api/api.js';
import {lerSessao} from '../../auth/session.js';
import '../Admin/Admin.css';
import GraficoAtendimentos from '../../components/dashboard/GraficoAtendimentos.jsx';
import SidebarProfissional from '../../components/layout/SidebarProfissional.jsx';

const moeda = v => Number(v ?? 0).toLocaleString('pt-BR', {style: 'currency', currency: 'BRL'});
const dataLocal = () => {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
};
const dataHora = v => {
    const [dia, hora] = String(v).split('T');
    if (!hora) return v;
    const [a, m, d] = dia.split('-');
    return `${d}/${m}/${a} ${hora.slice(0, 5)}`;
};
const mensagemErro = (e, fallback) => e?.response?.data?.message || fallback;

export default function PainelBarbeiro() {
    const sessao = lerSessao();
    const [searchParams, setSearchParams] = useSearchParams();
    const aba = searchParams.get('secao') || 'dashboard';
    const setAba = id => setSearchParams(id === 'dashboard' ? {} : {secao: id});
    const [barbeiro, setBarbeiro] = useState(null);
    const [agendamentos, setAgendamentos] = useState([]);
    const [financeiro, setFinanceiro] = useState(null);
    const [pagamentos, setPagamentos] = useState({});
    const [pagamentosErro, setPagamentosErro] = useState('');
    const [avaliacoes, setAvaliacoes] = useState(null);
    const [data, setData] = useState(dataLocal());
    const [erro, setErro] = useState('');
    const [aviso, setAviso] = useState('');
    const [financeiroErro, setFinanceiroErro] = useState('');
    const [avaliacoesErro, setAvaliacoesErro] = useState('');
    const [ocupacao, setOcupacao] = useState(null);
    const [ocupacaoErro, setOcupacaoErro] = useState('');
    const [carregando, setCarregando] = useState(true);
    const [processando, setProcessando] = useState(null);

    const carregar = useCallback(async () => {
        if (!sessao?.userId) return;
        setCarregando(true);
        setErro('');
        const [b, a, f, n] = await Promise.allSettled([
            api.get(`/barbeiros/${sessao.userId}`),
            api.get('/agendamentos'),
            api.get('/financeiro/barbeiro/meu'),
            api.get(`/avaliacoes/barbeiros/${sessao.userId}/resumo`)
        ]);
        if (b.status === 'fulfilled') setBarbeiro(b.value.data);
        if (a.status === 'fulfilled') {
            const lista = a.value.data || [];
            setAgendamentos(lista);
            const resultados = await Promise.allSettled(lista.map(item => api.get(`/pagamentos/agendamentos/${item.id}`)));
            const mapa = {};
            resultados.forEach((resultado, indice) => {
                if (resultado.status === 'fulfilled') mapa[lista[indice].id] = resultado.value.data;
            });
            setPagamentos(mapa);
            setPagamentosErro(resultados.some(resultado => resultado.status === 'rejected') ? 'Alguns pagamentos não puderam ser consultados.' : '');
        }
        if (b.status === 'rejected' || a.status === 'rejected') setErro('Não foi possível carregar os dados do barbeiro ou sua agenda.');
        setFinanceiro(f.status === 'fulfilled' ? f.value.data : null);
        setFinanceiroErro(f.status === 'rejected' ? 'Financeiro indisponível.' : '');
        setAvaliacoes(n.status === 'fulfilled' ? n.value.data : null);
        setAvaliacoesErro(n.status === 'rejected' ? 'Avaliações indisponíveis.' : '');
        setCarregando(false);
    }, [sessao?.userId]);

    useEffect(() => {
        carregar();
    }, [carregar]);
    useEffect(() => {
        if (!sessao?.userId) return;
        let ativo = true;
        setOcupacaoErro('');
        api.get(`/ocupacao/barbeiros/${sessao.userId}`, {params: {data}})
            .then(r => {
                if (ativo) setOcupacao(r.data);
            })
            .catch(() => {
                if (ativo) {
                    setOcupacao(null);
                    setOcupacaoErro('Ocupação indisponível.');
                }
            });
        return () => {
            ativo = false;
        };
    }, [sessao?.userId, data]);

    async function registrar(id, acao) {
        const pagamentoPix = pagamentos[id];
        const reembolsa = acao === 'nao-compareceu' && pagamentoPix?.status === 'PAGO' && pagamentoPix?.metodo === 'PIX';
        const pergunta = acao === 'concluir'
            ? 'Confirmar atendimento concluído?'
            : reembolsa
                ? `Confirmar que o cliente não compareceu? O Pix de ${moeda(pagamentoPix.valor)} será reembolsado automaticamente ao cliente.`
                : 'Confirmar que o cliente não compareceu?';
        if (!window.confirm(pergunta)) return;
        setProcessando(id);
        setErro('');
        setAviso('');
        try {
            await api.patch(`/agendamentos/${id}/${acao}`);
            setAviso(reembolsa ? 'Atendimento atualizado. O Pix foi reembolsado ao cliente.' : 'Atendimento atualizado.');
            await carregar();
        } catch (e) {
            setErro(mensagemErro(e, 'Não foi possível atualizar o atendimento.'));
        } finally {
            setProcessando(null);
        }
    }

    async function confirmarPagamento(id) {
        if (!window.confirm('Você recebeu o valor deste atendimento e deseja confirmar o pagamento?')) return;
        setProcessando(id);
        setErro('');
        setAviso('');
        try {
            const r = await api.post(`/pagamentos/agendamentos/${id}/confirmar`);
            setPagamentos(atual => ({ ...atual, [id]: r.data }));
            setAviso('Pagamento confirmado. O faturamento será atualizado.');
            await carregar();
        } catch (e) {
            setErro(mensagemErro(e, 'Não foi possível confirmar o pagamento.'));
        } finally {
            setProcessando(null);
        }
    }

    const pagamentoDo = id => pagamentos[id];
    const jaComecou = a => new Date(`${a.data}T${String(a.horario).slice(0, 5)}`) <= new Date();
// Pix aguardando conferência: pode confirmar já. No atendimento: só depois que o horário chegar.
    const confirmarPermitido = a => {
        const p = pagamentoDo(a.id);
        if (!p || a.status === 'CANCELADO' || a.status === 'NAO_COMPARECEU') return false;
        if (p.status === 'PAGO' || p.status === 'CANCELADO') return false;
        if (p.metodo === 'PIX') return p.status === 'AGUARDANDO_CONFIRMACAO';
        return p.metodo === 'PRESENCIAL' && jaComecou(a);
    };
    const ROTULO_METODO = {PIX: 'Pix', PRESENCIAL: 'No atendimento'};
    const ROTULO_PAGAMENTO = {PAGO: 'Pago', PENDENTE: 'Pendente', AGUARDANDO_CONFIRMACAO: 'Aguardando conferência', CANCELADO: 'Cancelado'};

    const hoje = dataLocal();
    const doDia = agendamentos.filter(a => a.data === data);
    const deHoje = agendamentos.filter(a => a.data === hoje && a.status !== 'CANCELADO');
    const concluidos = deHoje.filter(a => a.status === 'CONCLUIDO').length;
    const faltas = deHoje.filter(a => a.status === 'NAO_COMPARECEU').length;
    const servicos = barbeiro?.servicos || [];
    const horarios = barbeiro?.horarios || [];
    const menu = [
        ['dashboard', 'Visão geral', LayoutDashboard],
        ['agenda', 'Minha agenda', CalendarDays],
        ['clientes', 'Meus clientes', Users],
        ['servicos', 'Serviços e horários', Scissors],
        ['financeiro', 'Meu financeiro', Wallet],
        ['avaliacoes', 'Avaliações', Star]
    ];
    const clientes = [...new Map(agendamentos.filter(a => a.clienteNome).map(a => [a.clienteNome, a.clienteNome])).values()];
    const kpi = (titulo, valor, subtitulo, Icone) => <article className="adm-kpi">
        <div className="adm-kpi-top"><span>{titulo}</span><Icone size={19}/></div>
        <strong>{valor}</strong><small>{subtitulo}</small></article>;
    const ordemAgenda = (a, b) => (a.status === 'CANCELADO') - (b.status === 'CANCELADO')
        || String(a.horario).localeCompare(String(b.horario));
    const listaAgenda = lista => lista.length ? <div className="adm-list">{[...lista].sort(ordemAgenda).map(a => {
        const cancelado = a.status === 'CANCELADO';
        const pg = pagamentoDo(a.id);
        return <div className="adm-agenda-item" key={a.id}
                    style={cancelado ? {opacity: .5, filter: 'grayscale(1)'} : undefined}>
            <div className="adm-time" style={cancelado ? {textDecoration: 'line-through'} : undefined}>{String(a.horario).slice(0, 5)}</div>
            <div style={{flex: 1}}>
                <strong>{a.clienteNome}</strong><small>{a.servicoNome} · {moeda(a.servicoPreco)}</small></div>
            <div style={{display: 'flex', flexDirection: 'column', gap: 5, alignItems: 'flex-end'}}>
                <span className="adm-status">{cancelado ? '✕ Cancelado' : a.status}</span>
                {cancelado
                    ? <small>Horário cancelado · nada a receber</small>
                    : <small style={{display: 'flex', gap: 6, alignItems: 'center'}}>
                        {pg ? <>
                            <span className="adm-status">{ROTULO_METODO[pg.metodo] || pg.metodo}</span>
                            <span>{ROTULO_PAGAMENTO[pg.status] || pg.status} · {moeda(pg.valor)}</span>
                        </> : 'Pagamento indisponível'}
                    </small>}
                {confirmarPermitido(a) && <button type="button" className="adm-btn adm-btn--gold"
                                                  disabled={processando === a.id} onClick={() => confirmarPagamento(a.id)}>
                    Confirmar recebimento
                </button>}
            </div>{['PENDENTE', 'CONFIRMADO'].includes(a.status) &&
            <div style={{display: 'flex', gap: 6, flexWrap: 'wrap'}}>
                <button className="adm-btn adm-btn--gold" disabled={processando === a.id}
                        onClick={() => registrar(a.id, 'concluir')}>Concluir
                </button>
                <button className="adm-btn adm-btn--outline" disabled={processando === a.id}
                        onClick={() => registrar(a.id, 'nao-compareceu')}>Não compareceu
                </button>
            </div>}</div>;
    })}</div> : <div className="adm-vazio">Nenhum atendimento nesta data.</div>;

    return <SidebarProfissional>
        <>

            <div className="adm-page-top">
                <div><span className="adm-eyebrow">BARBERAPP / BARBEIRO</span>
                    <h1>{menu.find(x => x[0] === aba)?.[1]}</h1><p>Gerencie seus atendimentos e acompanhe seus
                        resultados.</p></div>
                <button className="adm-btn adm-btn--outline" onClick={carregar} disabled={carregando}><RefreshCw
                    size={16}/> Atualizar
                </button>
            </div>
            {erro && <div className="adm-aviso adm-aviso--erro">{erro}</div>}{aviso &&
            <div className="adm-aviso adm-aviso--ok">{aviso}</div>}
            {aba === 'dashboard' && <div className="adm-stack">
                <div
                    className="adm-kpi-grid">{kpi('Agendamentos hoje', deHoje.length, 'Seus atendimentos', CalendarDays)}{kpi('Concluídos hoje', concluidos, 'Atendimentos realizados', Scissors)}{kpi('Meu faturamento', financeiro ? moeda(financeiro.totalLiquido) : '—', '80% dos pagamentos confirmados', Wallet)}{kpi('Não compareceram', faltas, 'Faltas de hoje', Users)}</div>
                <div className="adm-columns">
                    <section className="adm-card">
                        <div className="adm-card-head">
                            <div><h3>Agenda de hoje</h3><p>Atendimentos sob sua responsabilidade</p></div>
                            <button className="adm-text-button" onClick={() => {
                                setData(hoje);
                                setAba('agenda');
                            }}>Ver agenda
                            </button>
                        </div>
                        {listaAgenda(deHoje)}</section>
                    <section className="adm-card"><h3>Resumo profissional</h3>
                        <div className="adm-summary-line">
                            <span>Serviços disponíveis</span><strong>{servicos.length}</strong></div>
                        <div className="adm-summary-line">
                            <span>Horários cadastrados</span><strong>{horarios.length}</strong></div>
                        <div className="adm-summary-line">
                            <span>Minha avaliação</span><strong>{avaliacoes?.media ?? avaliacoes?.notaMedia ?? '—'}</strong>
                        </div>
                        {financeiroErro && <p className="adm-muted">{financeiroErro}</p>}</section>
                </div>
                <GraficoAtendimentos agendamentos={agendamentos}/></div>}
            {aba === 'agenda' && <section className="adm-card">
                <div className="adm-card-head">
                    <div><h3>Minha agenda</h3><p>Conclua atendimentos ou registre ausências</p></div>
                    <input className="adm-date" type="date" value={data} onChange={e => setData(e.target.value)}/></div>
                {listaAgenda(doDia)}</section>}
            {aba === 'clientes' &&
                <section className="adm-card"><h3>Clientes dos meus atendimentos</h3><p className="adm-muted">Somente
                    clientes que constam em sua agenda.</p>{clientes.length ? clientes.map(nome => <div
                    className="adm-summary-line" key={nome}><strong>{nome}</strong><Link className="adm-text-button"
                                                                                         to={`/mensagens?cliente=${encodeURIComponent(nome)}`}>Conversar</Link>
                </div>) : <div className="adm-vazio">Nenhum cliente vinculado.</div>}</section>}
            {aba === 'servicos' && <div className="adm-stack">
                <section className="adm-card"><h3>Meus serviços</h3>
                    <div className="adm-chips">{servicos.map(s => <span className="adm-horario"
                                                                        key={s.id}>{s.nome} · {moeda(s.preco)}</span>)}</div>
                    {!servicos.length && <p className="adm-muted">Nenhum serviço atribuído.</p>}</section>
                <section className="adm-card"><h3>Meus horários</h3>
                    <div className="adm-chips">{[...horarios].sort().map(h => <span className="adm-horario"
                                                                                    key={h}>{dataHora(h)}</span>)}</div>
                    {!horarios.length && <p className="adm-muted">Nenhum horário cadastrado.</p>}</section>
            </div>}
            {aba === 'financeiro' && <div className="adm-stack">
                <div
                    className="adm-kpi-grid adm-kpi-grid--three">{kpi('Bruto dos meus atendimentos', financeiro ? moeda(financeiro.totalBruto) : '—', 'Pagamentos confirmados', Wallet)}{kpi('Taxa da barbearia', financeiro ? moeda(financeiro.taxaLoja) : '—', '20% dos seus atendimentos', Wallet)}{kpi('Meu valor líquido', financeiro ? moeda(financeiro.totalLiquido) : '—', '80% dos seus atendimentos', Wallet)}</div>
                {financeiroErro && <div className="adm-aviso adm-aviso--erro">{financeiroErro}</div>}{pagamentosErro && <p className="adm-muted">{pagamentosErro}</p>}<section className="adm-card"><h3>Recebimentos dos meus atendimentos</h3>{listaAgenda(agendamentos.filter(a => a.status !== 'CANCELADO'))}</section></div>}
            {aba === 'avaliacoes' && <section className="adm-card"><h3>Minhas avaliações</h3>{avaliacoesErro ?
                <p className="adm-muted">{avaliacoesErro}</p> : <>
                    <div className="adm-summary-line">
                        <span>Nota média</span><strong>{avaliacoes?.media ?? avaliacoes?.notaMedia ?? '—'}</strong>
                    </div>
                    <div className="adm-summary-line">
                        <span>Quantidade de avaliações</span><strong>{avaliacoes?.quantidade ?? avaliacoes?.totalAvaliacoes ?? '—'}</strong>
                    </div>
                </>}</section>}
        </>
    </SidebarProfissional>;
}