import { useEffect, useState } from 'react';
import api from '../../api/api.js';
import Navbar from '../../components/Navbar.jsx';
import { Title } from '../LoginECadastro/Style.js';

const FORM_VAZIO = { nome: '', email: '', senha: '', telefone: '', fotoUrl: '' };
const SERVICO_VAZIO = { nome: '', preco: '' };

// ---- datas (tudo em horário local, no formato "2026-10-20T09:00") ----
const dois = (n) => String(n).padStart(2, '0');
const paraISO = (d) => `${d.getFullYear()}-${dois(d.getMonth() + 1)}-${dois(d.getDate())}`;
const hojeISO = () => paraISO(new Date());
const agoraISO = () => {
    const d = new Date();
    return `${paraISO(d)}T${dois(d.getHours())}:${dois(d.getMinutes())}`;
};
const cortar = (dh) => String(dh).slice(0, 16);
const somarDias = (iso, n) => {
    const [a, m, d] = iso.split('-').map(Number);
    return paraISO(new Date(a, m - 1, d + n));
};
const formatar = (dh) => {
    const [data, hora] = cortar(dh).split('T');
    const [, mes, dia] = data.split('-');
    return `${dia}/${mes} ${hora}`;
};
// NOVO: "2026-10-28" -> "28/10/2026"
const br = (iso) => iso.split('-').reverse().join('/');

