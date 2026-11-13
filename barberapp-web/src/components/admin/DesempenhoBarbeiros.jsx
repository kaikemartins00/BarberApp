import {useState} from 'react';
import {CalendarClock, ChevronDown, ChevronUp, Star} from 'lucide-react';
import {dataBR, ehConcluido, hojeISO, moeda, somarDias, valorAg} from '../../pages/Admin/util.js';
import './AdminExtras.css';

const PERIODOS = [
    {id: '7d', titulo: '7 dias'},
    {id: '30d', titulo: '30 dias'},
    {id: 'mes', titulo: 'Este mês'},
    {id: 'tudo', titulo: 'Tudo'}
];

const inicioDoPeriodo = (periodo, hoje) => {
    if (periodo === '7d') return somarDias(hoje, -6);
    if (periodo === '30d') return somarDias(hoje, -29);
    if (periodo === 'mes') return `${hoje.slice(0, 7)}-01`;
    return '0000-00-00';
};

const pct = (parte, total) => (total ? Math.round((parte / total) * 100) : 0);

/** Desempenho de cada barbeiro (tudo calculado a partir dos agendamentos já carregados). */
export default function DesempenhoBarbeiros({barbeiros, agendamentos, notas, aoGerenciar}) {
    const [periodo, setPeriodo] = useState('30d');
    const [aberto, setAberto] = useState(null);

    const hoje = hojeISO();
    const inicio = inicioDoPeriodo(periodo, hoje);
    const agora = new Date();
    const agoraTxt = `${hoje}T${String(agora.getHours()).padStart(2, '0')}:${String(agora.getMinutes()).padStart(2, '0')}`;

    const linhas = barbeiros.map(b => {
        const todos = agendamentos.filter(a => a.barbeiroNome === b.nome);
        const doPeriodo = todos.filter(a => a.data >= inicio && a.data <= hoje);
        const concl = doPeriodo.filter(ehConcluido);
        const faltas = doPeriodo.filter(a => a.status === 'NAO_COMPARECEU').length;
        const cancelados = doPeriodo.filter(a => a.status === 'CANCELADO').length;
        const atendidos = concl.length + faltas;
        const receita = concl.reduce((s, a) => s + valorAg(a), 0);
        const dias = new Set(concl.map(a => a.data)).size;
        const proximos = todos.filter(a => ['PENDENTE', 'CONFIRMADO'].includes(a.status)
            && `${a.data}T${String(a.horario).slice(0, 5)}` >= agoraTxt).length;
        const horariosAbertos = (b.horarios || []).filter(h => String(h).slice(0, 16) >= agoraTxt).length;
        const ultimos = [...todos].sort((x, y) =>
            `${y.data}${y.horario}`.localeCompare(`${x.data}${x.horario}`)).slice(0, 8);
        const dias14 = Array.from({length: 14}, (_, i) => {
            const dia = somarDias(hoje, i - 13);
            return {dia, qtd: todos.filter(a => a.data === dia && ehConcluido(a)).length};
        });
        const nota = notas[b.id];
        return {
            b, concl: concl.length, faltas, cancelados, receita, dias, proximos, horariosAbertos, ultimos, dias14,
            presenca: pct(concl.length, atendidos),
            ticket: concl.length ? receita / concl.length : 0,
            media: nota?.media ?? nota?.notaMedia,
            avaliacoes: nota?.quantidade ?? nota?.totalAvaliacoes
        };
    }).sort((x, y) => y.concl - x.concl);

    const maximo = Math.max(1, ...linhas.map(l => l.concl));
    const totalAtend = linhas.reduce((s, l) => s + l.concl, 0);
    const totalReceita = linhas.reduce((s, l) => s + l.receita, 0);
    const melhorNota = [...linhas].filter(l => l.media != null && l.avaliacoes > 0)
        .sort((x, y) => y.media - x.media)[0];

    return <div className="adm-x-stack">
        <section className="adm-x-card">
            <div className="adm-x-head">
                <div>
                    <h3>Desempenho da equipe</h3>
                    <p className="adm-x-sub">Atendimentos concluídos, presença dos clientes, dias trabalhados e avaliações.</p>
                </div>
                <div className="adm-x-tabs" role="tablist">
                    {PERIODOS.map(p => <button key={p.id} type="button"
                                               className={`adm-x-tab ${periodo === p.id ? 'adm-x-tab--on' : ''}`}
                                               onClick={() => setPeriodo(p.id)}>{p.titulo}</button>)}
                </div>
            </div>

            <div className="adm-x-kpis">
                <div className="adm-x-kpi"><span>Atendimentos concluídos</span><strong>{totalAtend}</strong>
                    <small>toda a equipe</small></div>
                <div className="adm-x-kpi"><span>Valor em serviços</span><strong>{moeda(totalReceita)}</strong>
                    <small>pelos preços do catálogo</small></div>
                <div className="adm-x-kpi"><span>Mais atendimentos</span>
                    <strong>{linhas[0]?.concl ? linhas[0].b.nome : '—'}</strong>
                    <small>{linhas[0]?.concl ? `${linhas[0].concl} no período` : 'sem atendimentos'}</small></div>
                <div className="adm-x-kpi"><span>Melhor avaliação</span>
                    <strong>{melhorNota ? melhorNota.b.nome : '—'}</strong>
                    <small>{melhorNota ? `${Number(melhorNota.media).toFixed(1)} (${melhorNota.avaliacoes} avaliações)` : 'sem avaliações'}</small></div>
            </div>
        </section>

        {barbeiros.length === 0 ? <div className="adm-x-card"><p className="adm-x-sub">Nenhum barbeiro na equipe.</p></div>
            : <div className="adm-x-equipe">
                {linhas.map(l => <article key={l.b.id} className={`adm-x-barbeiro ${aberto === l.b.id ? 'adm-x-barbeiro--on' : ''}`}>
                    <div className="adm-x-pessoa">
                        {l.b.fotoUrl
                            ? <img className="adm-x-foto" src={l.b.fotoUrl} alt={l.b.nome}/>
                            : <span className="adm-x-foto">{l.b.nome?.charAt(0).toUpperCase()}</span>}
                        <div>
                            <strong>{l.b.nome}</strong>
                            <span className="adm-x-sub">{l.proximos} agendados à frente</span>
                        </div>
                        <span className="adm-x-nota" title="Nota média">
                            <Star size={15} fill="currentColor"/>{l.media == null || !l.avaliacoes ? '—' : Number(l.media).toFixed(1)}
                        </span>
                    </div>

                    <div>
                        <div className="adm-x-rotulo"><span>Atendimentos</span><b>{l.concl}</b></div>
                        <div className="adm-x-barra"><i style={{width: `${pct(l.concl, maximo)}%`}}/></div>
                    </div>

                    <div className="adm-x-numeros">
                        <div><b>{l.dias}</b><span>dias trabalhados</span></div>
                        <div><b>{l.presenca}%</b><span>presença dos clientes</span></div>
                        <div><b>{l.faltas}</b><span>faltas</span></div>
                        <div><b>{l.cancelados}</b><span>cancelados</span></div>
                        <div><b>{moeda(l.ticket)}</b><span>ticket médio</span></div>
                        <div><b>{l.horariosAbertos}</b><span>horários abertos</span></div>
                    </div>

                    <div className="adm-x-acoes">
                        <button type="button" className="adm-x-btn adm-x-btn--sm"
                                onClick={() => setAberto(aberto === l.b.id ? null : l.b.id)}>
                            {aberto === l.b.id ? <ChevronUp size={14}/> : <ChevronDown size={14}/>} Detalhes
                        </button>
                        <button type="button" className="adm-x-btn adm-x-btn--sm" onClick={() => aoGerenciar(l.b)}>
                            <CalendarClock size={14}/> Gerenciar horários
                        </button>
                    </div>

                    {aberto === l.b.id && <div className="adm-x-stack">
                        <div>
                            <div className="adm-x-rotulo"><span>Atendimentos nos últimos 14 dias</span></div>
                            <div className="adm-x-spark" aria-hidden="true">
                                {l.dias14.map(d => <i key={d.dia} className={d.qtd ? '' : 'vazio'}
                                                      title={`${dataBR(d.dia)}: ${d.qtd}`}
                                                      style={{height: `${d.qtd ? Math.max(12, (d.qtd / Math.max(1, ...l.dias14.map(x => x.qtd))) * 100) : 6}%`}}/>)}
                            </div>
                        </div>
                        <div className="adm-x-tabela-wrap">
                            <table className="adm-x-tabela">
                                <thead>
                                <tr><th>Data</th><th>Cliente</th><th>Serviço</th><th>Status</th></tr>
                                </thead>
                                <tbody>
                                {l.ultimos.length === 0 && <tr><td colSpan="4">Nenhum atendimento ainda.</td></tr>}
                                {l.ultimos.map(a => <tr key={a.id}>
                                    <td>{dataBR(a.data)} {String(a.horario).slice(0, 5)}</td>
                                    <td>{a.clienteNome}</td>
                                    <td>{a.servicoNome}</td>
                                    <td>{a.status}</td>
                                </tr>)}
                                </tbody>
                            </table>
                        </div>
                    </div>}
                </article>)}
            </div>}
    </div>;
}
