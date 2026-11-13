import { useCallback, useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
    LayoutDashboard, Users, CalendarDays, Wallet, Star,
    Scissors, Plus, Trash2, Settings, RefreshCw,
    ChevronRight, X, AlertTriangle, TrendingUp, UserMinus
} from 'lucide-react';
import api from '../../api/api.js';
import SidebarProfissional from '../../components/layout/SidebarProfissional.jsx';
import ClientesBloqueados from '../../components/ClientesBloqueados.jsx';
import FinanceiroAdmin from '../../components/FinanceiroAdmin.jsx';
import ServicosAdmin from '../../components/admin/ServicosAdmin.jsx';
import DesempenhoBarbeiros from '../../components/admin/DesempenhoBarbeiros.jsx';
import GraficosFinanceiro from '../../components/admin/GraficosFinanceiro.jsx';
import CentralPendencias from '../../components/admin/CentralPendencias.jsx';
import DesligarBarbeiro from '../../components/admin/DesligarBarbeiro.jsx';
import EquipeDesligada from '../../components/admin/EquipeDesligada.jsx';
import './Admin.css';

const FORM_VAZIO = {
    nome: '',
    email: '',
    senha: '',
    telefone: '',
    fotoUrl: ''
};

const SERVICO_VAZIO = {
    nome: '',
    preco: ''
};

const dois = (n) => String(n).padStart(2, '0');

const paraISO = (d) =>
    `${d.getFullYear()}-${dois(d.getMonth() + 1)}-${dois(d.getDate())}`;

const hojeISO = () => paraISO(new Date());

const agoraISO = () => {
    const d = new Date();
    return `${paraISO(d)}T${dois(d.getHours())}:${dois(d.getMinutes())}`;
};

const cortar = (valor) => String(valor).slice(0, 16);

const somarDias = (iso, dias) => {
    const [ano, mes, dia] = iso.split('-').map(Number);
    return paraISO(new Date(ano, mes - 1, dia + dias));
};

const formatar = (valor) => {
    const [data, hora] = cortar(valor).split('T');
    if (!data || !hora) return valor;
    const [ano, mes, dia] = data.split('-');
    return `${dia}/${mes}/${ano} ${hora}`;
};

const moeda = (valor) =>
    Number(valor || 0).toLocaleString('pt-BR', {
        style: 'currency',
        currency: 'BRL'
    });

const mensagemErro = (error, padrao) =>
    error.response?.data?.message || padrao;

const MENU = [
    { id: 'dashboard', titulo: 'Visão geral', Icone: LayoutDashboard },
    { id: 'barbeiros', titulo: 'Barbeiros', Icone: Users },
    { id: 'servicos', titulo: 'Serviços e preços', Icone: Scissors },
    { id: 'horarios', titulo: 'Horários', Icone: CalendarDays },
    { id: 'financeiro', titulo: 'Financeiro', Icone: Wallet },
    { id: 'avaliacoes', titulo: 'Avaliações', Icone: Star },
    { id: 'clientes', titulo: 'Clientes bloqueados', Icone: Users },
    { id: 'configuracoes', titulo: 'Configurações', Icone: Settings }
];

function Card({ titulo, valor, descricao, Icone, destaque }) {
    return (
        <article className={`adm-kpi ${destaque ? 'adm-kpi--gold' : ''}`}>
            <div className="adm-kpi-top">
                <span>{titulo}</span>
                <Icone size={19} />
            </div>
            <strong>{valor}</strong>
            <small>{descricao}</small>
        </article>
    );
}

function Aviso({ tipo, texto }) {
    if (!texto) return null;
    return <div className={`adm-aviso adm-aviso--${tipo}`}>{texto}</div>;
}

