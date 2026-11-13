import {useState} from 'react';
import {Pencil, Plus, Scissors, Trash2, TrendingUp, Users} from 'lucide-react';
import api from '../../api/api.js';
import {ehConcluido, mensagemErro, moeda, valorAg} from '../../pages/Admin/util.js';
import './AdminExtras.css';

const NOVO_VAZIO = {nome: '', preco: '', barbeiros: []};

/** Catálogo de serviços: preços, quem faz cada um, quanto cada um rende. */
export default function ServicosAdmin({servicos, barbeiros, agendamentos, aoAtualizar}) {
    const [msg, setMsg] = useState({tipo: '', texto: ''});
    const [novo, setNovo] = useState(NOVO_VAZIO);
    const [editandoId, setEditandoId] = useState(null);
    const [edicao, setEdicao] = useState({nome: '', preco: ''});
    const [salvando, setSalvando] = useState(false);

    const concluidos = agendamentos.filter(ehConcluido);

    const estatisticas = servico => {
        const feitos = concluidos.filter(a => a.servicoNome === servico.nome);
        return {
            qtd: feitos.length,
            receita: feitos.reduce((s, a) => s + valorAg(a), 0),
            barbeiros: barbeiros.filter(b => (b.servicos || []).some(s => s.id === servico.id))
        };
    };

    const linhas = servicos.map(s => ({servico: s, ...estatisticas(s)}));
    const maisPedido = [...linhas].sort((a, b) => b.qtd - a.qtd)[0];
    const maisRentavel = [...linhas].sort((a, b) => b.receita - a.receita)[0];
    const precoMedio = servicos.length
        ? servicos.reduce((s, x) => s + Number(x.preco || 0), 0) / servicos.length : 0;
    const semBarbeiro = linhas.filter(l => l.barbeiros.length === 0);

    async function criar(event) {
        event.preventDefault();
        setSalvando(true);
        setMsg({tipo: '', texto: ''});
        try {
            const {data: criado} = await api.post('/servicos', {
                nome: novo.nome.trim(),
                preco: Number(novo.preco)
            });
            for (const id of novo.barbeiros) {
                const b = barbeiros.find(x => x.id === id);
                const ids = [...new Set([...(b?.servicos || []).map(s => s.id), criado.id])];
                await api.put(`/barbeiros/${id}/servicos`, {servicosIds: ids});
            }
            setNovo(NOVO_VAZIO);
            setMsg({tipo: 'ok', texto: `Serviço "${criado.nome}" criado.`});
            await aoAtualizar();
        } catch (error) {
            setMsg({tipo: 'erro', texto: mensagemErro(error, 'Não foi possível criar o serviço.')});
        } finally {
            setSalvando(false);
        }
    }

    function abrirEdicao(servico) {
        setEditandoId(servico.id);
        setEdicao({nome: servico.nome, preco: String(servico.preco)});
        setMsg({tipo: '', texto: ''});
    }

    async function salvarEdicao(servico) {
        setSalvando(true);
        setMsg({tipo: '', texto: ''});
        try {
            await api.put(`/servicos/${servico.id}`, {
                nome: edicao.nome.trim(),
                preco: Number(edicao.preco)
            });
            setEditandoId(null);
            setMsg({tipo: 'ok', texto: 'Serviço atualizado. O novo preço vale para os próximos agendamentos.'});
            await aoAtualizar();
        } catch (error) {
            setMsg({tipo: 'erro', texto: mensagemErro(error, 'Não foi possível salvar.')});
        } finally {
            setSalvando(false);
        }
    }

    async function excluir(servico) {
        if (!window.confirm(`Excluir "${servico.nome}" do catálogo? Ele sai de todos os barbeiros.`)) return;
        setMsg({tipo: '', texto: ''});
        try {
            await api.delete(`/servicos/${servico.id}`);
            setMsg({tipo: 'ok', texto: `"${servico.nome}" foi excluído.`});
            await aoAtualizar();
        } catch (error) {
            setMsg({tipo: 'erro', texto: mensagemErro(error, 'Não foi possível excluir: o serviço já tem histórico.')});
        }
    }

    async function alternarVinculo(barbeiro, servico) {
        const atuais = (barbeiro.servicos || []).map(s => s.id);
        const ids = atuais.includes(servico.id)
            ? atuais.filter(id => id !== servico.id)
            : [...atuais, servico.id];
        setMsg({tipo: '', texto: ''});
        try {
            await api.put(`/barbeiros/${barbeiro.id}/servicos`, {servicosIds: ids});
            await aoAtualizar();
        } catch (error) {
            setMsg({tipo: 'erro', texto: mensagemErro(error, 'Não foi possível atualizar o vínculo.')});
        }
    }

    function alternarNovo(id) {
        setNovo(atual => ({
            ...atual,
            barbeiros: atual.barbeiros.includes(id)
                ? atual.barbeiros.filter(x => x !== id)
                : [...atual.barbeiros, id]
        }));
    }

    return <div className="adm-x-stack">
        <div className="adm-x-kpis">
            <div className="adm-x-kpi"><span>Serviços no catálogo</span><strong>{servicos.length}</strong>
                <small>preço médio {moeda(precoMedio)}</small></div>
            <div className="adm-x-kpi"><span>Mais pedido</span>
                <strong>{maisPedido?.qtd ? maisPedido.servico.nome : '—'}</strong>
                <small>{maisPedido?.qtd ? `${maisPedido.qtd} atendimentos concluídos` : 'sem atendimentos ainda'}</small></div>
            <div className="adm-x-kpi"><span>Que mais rende</span>
                <strong>{maisRentavel?.receita ? maisRentavel.servico.nome : '—'}</strong>
                <small>{maisRentavel?.receita ? moeda(maisRentavel.receita) : 'sem receita ainda'}</small></div>
            <div className="adm-x-kpi"><span>Sem nenhum barbeiro</span><strong>{semBarbeiro.length}</strong>
                <small>{semBarbeiro.length ? 'ninguém consegue agendar' : 'tudo coberto'}</small></div>
        </div>

        {msg.texto && <div className={`adm-x-aviso adm-x-aviso--${msg.tipo}`}>{msg.texto}</div>}

        <section className="adm-x-card">
            <div className="adm-x-head">
                <div>
                    <h3>Catálogo</h3>
                    <p className="adm-x-sub">Edite nome e preço, veja quem faz e quanto cada serviço rende.</p>
                </div>
            </div>

            {servicos.length === 0 && <p className="adm-x-sub">Nenhum serviço cadastrado ainda. Crie o primeiro abaixo.</p>}

            <div className="adm-x-servicos">
                {linhas.map(({servico, qtd, receita, barbeiros: fazem}) => <article className="adm-x-servico" key={servico.id}>
                    {editandoId === servico.id ? <div className="adm-x-edit">
                        <div className="adm-x-edit-linha">
                            <input className="adm-x-input" value={edicao.nome} aria-label="Nome do serviço"
                                   onChange={e => setEdicao({...edicao, nome: e.target.value})}/>
                            <input className="adm-x-input" type="number" min="0.01" step="0.01" value={edicao.preco}
                                   aria-label="Preço"
                                   onChange={e => setEdicao({...edicao, preco: e.target.value})}/>
                        </div>
                        <div className="adm-x-acoes">
                            <button type="button" className="adm-x-btn adm-x-btn--gold adm-x-btn--sm" disabled={salvando}
                                    onClick={() => salvarEdicao(servico)}>Salvar
                            </button>
                            <button type="button" className="adm-x-btn adm-x-btn--sm"
                                    onClick={() => setEditandoId(null)}>Cancelar
                            </button>
                        </div>
                    </div> : <>
                        <div className="adm-x-servico-top">
                            <div>
                                <Scissors size={16}/>
                                <strong> {servico.nome}</strong>
                            </div>
                            <span className="adm-x-preco">{moeda(servico.preco)}</span>
                        </div>
                        <div className="adm-x-mini">
                            <span><Users size={12}/> <b>{fazem.length}</b> barbeiro(s)</span>
                            <span><b>{qtd}</b> atendimentos</span>
                            <span><TrendingUp size={12}/> <b>{moeda(receita)}</b></span>
                        </div>
                        {fazem.length === 0 && <span className="adm-x-badge adm-x-badge--alerta">Nenhum barbeiro faz este serviço</span>}
                        <div className="adm-x-acoes">
                            <button type="button" className="adm-x-btn adm-x-btn--sm"
                                    onClick={() => abrirEdicao(servico)}><Pencil size={13}/> Editar
                            </button>
                            <button type="button" className="adm-x-btn adm-x-btn--danger adm-x-btn--sm"
                                    onClick={() => excluir(servico)}><Trash2 size={13}/> Excluir
                            </button>
                        </div>
                    </>}
                </article>)}
            </div>
        </section>

        <section className="adm-x-card">
            <div className="adm-x-head">
                <div>
                    <h3>Quem faz o quê</h3>
                    <p className="adm-x-sub">Marque ou desmarque para liberar o serviço para cada barbeiro. Salva na hora.</p>
                </div>
            </div>
            {barbeiros.length === 0 || servicos.length === 0
                ? <p className="adm-x-sub">Cadastre barbeiros e serviços para montar a tabela.</p>
                : <div className="adm-x-tabela-wrap">
                    <table className="adm-x-tabela">
                        <thead>
                        <tr>
                            <th>Barbeiro</th>
                            {servicos.map(s => <th className="centro" key={s.id}>{s.nome}</th>)}
                        </tr>
                        </thead>
                        <tbody>
                        {barbeiros.map(b => <tr key={b.id}>
                            <td>{b.nome}</td>
                            {servicos.map(s => <td className="centro" key={s.id}>
                                <input type="checkbox" className="adm-x-check"
                                       aria-label={`${b.nome} faz ${s.nome}`}
                                       checked={(b.servicos || []).some(x => x.id === s.id)}
                                       onChange={() => alternarVinculo(b, s)}/>
                            </td>)}
                        </tr>)}
                        </tbody>
                    </table>
                </div>}
        </section>

        <section className="adm-x-card">
            <div className="adm-x-head">
                <div>
                    <h3>Novo serviço</h3>
                    <p className="adm-x-sub">Crie e já escolha quem vai fazer.</p>
                </div>
            </div>
            <form className="adm-x-stack" onSubmit={criar}>
                <div className="adm-x-edit-linha">
                    <label className="adm-x-field">Nome
                        <input required className="adm-x-input" value={novo.nome} placeholder="Ex.: Corte degradê"
                               onChange={e => setNovo({...novo, nome: e.target.value})}/>
                    </label>
                    <label className="adm-x-field">Preço
                        <input required className="adm-x-input" type="number" min="0.01" step="0.01"
                               value={novo.preco} onChange={e => setNovo({...novo, preco: e.target.value})}/>
                    </label>
                </div>
                {barbeiros.length > 0 && <div className="adm-x-field">Quem faz (opcional)
                    <div className="adm-x-mini">
                        {barbeiros.map(b => <label key={b.id}>
                            <input type="checkbox" className="adm-x-check" checked={novo.barbeiros.includes(b.id)}
                                   onChange={() => alternarNovo(b.id)}/> {b.nome}
                        </label>)}
                    </div>
                </div>}
                <div>
                    <button className="adm-x-btn adm-x-btn--gold" type="submit" disabled={salvando}>
                        <Plus size={15}/> {salvando ? 'Criando...' : 'Criar serviço'}
                    </button>
                </div>
            </form>
        </section>
    </div>;
}
