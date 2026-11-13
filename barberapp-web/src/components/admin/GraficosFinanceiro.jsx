import {useState} from 'react';
import {dataBR, deISO, ehConcluido, hojeISO, moeda, paraISO, somarDias, textoVariacao, valorAg, variacao} from '../../pages/Admin/util.js';
import './AdminExtras.css';

const MODOS = [
    {id: 'mes', titulo: 'Mês × mês anterior'},
    {id: 'dia', titulo: 'Dia × dia'},
    {id: '30d', titulo: 'Últimos 30 dias'}
];

const COMPARAR = [
    {id: 'mes', titulo: 'mesmo dia do mês passado'},
    {id: 'semana', titulo: 'mesmo dia da semana passada'},
    {id: 'ontem', titulo: 'ontem'}
];

const SEMANA = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb'];

const somar = lista => lista.reduce((s, a) => s + valorAg(a), 0);

const chaveMes = (ano, mes0) => `${ano}-${String(mes0 + 1).padStart(2, '0')}`;

function Variacao({atual, anterior}) {
    const v = variacao(atual, anterior);
    const classe = v == null ? '' : v >= 0 ? 'adm-x-var--up' : 'adm-x-var--down';
    return <span className={`adm-x-var ${classe}`}>{textoVariacao(v)}</span>;
}

function GraficoLinhas({atual, anterior, dias, ate}) {
    const L = 640, A = 230, ml = 52, mr = 14, mt = 14, mb = 28;
    const maximo = Math.max(1, ...atual.filter(v => v != null), ...anterior);
    const x = i => ml + (dias <= 1 ? 0 : (i / (dias - 1)) * (L - ml - mr));
    const y = v => mt + (1 - v / maximo) * (A - mt - mb);
    const caminho = serie => serie.map((v, i) => (v == null ? '' : `${i === 0 || serie[i - 1] == null ? 'M' : 'L'}${x(i).toFixed(1)},${y(v).toFixed(1)}`)).join(' ');
    const marcas = [1, Math.ceil(dias / 4), Math.ceil(dias / 2), Math.ceil((dias * 3) / 4), dias];
    return <svg className="adm-x-grafico" viewBox={`0 0 ${L} ${A}`} role="img"
                aria-label="Faturamento acumulado do mês atual comparado ao mês anterior">
        {[0, .5, 1].map(f => <g key={f}>
            <line className="adm-x-eixo" x1={ml} x2={L - mr} y1={y(maximo * f)} y2={y(maximo * f)}/>
            <text className="adm-x-texto-svg" x={ml - 6} y={y(maximo * f) + 4} textAnchor="end">{Math.round(maximo * f)}</text>
        </g>)}
        {marcas.map(d => <text key={d} className="adm-x-texto-svg" x={x(d - 1)} y={A - 8} textAnchor="middle">{d}</text>)}
        <path className="adm-x-linha-ant" d={caminho(anterior)}/>
        <path className="adm-x-linha-atual" d={caminho(atual)}/>
        {atual[ate - 1] != null && <circle cx={x(ate - 1)} cy={y(atual[ate - 1])} r="4.5" fill="var(--adm-gold, #d6a44f)"/>}
    </svg>;
}

function Ranking({itens, vazio}) {
    const max = Math.max(1, ...itens.map(i => i.valor));
    if (!itens.length) return <p className="adm-x-sub">{vazio}</p>;
    return <div className="adm-x-ranking">
        {itens.map(i => <div key={i.nome}>
            <div className="adm-x-rotulo"><span>{i.nome} · {i.qtd}x</span><b>{moeda(i.valor)}</b></div>
            <div className="adm-x-barra"><i style={{width: `${(i.valor / max) * 100}%`}}/></div>
        </div>)}
    </div>;
}

const agrupar = (lista, chave) => Object.values(lista.reduce((mapa, a) => {
    const nome = a[chave];
    mapa[nome] = mapa[nome] || {nome, qtd: 0, valor: 0};
    mapa[nome].qtd += 1;
    mapa[nome].valor += valorAg(a);
    return mapa;
}, {})).sort((a, b) => b.valor - a.valor);

