import {useEffect, useState} from 'react';
import {AlertTriangle, CheckCircle2} from 'lucide-react';
import {useNavigate} from 'react-router-dom';
import api from '../../api/api.js';
import {hojeISO, somarDias} from '../../pages/Admin/util.js';
import './AdminExtras.css';

/** Configurações: central de pendências + regras que o sistema aplica hoje. */
export default function CentralPendencias({barbeiros, servicos, agendamentos, notas, aoGerenciarBarbeiro, irPara}) {
    const navegar = useNavigate();
    const [chamados, setChamados] = useState(null);
    const [bloqueados, setBloqueados] = useState(null);

    useEffect(() => {
        let ativo = true;
        api.get('/chamados').then(r => ativo && setChamados(r.data || [])).catch(() => ativo && setChamados([]));
        api.get('/admin/clientes/bloqueados').then(r => ativo && setBloqueados(r.data || [])).catch(() => ativo && setBloqueados([]));
        return () => {
            ativo = false;
        };
    }, []);

    const hoje = hojeISO();
    const agora = new Date();
    const agoraTxt = `${hoje}T${String(agora.getHours()).padStart(2, '0')}:${String(agora.getMinutes()).padStart(2, '0')}`;
    const ha30 = somarDias(hoje, -30);

    const chamadosAbertos = (chamados || []).filter(c => ['ABERTO', 'EM_ANDAMENTO'].includes(c.status));
    const reembolsos = chamadosAbertos.filter(c => c.reembolsavel);
    const semBaixa = agendamentos.filter(a => ['PENDENTE', 'CONFIRMADO'].includes(a.status)
        && `${a.data}T${String(a.horario).slice(0, 5)}` < agoraTxt);
    const servicosSemBarbeiro = servicos.filter(s => !barbeiros.some(b => (b.servicos || []).some(x => x.id === s.id)));

    const problemasBarbeiro = barbeiros.flatMap(b => {
        const lista = [];
        if (!(b.servicos || []).length) lista.push('Não tem nenhum serviço vinculado: ninguém consegue agendar com ele.');
        if (!(b.horarios || []).some(h => String(h).slice(0, 16) >= agoraTxt)) lista.push('Sem horários de trabalho à frente: a agenda dele está fechada.');
        const faltas = agendamentos.filter(a => a.barbeiroNome === b.nome && a.status === 'NAO_COMPARECEU' && a.data >= ha30).length;
        if (faltas >= 3) lista.push(`${faltas} clientes faltaram nos últimos 30 dias: vale revisar a agenda dele.`);
        const nota = notas[b.id];
        const media = nota?.media ?? nota?.notaMedia;
        const qtd = nota?.quantidade ?? nota?.totalAvaliacoes;
        if (media != null && qtd >= 3 && media < 4) lista.push(`Nota média ${Number(media).toFixed(1)} em ${qtd} avaliações.`);
        return lista.map(texto => ({barbeiro: b, texto}));
    });

    const gerais = [
        reembolsos.length > 0 && {
            id: 'reembolsos', titulo: `${reembolsos.length} chamado(s) pedindo análise de reembolso`,
            detalhe: 'Só o administrador pode aprovar reembolso fora da regra.', acao: 'Abrir chamados', ir: () => navegar('/chamados')
        },
        chamadosAbertos.length > reembolsos.length && {
            id: 'chamados', titulo: `${chamadosAbertos.length - reembolsos.length} chamado(s) em aberto`,
            detalhe: 'Clientes e barbeiros aguardando resposta.', acao: 'Abrir chamados', ir: () => navegar('/chamados')
        },
        (bloqueados || []).length > 0 && {
            id: 'bloqueados', titulo: `${bloqueados.length} cliente(s) bloqueado(s) por cancelamentos`,
            detalhe: 'Você pode liberar manualmente.', acao: 'Ver clientes', ir: () => irPara('clientes')
        },
        semBaixa.length > 0 && {
            id: 'baixa', titulo: `${semBaixa.length} atendimento(s) passado(s) sem baixa`,
            detalhe: 'Ainda marcados como pendentes/confirmados: o barbeiro precisa dar baixa (concluído ou faltou).',
            acao: 'Ver agenda', ir: () => irPara('horarios')
        },
        servicosSemBarbeiro.length > 0 && {
            id: 'servicos', titulo: `${servicosSemBarbeiro.length} serviço(s) sem barbeiro`,
            detalhe: servicosSemBarbeiro.map(s => s.nome).join(', '), acao: 'Vincular', ir: () => irPara('servicos')
        }
    ].filter(Boolean);

    const carregando = chamados === null || bloqueados === null;

    return <div className="adm-x-stack">
        <section className="adm-x-card">
            <div className="adm-x-head">
                <div>
                    <h3>Central de pendências</h3>
                    <p className="adm-x-sub">Tudo que precisa da sua atenção, em um lugar só.</p>
                </div>
            </div>
            {carregando ? <p className="adm-x-sub">Verificando...</p>
                : gerais.length === 0
                    ? <div className="adm-x-ok"><CheckCircle2 size={20}/> Nada pendente por aqui. Tudo em dia!</div>
                    : <div className="adm-x-lista">
                        {gerais.map(p => <div className="adm-x-item" key={p.id}>
                            <AlertTriangle size={18} color="var(--adm-gold, #d6a44f)"/>
                            <div className="adm-x-item-txt"><strong>{p.titulo}</strong><small>{p.detalhe}</small></div>
                            <button type="button" className="adm-x-btn adm-x-btn--sm" onClick={p.ir}>{p.acao}</button>
                        </div>)}
                    </div>}
        </section>

        <section className="adm-x-card">
            <div className="adm-x-head">
                <div>
                    <h3>Problemas dos barbeiros</h3>
                    <p className="adm-x-sub">Detectados automaticamente com base em serviços, horários, faltas e notas.</p>
                </div>
            </div>
            {problemasBarbeiro.length === 0
                ? <div className="adm-x-ok"><CheckCircle2 size={20}/> Nenhum problema encontrado na equipe.</div>
                : <div className="adm-x-lista">
                    {problemasBarbeiro.map((p, i) => <div className="adm-x-item" key={`${p.barbeiro.id}-${i}`}>
                        <div className="adm-x-item-txt"><strong>{p.barbeiro.nome}</strong><small>{p.texto}</small></div>
                        <button type="button" className="adm-x-btn adm-x-btn--sm"
                                onClick={() => aoGerenciarBarbeiro(p.barbeiro)}>Resolver</button>
                    </div>)}
                </div>}
        </section>

        <section className="adm-x-card">
            <h3>Regras em vigor</h3>
            <p className="adm-x-sub">Valem para todos os clientes e estão definidas no sistema. Para alterar, fale com o desenvolvedor.</p>
            <div className="adm-x-regras">
                <div className="adm-x-regra"><b>2 horas</b><span>antecedência mínima para cancelar com reembolso do Pix</span></div>
                <div className="adm-x-regra"><b>3 por mês</b><span>cancelamentos permitidos por cliente</span></div>
                <div className="adm-x-regra"><b>7 dias</b><span>bloqueio ao estourar o limite de cancelamentos</span></div>
                <div className="adm-x-regra"><b>20%</b><span>fica com a barbearia; 80% é repassado ao barbeiro</span></div>
            </div>
            <div className="adm-x-atalhos">
                <button type="button" className="adm-x-btn" onClick={() => irPara('barbeiros')}>Equipe</button>
                <button type="button" className="adm-x-btn" onClick={() => irPara('servicos')}>Serviços e preços</button>
                <button type="button" className="adm-x-btn" onClick={() => irPara('financeiro')}>Financeiro</button>
                <button type="button" className="adm-x-btn" onClick={() => navegar('/chamados')}>Chamados</button>
            </div>
        </section>
    </div>;
}
