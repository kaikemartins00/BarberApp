import {useEffect, useState} from 'react';
import api from '../api/api.js';
import {plural, resumirCliente} from '../utils/resumoCliente.js';

const ESTILOS = `
.ficha-grade { display:grid; grid-template-columns:repeat(auto-fit, minmax(150px, 1fr)); gap:10px; margin:12px 0 }
.ficha-item { padding:12px 14px; border-radius:12px; background:var(--surface-2, #17171c); border:1px solid var(--line, #2a2a31) }
.ficha-item span { display:block; font-size:.78rem; color:var(--muted, #a1a1aa) }
.ficha-item strong { display:block; margin-top:2px; color:var(--text, #fff) }
`;

const mesAno = iso => new Date(`${iso}T12:00:00`).toLocaleDateString('pt-BR', {month: 'long', year: 'numeric'});

export default function FichaCliente({agendamentos}) {
    const [observacoes, setObservacoes] = useState('');
    const [carregado, setCarregado] = useState(false);
    const [erro, setErro] = useState('');
    const [mensagem, setMensagem] = useState('');
    const [salvando, setSalvando] = useState(false);

    useEffect(() => {
        let ativo = true;
        api.get('/cliente/painel/preferencias', {semRedirect: true})
            .then(r => {
                if (ativo) setObservacoes(r.data?.observacoes || '');
            })
            .catch(() => {
                if (ativo) setErro('Não foi possível carregar suas preferências.');
            })
            .finally(() => {
                if (ativo) setCarregado(true);
            });
        return () => {
            ativo = false;
        };
    }, []);

    async function salvar(e) {
        e.preventDefault();
        setSalvando(true);
        setErro('');
        setMensagem('');
        try {
            await api.put('/cliente/painel/preferencias', {observacoes});
            setMensagem('Preferências salvas.');
        } catch (e2) {
            setErro(e2.response?.data?.message || 'Não foi possível salvar.');
        } finally {
            setSalvando(false);
        }
    }

    const resumo = resumirCliente(agendamentos || []);

    return <section className="panel">
        <style>{ESTILOS}</style>
        <h2>Minha ficha de atendimento</h2>
        {resumo.concluidos === 0
            ? <p className="muted">Sua ficha vai sendo preenchida conforme você for atendido. Enquanto isso, conte
                para a gente como você gosta do corte.</p>
            : <div className="ficha-grade">
                <div className="ficha-item"><span>Cliente desde</span>
                    <strong>{resumo.clienteDesde ? mesAno(resumo.clienteDesde) : '—'}</strong></div>
                <div className="ficha-item"><span>Atendimentos</span>
                    <strong>{plural(resumo.concluidos, 'concluído', 'concluídos')}</strong></div>
                <div className="ficha-item"><span>Serviço favorito</span>
                    <strong>{resumo.servicoFavorito || '—'}</strong></div>
                <div className="ficha-item"><span>Barbeiro favorito</span>
                    <strong>{resumo.barbeiroFavorito?.nome || '—'}</strong></div>
                <div className="ficha-item"><span>Você volta a cada</span>
                    <strong>{resumo.frequenciaDias ? `cerca de ${plural(resumo.frequenciaDias, 'dia', 'dias')}` : '—'}</strong>
                </div>
            </div>}
        {erro && <p className="alert alert--erro">{erro}</p>}
        {mensagem && <p className="alert alert--ok">{mensagem}</p>}
        <form onSubmit={salvar}>
            <label className="field"><span>Minhas preferências</span>
                <textarea rows={3} maxLength={2000} value={observacoes} disabled={!carregado}
                          placeholder="Ex.: degradê baixo, tesoura no topo, barba aparada"
                          onChange={e => setObservacoes(e.target.value)}/>
            </label>
            <button className="btn btn--primary" disabled={salvando || !carregado}>
                {salvando ? 'Salvando...' : 'Salvar preferências'}</button>
        </form>
    </section>;
}