/** Comparativos do faturamento. Usa os atendimentos concluídos e o preço do catálogo (estimativa). */
export default function GraficosFinanceiro({agendamentos}) {
    const [modo, setModo] = useState('mes');
    const [comparar, setComparar] = useState('mes');
    const hoje = hojeISO();
    const [diaEscolhido, setDiaEscolhido] = useState(hoje);

    const concluidos = agendamentos.filter(ehConcluido);
    const dHoje = deISO(hoje);

    // ---- mês atual x anterior (acumulado)
    const chaveAtual = chaveMes(dHoje.getFullYear(), dHoje.getMonth());
    const dAnt = new Date(dHoje.getFullYear(), dHoje.getMonth() - 1, 1);
    const chaveAnt = chaveMes(dAnt.getFullYear(), dAnt.getMonth());
    const diasMesAtual = new Date(dHoje.getFullYear(), dHoje.getMonth() + 1, 0).getDate();
    const diasMesAnt = new Date(dAnt.getFullYear(), dAnt.getMonth() + 1, 0).getDate();
    const diasGrafico = Math.max(diasMesAtual, diasMesAnt);

    const porDia = chave => {
        const arr = Array(diasGrafico).fill(0);
        concluidos.filter(a => a.data.startsWith(chave)).forEach(a => {
            arr[Number(a.data.slice(8, 10)) - 1] += valorAg(a);
        });
        return arr;
    };
    const acumular = (arr, ate) => arr.map((_, i) => (i < ate ? arr.slice(0, i + 1).reduce((s, v) => s + v, 0) : null));
    const hojeNum = dHoje.getDate();
    const serieAtual = acumular(porDia(chaveAtual), hojeNum);
    const serieAntFull = acumular(porDia(chaveAnt), diasMesAnt).map((v, i) => (i < diasMesAnt ? v : null));
    const totalAtual = serieAtual[hojeNum - 1] || 0;
    const totalAntMesmoPonto = serieAntFull[Math.min(hojeNum, diasMesAnt) - 1] || 0;
    const totalAntFinal = serieAntFull[diasMesAnt - 1] || 0;
    const listaMesAtual = concluidos.filter(a => a.data.startsWith(chaveAtual));

    // ---- dia x dia
    const refDia = comparar === 'semana' ? somarDias(diaEscolhido, -7)
        : comparar === 'ontem' ? somarDias(diaEscolhido, -1)
            : (() => {
                const d = deISO(diaEscolhido);
                const alvo = new Date(d.getFullYear(), d.getMonth() - 1, 1);
                const ultimo = new Date(alvo.getFullYear(), alvo.getMonth() + 1, 0).getDate();
                alvo.setDate(Math.min(d.getDate(), ultimo));
                return paraISO(alvo);
            })();
    const listaDia = concluidos.filter(a => a.data === diaEscolhido);
    const listaRef = concluidos.filter(a => a.data === refDia);

    // ---- 30 dias
    const ultimos30 = Array.from({length: 30}, (_, i) => {
        const dia = somarDias(hoje, i - 29);
        return {dia, valor: somar(concluidos.filter(a => a.data === dia))};
    });
    const max30 = Math.max(1, ...ultimos30.map(d => d.valor));
    const total30 = ultimos30.reduce((s, d) => s + d.valor, 0);
    const porSemana = SEMANA.map((nome, i) => {
        const dias = ultimos30.filter(d => deISO(d.dia).getDay() === i);
        return {nome, valor: dias.length ? dias.reduce((s, d) => s + d.valor, 0) / dias.length : 0, qtd: dias.length};
    });
    const melhorDia = [...porSemana].sort((a, b) => b.valor - a.valor)[0];

    return <div className="adm-x-stack">
        <section className="adm-x-card">
            <div className="adm-x-head">
                <div>
                    <h3>Comparativos de faturamento</h3>
                    <p className="adm-x-sub">Baseado nos atendimentos concluídos e nos preços do catálogo. Gorjetas e
                        extras lançados na conta não entram aqui.</p>
                </div>
                <div className="adm-x-tabs" role="tablist">
                    {MODOS.map(m => <button key={m.id} type="button"
                                            className={`adm-x-tab ${modo === m.id ? 'adm-x-tab--on' : ''}`}
                                            onClick={() => setModo(m.id)}>{m.titulo}</button>)}
                </div>
            </div>

            {modo === 'mes' && <div className="adm-x-stack">
                <div className="adm-x-duelo">
                    <div className="adm-x-duelo-col adm-x-duelo-col--atual">
                        <span className="adm-x-sub">Este mês (até dia {hojeNum})</span>
                        <strong>{moeda(totalAtual)}</strong>
                        <span className="adm-x-sub">{listaMesAtual.length} atendimentos</span>
                    </div>
                    <div className="adm-x-duelo-col">
                        <span className="adm-x-sub">Mês passado (até dia {Math.min(hojeNum, diasMesAnt)})</span>
                        <strong>{moeda(totalAntMesmoPonto)}</strong>
                        <span className="adm-x-sub">
                            Variação: <Variacao atual={totalAtual} anterior={totalAntMesmoPonto}/> · mês passado
                            fechou em {moeda(totalAntFinal)}
                        </span>
                    </div>
                </div>
                <div>
                    <div className="adm-x-legenda"><span>Este mês</span><span className="ant">Mês passado</span></div>
                    <GraficoLinhas atual={serieAtual} anterior={serieAntFull} dias={diasGrafico} ate={hojeNum}/>
                    <p className="adm-x-sub">Faturamento acumulado por dia do mês (R$).</p>
                </div>
            </div>}

            {modo === 'dia' && <div className="adm-x-stack">
                <div className="adm-x-head" style={{marginBottom: 0}}>
                    <label className="adm-x-field">Dia analisado
                        <input className="adm-x-input" type="date" max={hoje} value={diaEscolhido}
                               onChange={e => e.target.value && setDiaEscolhido(e.target.value)}/>
                    </label>
                    <label className="adm-x-field">Comparar com
                        <select className="adm-x-select" value={comparar} onChange={e => setComparar(e.target.value)}>
                            {COMPARAR.map(c => <option key={c.id} value={c.id}>{c.titulo}</option>)}
                        </select>
                    </label>
                </div>
                <div className="adm-x-duelo">
                    <div className="adm-x-duelo-col adm-x-duelo-col--atual">
                        <span className="adm-x-sub">{diaEscolhido === hoje ? 'Hoje' : 'Dia'} · {dataBR(diaEscolhido)}</span>
                        <strong>{moeda(somar(listaDia))}</strong>
                        <span className="adm-x-sub">{listaDia.length} atendimentos
                            · ticket {moeda(listaDia.length ? somar(listaDia) / listaDia.length : 0)}</span>
                        <div className="adm-x-barra"><i style={{width: `${(somar(listaDia) / Math.max(1, somar(listaDia), somar(listaRef))) * 100}%`}}/></div>
                    </div>
                    <div className="adm-x-duelo-col">
                        <span className="adm-x-sub">{dataBR(refDia)}</span>
                        <strong>{moeda(somar(listaRef))}</strong>
                        <span className="adm-x-sub">{listaRef.length} atendimentos
                            · ticket {moeda(listaRef.length ? somar(listaRef) / listaRef.length : 0)}</span>
                        <div className="adm-x-barra adm-x-barra--cinza"><i style={{width: `${(somar(listaRef) / Math.max(1, somar(listaDia), somar(listaRef))) * 100}%`}}/></div>
                    </div>
                </div>
                <p className="adm-x-sub">Variação: <Variacao atual={somar(listaDia)} anterior={somar(listaRef)}/>
                    {diaEscolhido === hoje && ' (o dia de hoje ainda está em andamento)'}</p>
            </div>}

            {modo === '30d' && <div className="adm-x-stack">
                <div className="adm-x-kpis">
                    <div className="adm-x-kpi"><span>Total em 30 dias</span><strong>{moeda(total30)}</strong>
                        <small>média de {moeda(total30 / 30)} por dia</small></div>
                    <div className="adm-x-kpi"><span>Melhor dia da semana</span>
                        <strong>{melhorDia?.valor ? melhorDia.nome : '—'}</strong>
                        <small>{melhorDia?.valor ? `média de ${moeda(melhorDia.valor)}` : 'sem dados'}</small></div>
                </div>
                <div className="adm-x-barras-dia" aria-hidden="true">
                    {ultimos30.map(d => <i key={d.dia} className={d.valor ? '' : 'zero'} title={`${dataBR(d.dia)}: ${moeda(d.valor)}`}
                                           style={{height: `${d.valor ? Math.max(4, (d.valor / max30) * 100) : 2}%`}}/>)}
                </div>
                <p className="adm-x-sub">Faturamento por dia, de {dataBR(ultimos30[0].dia)} até hoje.</p>
                <div>
                    <div className="adm-x-rotulo"><span>Média por dia da semana</span></div>
                    <div className="adm-x-ranking">
                        {porSemana.map(s => <div key={s.nome}>
                            <div className="adm-x-rotulo"><span>{s.nome}</span><b>{moeda(s.valor)}</b></div>
                            <div className="adm-x-barra"><i style={{width: `${melhorDia?.valor ? (s.valor / melhorDia.valor) * 100 : 0}%`}}/></div>
                        </div>)}
                    </div>
                </div>
            </div>}
        </section>

        <div className="adm-x-colunas">
            <section className="adm-x-card">
                <h3>Por barbeiro</h3>
                <p className="adm-x-sub" style={{marginBottom: 14}}>Mês atual</p>
                <Ranking itens={agrupar(listaMesAtual, 'barbeiroNome')} vazio="Nenhum atendimento concluído neste mês."/>
            </section>
            <section className="adm-x-card">
                <h3>Por serviço</h3>
                <p className="adm-x-sub" style={{marginBottom: 14}}>Mês atual</p>
                <Ranking itens={agrupar(listaMesAtual, 'servicoNome')} vazio="Nenhum atendimento concluído neste mês."/>
            </section>
        </div>
    </div>;
}