function Admin() {
    const [barbeiros, setBarbeiros] = useState([]);
    const [servicos, setServicos] = useState([]);
    const [form, setForm] = useState(FORM_VAZIO);
    const [erro, setErro] = useState('');
    const [sucesso, setSucesso] = useState('');
    const [carregando, setCarregando] = useState(false);

    // Edição de horários e serviços do barbeiro escolhido
    const [editando, setEditando] = useState(null);
    const [servicosEscolhidos, setServicosEscolhidos] = useState([]);
    const [novoServico, setNovoServico] = useState(SERVICO_VAZIO);
    const [horarios, setHorarios] = useState([]);
    const [novaData, setNovaData] = useState(hojeISO());
    const [novoHorario, setNovoHorario] = useState('09:00');
    const [repetir, setRepetir] = useState(1);
    const [msgEdicao, setMsgEdicao] = useState({ tipo: '', texto: '' });

    // NOVO: cancelamento de dia
    const [diaCancelar, setDiaCancelar] = useState(hojeISO());
    const [ateDia, setAteDia] = useState('');
    const [motivo, setMotivo] = useState('');
    const [cancelando, setCancelando] = useState(false);

    async function carregarBarbeiros() {
        try {
            const response = await api.get('/barbeiros');
            setBarbeiros(response.data);
        } catch (error) {
            console.error('Erro ao carregar barbeiros:', error);
        }
    }

    async function carregarServicos() {
        try {
            const response = await api.get('/servicos');
            setServicos(response.data);
        } catch (error) {
            console.error('Erro ao carregar serviços:', error);
        }
    }

    useEffect(() => {
        carregarBarbeiros();
        carregarServicos();
    }, []);

    function atualizarCampo(campo, valor) {
        setForm({ ...form, [campo]: valor });
    }

    async function handleCadastroBarbeiro(event) {
        event.preventDefault();
        setErro('');
        setSucesso('');
        setCarregando(true);

        try {
            await api.post('/auth/cadastro-barbeiro', {
                ...form,
                fotoUrl: form.fotoUrl.trim() === '' ? null : form.fotoUrl.trim(),
            });
            setSucesso('Barbeiro cadastrado com sucesso!');
            setForm(FORM_VAZIO);
            carregarBarbeiros();
        } catch (error) {
            setErro(error.response?.data?.message || 'Erro ao cadastrar barbeiro. Verifique os dados.');
        } finally {
            setCarregando(false);
        }
    }

    function abrirEdicao(barbeiro) {
        setEditando(barbeiro);
        setServicosEscolhidos((barbeiro.servicos || []).map((s) => s.id));
        setNovoServico(SERVICO_VAZIO);
        setHorarios((barbeiro.horarios || []).map(cortar).filter((h) => h >= agoraISO()).sort());
        setMsgEdicao({ tipo: '', texto: '' });
        setDiaCancelar(hojeISO());   // NOVO: limpa o formulário de cancelamento
        setAteDia('');
        setMotivo('');
    }

    function alternarServico(id) {
        setServicosEscolhidos(
            servicosEscolhidos.includes(id)
                ? servicosEscolhidos.filter((s) => s !== id)
                : [...servicosEscolhidos, id]
        );
    }

    // Cria o serviço e já adiciona ao barbeiro que está sendo editado
    async function criarServicoParaBarbeiro(event) {
        event.preventDefault();
        setMsgEdicao({ tipo: '', texto: '' });
        try {
            const { data: criado } = await api.post('/servicos', {
                nome: novoServico.nome.trim(),
                preco: Number(novoServico.preco),
            });

            const ids = [...servicosEscolhidos, criado.id];
            await api.put(`/barbeiros/${editando.id}/servicos`, { servicosIds: ids });

            setServicosEscolhidos(ids);
            setNovoServico(SERVICO_VAZIO);
            setMsgEdicao({ tipo: 'ok', texto: `"${criado.nome}" criado e adicionado a ${editando.nome}.` });
            carregarServicos();
            carregarBarbeiros();
        } catch (error) {
            setMsgEdicao({
                tipo: 'erro',
                texto: error.response?.data?.message || 'Não foi possível criar o serviço.',
            });
        }
    }

    async function excluirServico(servico) {
        // ALTERADO: o texto deixa claro que apaga para todos os barbeiros
        if (!window.confirm(`Excluir "${servico.nome}" de TODOS os barbeiros? Isso não pode ser desfeito.`)) return;
        setMsgEdicao({ tipo: '', texto: '' });
        try {
            await api.delete(`/servicos/${servico.id}`);
            setServicosEscolhidos((ids) => ids.filter((id) => id !== servico.id));
            setMsgEdicao({ tipo: 'ok', texto: 'Serviço excluído.' });
            carregarServicos();
            carregarBarbeiros();
        } catch (error) {
            setMsgEdicao({
                tipo: 'erro',
                texto: error.response?.data?.message || 'Não foi possível excluir.',
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
        setHorarios([...new Set([...horarios, ...novos])].sort());
    }

    async function salvar(url, corpo, textoOk) {
        setMsgEdicao({ tipo: '', texto: '' });
        try {
            await api.put(url, corpo);
            setMsgEdicao({ tipo: 'ok', texto: textoOk });
            carregarBarbeiros();
        } catch (error) {
            setMsgEdicao({
                tipo: 'erro',
                texto: error.response?.data?.message || 'Erro ao salvar.',
            });
        }
    }

    // NOVO: cancela o dia (ou período) do barbeiro e avisa os clientes por e-mail
    async function cancelarDia(event) {
        event.preventDefault();
        const fim = ateDia || diaCancelar;
        const periodo = fim !== diaCancelar
            ? `de ${br(diaCancelar)} a ${br(fim)}`
            : `o dia ${br(diaCancelar)}`;

        if (!window.confirm(`Cancelar ${periodo} de ${editando.nome}? Os clientes com agendamento serão avisados por e-mail.`)) return;

        setMsgEdicao({ tipo: '', texto: '' });
        setCancelando(true);
        try {
            const { data } = await api.post(`/barbeiros/${editando.id}/cancelar-dia`, {
                data: diaCancelar,
                ateData: ateDia || null,
                motivo: motivo.trim() || null,
            });

            setHorarios((lista) => lista.filter((h) => {
                const dia = cortar(h).slice(0, 10);
                return dia < diaCancelar || dia > fim;
            }));
            setMotivo('');
            setMsgEdicao({
                tipo: 'ok',
                texto: `${data.agendamentosCancelados} agendamento(s) cancelado(s) e ${data.horariosRemovidos} horário(s) removido(s). Os clientes foram avisados por e-mail.`,
            });
            carregarBarbeiros();
        } catch (error) {
            setMsgEdicao({
                tipo: 'erro',
                texto: error.response?.data?.message || 'Não foi possível cancelar o dia.',
            });
        } finally {
            setCancelando(false);
        }
    }

    return (
        <>
            <Navbar />
            <main className="page">
                <header className="page__head">
                    <Title>Administração</Title>
                    <p className="muted">Cadastre barbeiros e defina os serviços e horários de cada um.</p>
                </header>

                {/* ---------- CRIAR BARBEIRO ---------- */}
                <section className="panel">
                    <h2>Criar barbeiro</h2>
                    <form onSubmit={handleCadastroBarbeiro} autoComplete="off">
                        <div className="form-grid">
                            <label className="field">
                                <span>Nome</span>
                                <input type="text" placeholder="Nome do barbeiro" value={form.nome}
                                       onChange={(e) => atualizarCampo('nome', e.target.value)} required />
                            </label>

                            <label className="field">
                                <span>Email</span>
                                <input type="email" placeholder="barbeiro@email.com" value={form.email}
                                       autoComplete="off"
                                       onChange={(e) => atualizarCampo('email', e.target.value)} required />
                            </label>

                            <label className="field">
                                <span>Senha inicial</span>
                                <input type="password" placeholder="••••••••" value={form.senha}
                                       autoComplete="new-password"
                                       onChange={(e) => atualizarCampo('senha', e.target.value)} required />
                            </label>

                            <label className="field">
                                <span>Telefone</span>
                                <input type="text" placeholder="(99) 9 9999-9999" value={form.telefone}
                                       onChange={(e) => atualizarCampo('telefone', e.target.value)} required />
                            </label>

                            <label className="field full">
                                <span>URL da foto (opcional)</span>
                                <input type="text" placeholder="https://..." value={form.fotoUrl}
                                       onChange={(e) => atualizarCampo('fotoUrl', e.target.value)} />
                            </label>
                        </div>

                        {erro && <p className="alert alert--erro" style={{ marginTop: 14 }}>{erro}</p>}
                        {sucesso && <p className="alert alert--ok" style={{ marginTop: 14 }}>{sucesso}</p>}

                        <button type="submit" className="btn btn--primary btn--block"
                                disabled={carregando} style={{ marginTop: 14 }}>
                            {carregando ? 'Criando...' : 'Criar barbeiro'}
                        </button>
                    </form>
                </section>

                {/* ---------- HORÁRIOS E SERVIÇOS DO BARBEIRO ---------- */}
                {editando && (
                    <section className="panel">
                        <div className="row row--between">
                            <h2>Horários e serviços de {editando.nome}</h2>
                            <button className="btn btn--ghost" onClick={() => setEditando(null)}>Fechar</button>
                        </div>

                        {msgEdicao.texto && (
                            <p className={`alert alert--${msgEdicao.tipo}`}>{msgEdicao.texto}</p>
                        )}

                        <h3>Serviços</h3>
                        {servicos.length === 0 && (
                            <p className="muted">Nenhum serviço ainda. Crie o primeiro logo abaixo.</p>
                        )}
                        <div className="chips">
                            {servicos.map((s) => {
                                const ativo = servicosEscolhidos.includes(s.id);
                                return (
                                    <label key={s.id} className={`chip ${ativo ? 'chip--on' : ''}`}>
                                        <input type="checkbox" checked={ativo}
                                               onChange={() => alternarServico(s.id)} />
                                        {s.nome} · R$ {Number(s.preco).toFixed(2)}
                                        <button type="button" aria-label={`Excluir ${s.nome}`}
                                                title="Excluir este serviço de todos os barbeiros"
                                                onClick={(e) => {
                                                    e.preventDefault();
                                                    e.stopPropagation();
                                                    excluirServico(s);
                                                }}>
                                            ×
                                        </button>
                                    </label>
                                );
                            })}
                        </div>
                        <div style={{ marginTop: 14 }}>
                            <button className="btn btn--primary"
                                    onClick={() => salvar(`/barbeiros/${editando.id}/servicos`,
                                        { servicosIds: servicosEscolhidos }, 'Serviços salvos.')}>
                                Salvar serviços
                            </button>
                        </div>

                        <h3>Criar novo serviço para {editando.nome}</h3>
                        <form onSubmit={criarServicoParaBarbeiro}>
                            <div className="form-grid">
                                <label className="field">
                                    <span>Nome do serviço</span>
                                    <input type="text" placeholder="Ex.: Corte" value={novoServico.nome}
                                           onChange={(e) => setNovoServico({ ...novoServico, nome: e.target.value })}
                                           required />
                                </label>
                                <label className="field">
                                    <span>Preço (R$)</span>
                                    <input type="number" min="0.01" step="0.01" placeholder="40.00"
                                           value={novoServico.preco}
                                           onChange={(e) => setNovoServico({ ...novoServico, preco: e.target.value })}
                                           required />
                                </label>
                            </div>
                            <button type="submit" className="btn btn--ghost" style={{ marginTop: 14 }}>
                                Criar e adicionar
                            </button>
                        </form>

                        <h3>Horários de trabalho</h3>
                        <div className="row">
                            <label className="field">
                                <span>Dia</span>
                                <input type="date" value={novaData} min={hojeISO()}
                                       onChange={(e) => setNovaData(e.target.value)} />
                            </label>
                            <label className="field">
                                <span>Hora</span>
                                <input type="time" value={novoHorario}
                                       onChange={(e) => setNovoHorario(e.target.value)} />
                            </label>
                            <label className="field" style={{ width: 110 }}>
                                <span>Dias seguidos</span>
                                <input type="number" min="1" max="60" value={repetir}
                                       onChange={(e) => setRepetir(e.target.value)} />
                            </label>
                            <button type="button" className="btn btn--ghost" onClick={adicionarHorario}
                                    style={{ alignSelf: 'flex-end' }}>
                                Adicionar
                            </button>
                        </div>
                        <p className="muted small" style={{ margin: '8px 0 0' }}>
                            Ex.: dia 20, 09:00, 5 dias seguidos = 09:00 de 20 a 24.
                        </p>

                        <div className="chips" style={{ marginTop: 12 }}>
                            {horarios.length === 0 && <span className="muted">Nenhum horário ainda.</span>}
                            {horarios.map((h) => (
                                <span key={h} className="chip chip--on">
                                    {formatar(h)}
                                    <button type="button" aria-label={`Remover ${formatar(h)}`}
                                            onClick={() => setHorarios(horarios.filter((x) => x !== h))}>
                                        ×
                                    </button>
                                </span>
                            ))}
                        </div>
                        <div style={{ marginTop: 14 }}>
                            <button className="btn btn--primary"
                                    onClick={() => salvar(`/barbeiros/${editando.id}/horarios`,
                                        { horarios }, 'Horários salvos.')}>
                                Salvar horários
                            </button>
                        </div>

                        {/* NOVO: cancelar dia e avisar clientes */}
                        <h3>Cancelar dia (ex.: barbeiro doente)</h3>
                        <form onSubmit={cancelarDia}>
                            <div className="row">
                                <label className="field">
                                    <span>Dia</span>
                                    <input type="date" value={diaCancelar} min={hojeISO()}
                                           onChange={(e) => setDiaCancelar(e.target.value)} required />
                                </label>
                                <label className="field">
                                    <span>Até (opcional)</span>
                                    <input type="date" value={ateDia} min={diaCancelar}
                                           onChange={(e) => setAteDia(e.target.value)} />
                                </label>
                            </div>
                            <label className="field" style={{ marginTop: 12 }}>
                                <span>Motivo (aparece no e-mail)</span>
                                <input type="text" placeholder="Ex.: o barbeiro está doente" value={motivo}
                                       onChange={(e) => setMotivo(e.target.value)} />
                            </label>
                            <button type="submit" className="btn btn--ghost" disabled={cancelando}
                                    style={{ marginTop: 14 }}>
                                {cancelando ? 'Cancelando...' : 'Cancelar dia e avisar clientes'}
                            </button>
                        </form>
                    </section>
                )}

                {/* ---------- LISTA DE BARBEIROS ---------- */}
                <section className="panel">
                    <h2>Barbeiros cadastrados</h2>
                    {barbeiros.length === 0 && <p className="muted">Nenhum barbeiro cadastrado no momento.</p>}
                    <ul>
                        {barbeiros.map((barbeiro) => (
                            <li key={barbeiro.id} className="lista__item">
                                {barbeiro.fotoUrl ? (
                                    <img src={barbeiro.fotoUrl} alt={barbeiro.nome}
                                         style={{ width: 42, height: 42, borderRadius: '50%', objectFit: 'cover' }} />
                                ) : (
                                    <div className="avatar">{barbeiro.nome.charAt(0).toUpperCase()}</div>
                                )}
                                <div className="lista__info">
                                    <strong>{barbeiro.nome}</strong>
                                    <span className="muted small">
                                        {barbeiro.servicos?.length ?? 0} serviços · {barbeiro.horarios?.length ?? 0} horários
                                    </span>
                                </div>
                                <button className="btn btn--ghost" onClick={() => abrirEdicao(barbeiro)}>
                                    Horários e serviços
                                </button>
                            </li>
                        ))}
                    </ul>
                </section>
            </main>
        </>
    );
}

export default Admin;