function Admin() {
    // a seção fica no endereço (?secao=...), o menu lateral é o mesmo do painel do barbeiro
    const [searchParams, setSearchParams] = useSearchParams();
    const aba = searchParams.get('secao') || 'dashboard';
    const setAba = id => setSearchParams(id === 'dashboard' ? {} : { secao: id });
    const [barbeiros, setBarbeiros] = useState([]);
    const [servicos, setServicos] = useState([]);
    const [agendamentos, setAgendamentos] = useState([]);
    const [form, setForm] = useState(FORM_VAZIO);
    const [erro, setErro] = useState('');
    const [sucesso, setSucesso] = useState('');
    const [carregando, setCarregando] = useState(false);
    const [carregandoDados, setCarregandoDados] = useState(true);
    const [erroDados, setErroDados] = useState('');
    const [editando, setEditando] = useState(null);
    const [servicosEscolhidos, setServicosEscolhidos] = useState([]);
    const [novoServico, setNovoServico] = useState(SERVICO_VAZIO);
    const [horarios, setHorarios] = useState([]);
    const [novaData, setNovaData] = useState(hojeISO());
    const [novoHorario, setNovoHorario] = useState('09:00');
    const [repetir, setRepetir] = useState(1);
    const [msgEdicao, setMsgEdicao] = useState({ tipo: '', texto: '' });
    const [diaCancelar, setDiaCancelar] = useState(hojeISO());
    const [ateDia, setAteDia] = useState('');
    const [motivo, setMotivo] = useState('');
    const [cancelando, setCancelando] = useState(false);
    const [resumoFinanceiro, setResumoFinanceiro] = useState(null);
    const [erroFinanceiro, setErroFinanceiro] = useState('');
    const [notas, setNotas] = useState({});
    const [dataAgenda, setDataAgenda] = useState(hojeISO());
    const [desligando, setDesligando] = useState(null);
    const [versaoDesligados, setVersaoDesligados] = useState(0);

    const carregarDados = useCallback(async () => {
        setCarregandoDados(true);
        setErroDados('');

        const resultados = await Promise.allSettled([
            api.get('/barbeiros'),
            api.get('/servicos'),
            api.get('/agendamentos'),
            api.get('/financeiro/admin/resumo')
        ]);

        if (resultados[0].status === 'fulfilled') {
            setBarbeiros(resultados[0].value.data || []);
        }

        if (resultados[1].status === 'fulfilled') {
            setServicos(resultados[1].value.data || []);
        }

        if (resultados[2].status === 'fulfilled') {
            setAgendamentos(resultados[2].value.data || []);
        }

        if (resultados[3].status === 'fulfilled') {
            setResumoFinanceiro(resultados[3].value.data);
            setErroFinanceiro('');
        } else {
            setErroFinanceiro('Não foi possível carregar o financeiro.');
        }

        if (resultados.slice(0, 3).some(r => r.status === 'rejected')) {
            setErroDados('Alguns dados não puderam ser carregados.');
        }

        setCarregandoDados(false);
    }, []);

    useEffect(() => {
        carregarDados();
    }, [carregarDados]);

    useEffect(() => {
        let ativo = true;

        async function carregarNotas() {
            const resultados = await Promise.allSettled(
                barbeiros.map(b =>
                    api.get(`/avaliacoes/barbeiros/${b.id}/resumo`)
                )
            );

            if (!ativo) return;

            const novasNotas = {};

            resultados.forEach((resultado, indice) => {
                if (resultado.status === 'fulfilled') {
                    novasNotas[barbeiros[indice].id] = resultado.value.data;
                }
            });

            setNotas(novasNotas);
        }

        if (barbeiros.length) carregarNotas();

        return () => {
            ativo = false;
        };
    }, [barbeiros]);

    async function atualizarListas() {
        const resultados = await Promise.allSettled([
            api.get('/barbeiros'),
            api.get('/servicos')
        ]);

        if (resultados[0].status === 'fulfilled') {
            setBarbeiros(resultados[0].value.data || []);
        }

        if (resultados[1].status === 'fulfilled') {
            setServicos(resultados[1].value.data || []);
        }
    }

    async function handleCadastroBarbeiro(event) {
        event.preventDefault();
        setErro('');
        setSucesso('');
        setCarregando(true);

        try {
            await api.post('/auth/cadastro-barbeiro', {
                ...form,
                fotoUrl: form.fotoUrl.trim() || null
            });

            setForm(FORM_VAZIO);
            setSucesso('Barbeiro cadastrado com sucesso.');
            await atualizarListas();
        } catch (error) {
            setErro(mensagemErro(error, 'Erro ao cadastrar barbeiro.'));
        } finally {
            setCarregando(false);
        }
    }

    function abrirEdicao(barbeiro, destino = 'servicos') {
        setEditando(barbeiro);
        setServicosEscolhidos((barbeiro.servicos || []).map(s => s.id));
        setHorarios(
            (barbeiro.horarios || [])
                .map(cortar)
                .filter(h => h >= agoraISO())
                .sort()
        );
        setNovoServico(SERVICO_VAZIO);
        setMsgEdicao({ tipo: '', texto: '' });
        setDiaCancelar(hojeISO());
        setAteDia('');
        setMotivo('');
        setAba(destino === 'servicos' ? 'configurar' : destino);
    }

    async function aoDesligar(barbeiro, resultado) {
        setDesligando(null);
        setErro('');
        setSucesso(
            `${barbeiro.nome} foi desligado.` +
            (resultado?.agendamentosCancelados
                ? ` ${resultado.agendamentosCancelados} agendamento(s) cancelado(s) e clientes avisados.`
                : '')
        );
        setEditando(atual => (atual?.id === barbeiro.id ? null : atual));
        setVersaoDesligados(v => v + 1);
        await carregarDados();
    }

    function alternarServico(id) {
        setServicosEscolhidos(atual =>
            atual.includes(id)
                ? atual.filter(valor => valor !== id)
                : [...atual, id]
        );
    }

    async function criarServicoParaBarbeiro(event) {
        event.preventDefault();
        if (!editando) return;

        setMsgEdicao({ tipo: '', texto: '' });

        try {
            const { data: criado } = await api.post('/servicos', {
                nome: novoServico.nome.trim(),
                preco: Number(novoServico.preco)
            });

            const ids = [...new Set([...servicosEscolhidos, criado.id])];

            await api.put(`/barbeiros/${editando.id}/servicos`, {
                servicosIds: ids
            });

            setServicosEscolhidos(ids);
            setNovoServico(SERVICO_VAZIO);
            setMsgEdicao({
                tipo: 'ok',
                texto: 'Serviço criado e vinculado ao barbeiro.'
            });

            await atualizarListas();
        } catch (error) {
            setMsgEdicao({
                tipo: 'erro',
                texto: mensagemErro(error, 'Erro ao criar serviço.')
            });
        }
    }

    async function excluirServico(servico) {
        if (!window.confirm(
            `Excluir "${servico.nome}" de todos os barbeiros?`
        )) return;

        try {
            await api.delete(`/servicos/${servico.id}`);
            setServicosEscolhidos(atual =>
                atual.filter(id => id !== servico.id)
            );
            setMsgEdicao({
                tipo: 'ok',
                texto: 'Serviço excluído.'
            });
            await atualizarListas();
        } catch (error) {
            setMsgEdicao({
                tipo: 'erro',
                texto: mensagemErro(error, 'Erro ao excluir serviço.')
            });
        }
    }

    function adicionarHorario() {
        if (!novaData || !novoHorario) return;

        const dias = Math.min(Math.max(Number(repetir) || 1, 1), 60);
        const novos = [];

        for (let i = 0; i < dias; i++) {
            novos.push(`${somarDias(novaData, i)}T${novoHorario}`);
        }

        setHorarios(atual => [...new Set([...atual, ...novos])].sort());
    }

    async function salvar(url, corpo, textoOk) {
        setMsgEdicao({ tipo: '', texto: '' });

        try {
            await api.put(url, corpo);
            setMsgEdicao({ tipo: 'ok', texto: textoOk });
            await atualizarListas();
        } catch (error) {
            setMsgEdicao({
                tipo: 'erro',
                texto: mensagemErro(error, 'Erro ao salvar.')
            });
        }
    }

    async function cancelarDia(event) {
        event.preventDefault();
        if (!editando) return;

        const fim = ateDia || diaCancelar;

        if (!window.confirm(
            `Cancelar os horários de ${editando.nome} de ${diaCancelar} até ${fim}? Os clientes serão avisados por e-mail.`
        )) return;

        setCancelando(true);
        setMsgEdicao({ tipo: '', texto: '' });

        try {
            const { data } = await api.post(
                `/barbeiros/${editando.id}/cancelar-dia`,
                {
                    data: diaCancelar,
                    ateData: ateDia || null,
                    motivo: motivo.trim() || null
                }
            );

            setHorarios(atual => atual.filter(h => {
                const dia = cortar(h).slice(0, 10);
                return dia < diaCancelar || dia > fim;
            }));

            setMotivo('');
            setMsgEdicao({
                tipo: 'ok',
                texto: `${data.agendamentosCancelados} agendamentos cancelados e ${data.horariosRemovidos} horários removidos.`
            });

            await atualizarListas();
        } catch (error) {
            setMsgEdicao({
                tipo: 'erro',
                texto: mensagemErro(error, 'Erro ao cancelar o dia.')
            });
        } finally {
            setCancelando(false);
        }
    }

    const hoje = hojeISO();

    const agendamentosHoje = agendamentos.filter(a =>
        a.data === hoje && a.status !== 'CANCELADO'
    );

    const concluidosHoje = agendamentosHoje.filter(a =>
        a.status === 'CONCLUIDO'
    );

    const faltasHoje = agendamentosHoje.filter(a =>
        a.status === 'NAO_COMPARECEU'
    );

    const agendaFiltrada = agendamentos
        .filter(a => a.data === dataAgenda)
        .sort((a, b) =>
            String(a.horario).localeCompare(String(b.horario))
        );

    const totalBruto = resumoFinanceiro?.totalBruto;
    const taxaLoja = resumoFinanceiro?.taxaLoja;
    const totalLiquido = resumoFinanceiro?.totalLiquido;

    function GestaoBarbeiro() {
        if (!editando) {
            return (
                <div className="adm-vazio">
                    Selecione um barbeiro na aba Barbeiros para configurar
                    seus serviços e horários.
                </div>
            );
        }

        return (
            <div className="adm-stack">
                <div className="adm-card">
                    <div className="adm-card-head">
                        <div>
                            <h3>{editando.nome}</h3>
                            <p>Gerenciamento de serviços e agenda</p>
                        </div>
                        <button
                            className="adm-btn adm-btn--outline"
                            onClick={() => {
                                setEditando(null);
                                setAba('barbeiros');
                            }}
                        >
                            Fechar
                        </button>
                    </div>

                    <Aviso tipo={msgEdicao.tipo} texto={msgEdicao.texto} />

                    <h4>Serviços vinculados</h4>

                    <div className="adm-chips">
                        {servicos.map(s => (
                            <div className="adm-service-chip" key={s.id}>
                                <label>
                                    <input
                                        type="checkbox"
                                        checked={servicosEscolhidos.includes(s.id)}
                                        onChange={() => alternarServico(s.id)}
                                    />
                                    <span>{s.nome} · {moeda(s.preco)}</span>
                                </label>
                                <button
                                    title="Excluir serviço de todos os barbeiros"
                                    onClick={() => excluirServico(s)}
                                >
                                    <Trash2 size={15} />
                                </button>
                            </div>
                        ))}
                    </div>

                    <button
                        className="adm-btn adm-btn--gold"
                        onClick={() => salvar(
                            `/barbeiros/${editando.id}/servicos`,
                            { servicosIds: servicosEscolhidos },
                            'Serviços atualizados.'
                        )}
                    >
                        Salvar serviços
                    </button>
                </div>

                <div className="adm-card">
                    <h3>Criar serviço</h3>
                    <p className="adm-muted">
                        O novo serviço será vinculado a {editando.nome}.
                    </p>

                    <form onSubmit={criarServicoParaBarbeiro}>
                        <div className="adm-form-grid">
                            <label>
                                Nome
                                <input
                                    required
                                    value={novoServico.nome}
                                    onChange={e => setNovoServico({
                                        ...novoServico,
                                        nome: e.target.value
                                    })}
                                    placeholder="Ex.: Corte degradê"
                                />
                            </label>
                            <label>
                                Preço
                                <input
                                    required
                                    type="number"
                                    min="0.01"
                                    step="0.01"
                                    value={novoServico.preco}
                                    onChange={e => setNovoServico({
                                        ...novoServico,
                                        preco: e.target.value
                                    })}
                                />
                            </label>
                        </div>

                        <button className="adm-btn adm-btn--gold" type="submit">
                            <Plus size={16} /> Criar e adicionar
                        </button>
                    </form>
                </div>

                <div className="adm-card">
                    <h3>Horários de trabalho</h3>

                    <div className="adm-form-grid adm-form-grid--three">
                        <label>
                            Data
                            <input
                                type="date"
                                min={hojeISO()}
                                value={novaData}
                                onChange={e => setNovaData(e.target.value)}
                            />
                        </label>
                        <label>
                            Horário
                            <input
                                type="time"
                                value={novoHorario}
                                onChange={e => setNovoHorario(e.target.value)}
                            />
                        </label>
                        <label>
                            Dias seguidos
                            <input
                                type="number"
                                min="1"
                                max="60"
                                value={repetir}
                                onChange={e => setRepetir(e.target.value)}
                            />
                        </label>
                    </div>

                    <button
                        className="adm-btn adm-btn--outline"
                        onClick={adicionarHorario}
                    >
                        <Plus size={16} /> Adicionar horário
                    </button>

                    <div className="adm-chips">
                        {horarios.map(h => (
                            <span className="adm-horario" key={h}>
                                {formatar(h)}
                                <button
                                    onClick={() => setHorarios(
                                        horarios.filter(x => x !== h)
                                    )}
                                >
                                    <X size={14} />
                                </button>
                            </span>
                        ))}
                    </div>

                    <button
                        className="adm-btn adm-btn--gold"
                        onClick={() => salvar(
                            `/barbeiros/${editando.id}/horarios`,
                            { horarios },
                            'Horários atualizados.'
                        )}
                    >
                        Salvar horários
                    </button>
                </div>

                <div className="adm-card">
                    <h3>Cancelar dia de trabalho</h3>
                    <p className="adm-muted">
                        Os clientes com agendamento serão avisados por e-mail.
                    </p>

                    <form onSubmit={cancelarDia}>
                        <div className="adm-form-grid">
                            <label>
                                Data inicial
                                <input
                                    type="date"
                                    min={hojeISO()}
                                    value={diaCancelar}
                                    onChange={e => setDiaCancelar(e.target.value)}
                                    required
                                />
                            </label>
                            <label>
                                Até (opcional)
                                <input
                                    type="date"
                                    min={diaCancelar}
                                    value={ateDia}
                                    onChange={e => setAteDia(e.target.value)}
                                />
                            </label>
                        </div>

                        <label className="adm-field">
                            Motivo
                            <input
                                value={motivo}
                                onChange={e => setMotivo(e.target.value)}
                                placeholder="Ex.: Barbeiro indisponível"
                            />
                        </label>

                        <button
                            className="adm-btn adm-btn--danger"
                            type="submit"
                            disabled={cancelando}
                        >
                            <AlertTriangle size={16} />
                            {cancelando ? 'Cancelando...' : 'Cancelar dia e avisar'}
                        </button>
                    </form>
                </div>
            </div>
        );
    }

    return (
        <SidebarProfissional>
            <div className="adm-page-top">
                <div>
                    <span className="adm-eyebrow">BARBERAPP / ADMINISTRAÇÃO</span>
                    <h1>
                        {aba === 'configurar' ? 'Gerenciar barbeiro' : MENU.find(item => item.id === aba)?.titulo || 'Administração'}
                    </h1>
                    <p>Gerencie sua barbearia em um só lugar.</p>
                </div>

                <button
                    className="adm-btn adm-btn--outline"
                    onClick={carregarDados}
                    disabled={carregandoDados}
                >
                    <RefreshCw size={16} />
                    Atualizar
                </button>
            </div>

            <Aviso tipo="erro" texto={erroDados} />

            {aba === 'dashboard' && (
                <div className="adm-stack">
                    <div className="adm-kpi-grid">
                        <Card
                            titulo="Faturamento bruto"
                            valor={totalBruto == null ? '—' : moeda(totalBruto)}
                            descricao="Pagamentos confirmados"
                            Icone={TrendingUp}
                            destaque
                        />
                        <Card
                            titulo="Receita da barbearia"
                            valor={taxaLoja == null ? '—' : moeda(taxaLoja)}
                            descricao="Participação de 20%"
                            Icone={Wallet}
                        />
                        <Card
                            titulo="Agendamentos hoje"
                            valor={agendamentosHoje.length}
                            descricao={`${concluidosHoje.length} concluídos`}
                            Icone={CalendarDays}
                        />
                        <Card
                            titulo="Barbeiros cadastrados"
                            valor={barbeiros.length}
                            descricao="Profissionais registrados"
                            Icone={Users}
                        />
                    </div>

                    <Aviso tipo="erro" texto={erroFinanceiro} />

                    <div className="adm-columns">
                        <section className="adm-card">
                            <div className="adm-card-head">
                                <div>
                                    <h3>Agenda de hoje</h3>
                                    <p>Atendimentos da barbearia</p>
                                </div>
                                <button
                                    className="adm-text-button"
                                    onClick={() => setAba('horarios')}
                                >
                                    Ver agenda
                                </button>
                            </div>

                            {agendamentosHoje.length === 0 ? (
                                <div className="adm-vazio">
                                    Nenhum agendamento para hoje.
                                </div>
                            ) : (
                                <div className="adm-list">
                                    {agendamentosHoje
                                        .slice(0, 8)
                                        .map(a => (
                                            <div className="adm-agenda-item" key={a.id}>
                                                <div className="adm-time">
                                                    {String(a.horario).slice(0, 5)}
                                                </div>
                                                <div>
                                                    <strong>{a.clienteNome}</strong>
                                                    <small>
                                                        {a.barbeiroNome} · {a.servicoNome}
                                                    </small>
                                                </div>
                                                <span className="adm-status">
                                                    {a.status}
                                                </span>
                                            </div>
                                        ))}
                                </div>
                            )}
                        </section>

                        <section className="adm-card">
                            <div className="adm-card-head">
                                <div>
                                    <h3>Resumo operacional</h3>
                                    <p>Indicadores do dia</p>
                                </div>
                            </div>

                            <div className="adm-summary-line">
                                <span>Atendimentos concluídos</span>
                                <strong>{concluidosHoje.length}</strong>
                            </div>
                            <div className="adm-summary-line">
                                <span>Não compareceram</span>
                                <strong>{faltasHoje.length}</strong>
                            </div>
                            <div className="adm-summary-line">
                                <span>Serviços cadastrados</span>
                                <strong>{servicos.length}</strong>
                            </div>
                            <div className="adm-summary-line">
                                <span>Repasse aos barbeiros</span>
                                <strong>
                                    {totalLiquido == null ? '—' : moeda(totalLiquido)}
                                </strong>
                            </div>
                        </section>
                    </div>

                    <section className="adm-card">
                        <div className="adm-card-head">
                            <div>
                                <h3>Equipe</h3>
                                <p>Barbeiros e serviços oferecidos</p>
                            </div>
                            <button
                                className="adm-btn adm-btn--gold"
                                onClick={() => setAba('barbeiros')}
                            >
                                <Plus size={16} /> Gerenciar
                            </button>
                        </div>

                        <div className="adm-team-grid">
                            {barbeiros.map(b => (
                                <div className="adm-team-card" key={b.id}>
                                    {b.fotoUrl ? (
                                        <img src={b.fotoUrl} alt={b.nome} />
                                    ) : (
                                        <span className="adm-avatar">
                                            {b.nome?.charAt(0).toUpperCase()}
                                        </span>
                                    )}
                                    <strong>{b.nome}</strong>
                                    <small>
                                        {b.servicos?.length || 0} serviços
                                    </small>
                                    <button onClick={() => abrirEdicao(b)}>
                                        Gerenciar <ChevronRight size={14} />
                                    </button>
                                </div>
                            ))}
                        </div>
                    </section>
                </div>
            )}

            {aba === 'barbeiros' && (
                <div className="adm-stack">
                    <section className="adm-card">
                        <div className="adm-card-head">
                            <div>
                                <h3>Cadastrar barbeiro</h3>
                                <p>Adicione um profissional à equipe</p>
                            </div>
                            <Users size={21} className="adm-gold" />
                        </div>

                        <form onSubmit={handleCadastroBarbeiro} autoComplete="off">
                            <div className="adm-form-grid">
                                {[
                                    ['nome', 'Nome', 'text', 'Nome do barbeiro'],
                                    ['email', 'Email', 'email', 'barbeiro@email.com'],
                                    ['senha', 'Senha inicial', 'password', 'Senha'],
                                    ['telefone', 'Telefone', 'tel', '(79) 99999-9999']
                                ].map(([campo, titulo, tipo, placeholder]) => (
                                    <label key={campo}>
                                        {titulo}
                                        <input
                                            required
                                            type={tipo}
                                            placeholder={placeholder}
                                            value={form[campo]}
                                            autoComplete={
                                                campo === 'senha'
                                                    ? 'new-password'
                                                    : 'off'
                                            }
                                            onChange={e => setForm({
                                                ...form,
                                                [campo]: e.target.value
                                            })}
                                        />
                                    </label>
                                ))}

                                <label className="adm-full">
                                    URL da foto (opcional)
                                    <input
                                        value={form.fotoUrl}
                                        placeholder="https://..."
                                        onChange={e => setForm({
                                            ...form,
                                            fotoUrl: e.target.value
                                        })}
                                    />
                                </label>
                            </div>

                            <Aviso tipo="erro" texto={erro} />
                            <Aviso tipo="ok" texto={sucesso} />

                            <button
                                type="submit"
                                className="adm-btn adm-btn--gold"
                                disabled={carregando}
                            >
                                <Plus size={16} />
                                {carregando ? 'Cadastrando...' : 'Cadastrar barbeiro'}
                            </button>
                        </form>
                    </section>

                    <section className="adm-card">
                        <h3>Barbeiros cadastrados</h3>

                        {barbeiros.length === 0 && (
                            <div className="adm-vazio">
                                Nenhum barbeiro cadastrado.
                            </div>
                        )}

                        <div className="adm-list">
                            {barbeiros.map(b => (
                                <div className="adm-barber-row" key={b.id}>
                                    {b.fotoUrl ? (
                                        <img src={b.fotoUrl} alt={b.nome} />
                                    ) : (
                                        <span className="adm-avatar">
                                            {b.nome?.charAt(0).toUpperCase()}
                                        </span>
                                    )}

                                    <div className="adm-barber-info">
                                        <strong>{b.nome}</strong>
                                        <small>
                                            {b.servicos?.length || 0} serviços ·
                                            {' '}{b.horarios?.length || 0} horários
                                        </small>
                                    </div>

                                    <button
                                        className="adm-btn adm-btn--outline"
                                        onClick={() => abrirEdicao(b)}
                                    >
                                        Gerenciar
                                    </button>
                                    <button
                                        className="adm-btn adm-btn--danger"
                                        onClick={() => {
                                            setSucesso('');
                                            setDesligando(b);
                                        }}
                                    >
                                        <UserMinus size={15} /> Desligar
                                    </button>
                                </div>
                            ))}
                        </div>
                    </section>

                    <EquipeDesligada versao={versaoDesligados} />
                </div>
            )}

            <DesligarBarbeiro
                key={desligando?.id ?? 'nenhum'}
                barbeiro={desligando}
                futuros={desligando ? agendamentos.filter(a =>
                    a.barbeiroNome === desligando.nome
                    && ['PENDENTE', 'CONFIRMADO'].includes(a.status)
                    && `${a.data}T${String(a.horario).slice(0, 5)}` >= agoraISO()
                ).length : 0}
                aoFechar={() => setDesligando(null)}
                aoConcluir={aoDesligar}
            />

            {aba === 'servicos' && (
                <ServicosAdmin
                    servicos={servicos}
                    barbeiros={barbeiros}
                    agendamentos={agendamentos}
                    aoAtualizar={atualizarListas}
                />
            )}

            {aba === 'configurar' && GestaoBarbeiro()}

            {aba === 'horarios' && (
                <div className="adm-stack">
                    <section className="adm-card">
                        <div className="adm-card-head">
                            <div>
                                <h3>Agenda geral</h3>
                                <p>Agendamentos de todos os barbeiros</p>
                            </div>
                            <input
                                className="adm-date"
                                type="date"
                                value={dataAgenda}
                                onChange={e => setDataAgenda(e.target.value)}
                            />
                        </div>

                        {agendaFiltrada.length === 0 ? (
                            <div className="adm-vazio">
                                Nenhum agendamento nessa data.
                            </div>
                        ) : (
                            <div className="adm-list">
                                {agendaFiltrada.map(a => (
                                    <div className="adm-agenda-item" key={a.id}>
                                        <div className="adm-time">
                                            {String(a.horario).slice(0, 5)}
                                        </div>
                                        <div>
                                            <strong>{a.clienteNome}</strong>
                                            <small>
                                                {a.barbeiroNome} · {a.servicoNome}
                                            </small>
                                        </div>
                                        <span className="adm-status">
                                            {a.status}
                                        </span>
                                    </div>
                                ))}
                            </div>
                        )}
                    </section>

                    <DesempenhoBarbeiros
                        barbeiros={barbeiros}
                        agendamentos={agendamentos}
                        notas={notas}
                        aoGerenciar={b => abrirEdicao(b, 'configurar')}
                    />
                </div>
            )}

            {aba === 'financeiro' && (
                <div className="adm-stack">
                    <div className="adm-kpi-grid adm-kpi-grid--three">
                        <Card
                            titulo="Faturamento bruto"
                            valor={totalBruto == null ? '—' : moeda(totalBruto)}
                            descricao="Pagamentos confirmados"
                            Icone={TrendingUp}
                        />
                        <Card
                            titulo="Receita da loja"
                            valor={taxaLoja == null ? '—' : moeda(taxaLoja)}
                            descricao="20% do faturamento"
                            Icone={Wallet}
                            destaque
                        />
                        <Card
                            titulo="Repasse aos barbeiros"
                            valor={totalLiquido == null ? '—' : moeda(totalLiquido)}
                            descricao="80% do faturamento"
                            Icone={Users}
                        />
                    </div>

                    <Aviso tipo="erro" texto={erroFinanceiro} />

                    <GraficosFinanceiro agendamentos={agendamentos} />

                    <section className="adm-card">
                        <h3>Relatório financeiro administrativo</h3>
                        <p className="adm-muted">
                            Informações restritas ao administrador.
                        </p>
                        <FinanceiroAdmin />
                    </section>
                </div>
            )}

            {aba === 'avaliacoes' && (
                <section className="adm-card">
                    <h3>Avaliações dos barbeiros</h3>
                    <p className="adm-muted">
                        Notas registradas pelos clientes.
                    </p>

                    <div className="adm-list">
                        {barbeiros.map(b => {
                            const nota = notas[b.id];
                            const media = nota?.media ?? nota?.notaMedia;
                            const quantidade =
                                nota?.quantidade ?? nota?.totalAvaliacoes;

                            return (
                                <div className="adm-barber-row" key={b.id}>
                                    <span className="adm-avatar">
                                        {b.nome?.charAt(0).toUpperCase()}
                                    </span>
                                    <div className="adm-barber-info">
                                        <strong>{b.nome}</strong>
                                        <small>
                                            {quantidade == null
                                                ? 'Avaliações indisponíveis'
                                                : `${quantidade} avaliações`}
                                        </small>
                                    </div>
                                    <div className="adm-rating">
                                        <Star size={17} fill="currentColor" />
                                        <strong>
                                            {media == null
                                                ? '—'
                                                : Number(media).toFixed(1)}
                                        </strong>
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                </section>
            )}

            {aba === 'clientes' && <ClientesBloqueados />}

            {aba === 'configuracoes' && (
                <CentralPendencias
                    barbeiros={barbeiros}
                    servicos={servicos}
                    agendamentos={agendamentos}
                    notas={notas}
                    aoGerenciarBarbeiro={b => abrirEdicao(b, 'configurar')}
                    irPara={setAba}
                />
            )}
        </SidebarProfissional>
    );
}

export default Admin;
