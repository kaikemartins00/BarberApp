import {useMemo} from 'react';
import {CalendarDays} from 'lucide-react';
import './GraficoAtendimentos.css';

const nomesMeses = ['janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho', 'julho', 'agosto', 'setembro', 'outubro', 'novembro', 'dezembro'];

export default function GraficoAtendimentos({agendamentos = []}) {
    const dados = useMemo(() => {
        const hoje = new Date();
        const ano = hoje.getFullYear();
        const mes = hoje.getMonth();
        const anterior = new Date(ano, mes - 1, 1);
        const dias = new Date(ano, mes + 1, 0).getDate();
        const diasAnterior = new Date(anterior.getFullYear(), anterior.getMonth() + 1, 0).getDate();
        const atual = Array(dias).fill(0);
        const passado = Array(dias).fill(0);

        agendamentos.forEach(a => {
            if (a.status !== 'CONCLUIDO' || !/^\d{4}-\d{2}-\d{2}$/.test(a.data || '')) return;
            const [aAno, aMes, aDia] = a.data.split('-').map(Number);
            if (aAno === ano && aMes === mes + 1) atual[aDia - 1]++;
            if (aAno === anterior.getFullYear() && aMes === anterior.getMonth() + 1 && aDia <= dias) passado[aDia - 1]++;
        });

        const totalAtual = atual.reduce((s, v) => s + v, 0);
        const totalAnterior = passado.reduce((s, v) => s + v, 0);
        const maximo = Math.max(1, ...atual, ...passado);
        const largura = 1000;
        const altura = 120;
        const esquerda = 40;
        const direita = 12;
        const topo = 8;
        const baixo = 20;
        const areaLargura = largura - esquerda - direita;
        const areaAltura = altura - topo - baixo;
        const x = i => esquerda + i * areaLargura / Math.max(1, dias - 1);
        const y = v => topo + (1 - v / maximo) * areaAltura;
        const caminho = valores => valores.map((v, i) => `${i ? 'L' : 'M'}${x(i).toFixed(2)},${y(v).toFixed(2)}`).join(' ');
        const variacao = totalAnterior ? ((totalAtual - totalAnterior) / totalAnterior) * 100 : null;
        return {
            ano,
            mes,
            anterior,
            dias,
            diasAnterior,
            atual,
            passado,
            totalAtual,
            totalAnterior,
            maximo,
            largura,
            altura,
            esquerda,
            topo,
            baixo,
            areaLargura,
            areaAltura,
            x,
            y,
            caminho,
            variacao
        };
    }, [agendamentos]);

    return (
        <section className="ga-card">
            <div className="ga-topo">
                <div>
                    <h2><CalendarDays size={16} className="ga-icone"/> Atendimentos diários</h2>
                    <p>{nomesMeses[dados.mes]} comparado a {nomesMeses[dados.anterior.getMonth()]} · só concluídos</p>
                </div>
                <div className="ga-resumo">
                    <div><span>Mês atual</span><strong>{dados.totalAtual}</strong></div>
                    <div><span>Mês anterior</span><strong>{dados.totalAnterior}</strong></div>
                    <div><span>Variação</span><strong
                        className={dados.variacao != null && dados.variacao < 0 ? 'ga-negativo' : 'ga-positivo'}>{dados.variacao == null ? '—' : `${dados.variacao > 0 ? '+' : ''}${dados.variacao.toFixed(1)}%`}</strong>
                    </div>
                </div>
                <div className="ga-legenda"><span><i className="ga-cor-atual"/>Atual</span><span><i
                    className="ga-cor-anterior"/>Anterior</span></div>
            </div>
            <div className="ga-grafico">
                <svg viewBox={`0 0 ${dados.largura} ${dados.altura}`} role="img"
                     aria-label="Gráfico de atendimentos concluídos por dia nos dois meses">
                    {[0, 1, 2].map(i => {
                        const valor = dados.maximo * i / 2;
                        const y = dados.y(valor);
                        return <g key={i}>
                            <line x1={dados.esquerda} y1={y} x2={dados.largura - 12} y2={y} stroke="currentColor"
                                  opacity=".13"/>
                            <text x={dados.esquerda - 10} y={y + 4} fill="currentColor" opacity=".6" fontSize="12"
                                  textAnchor="end">{Number.isInteger(valor) ? valor : valor.toFixed(1)}</text>
                        </g>;
                    })}
                    <path d={dados.caminho(dados.passado)} fill="none" stroke="#8b91a4" strokeWidth="2.5"
                          strokeLinejoin="round" strokeLinecap="round" strokeDasharray="6 5"/>
                    <path d={dados.caminho(dados.atual)} fill="none" stroke="#d6a447" strokeWidth="3"
                          strokeLinejoin="round" strokeLinecap="round"/>
                    {dados.atual.map((valor, i) => valor > 0 ?
                        <circle key={i} cx={dados.x(i)} cy={dados.y(valor)} r="3" fill="#d6a447">
                            <title>{`${i + 1}/${dados.mes + 1}: ${valor} atendimentos`}</title></circle> : null)}
                    {Array.from({length: dados.dias}, (_, i) => i).filter(i => i === 0 || (i + 1) % 5 === 0 || i === dados.dias - 1).map(i =>
                        <text key={i} x={dados.x(i)} y={dados.altura - 4} fill="currentColor" opacity=".7" fontSize="12"
                              textAnchor="middle">{i + 1}</text>)}
                </svg>
            </div>
        </section>
    );
}
