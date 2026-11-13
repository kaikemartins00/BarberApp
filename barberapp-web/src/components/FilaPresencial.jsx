import {useCallback, useEffect, useState} from 'react';
import api from '../api/api.js';

const statusTexto = {
    AGUARDANDO: 'Aguardando',
    EM_ATENDIMENTO: 'Em atendimento',
    CONCLUIDO: 'Concluído',
    CANCELADO: 'Cancelado'
};

export default function FilaPresencial(
    {barbeiroId, modo = 'barbeiro'}) {

    const [itens, setItens] = useState([]);
    const [erro, setErro] = useState('');
    const [carregando, setCarregando] = useState(true);
    const [ocupado, setOcupado] = useState(null);
    const carregar = useCallback(async () => {

            try {
                const rota = modo === 'cliente' ? '/fila-presencial/minha' : `/fila-presencial/barbeiro/${barbeiroId}`;

                const {data} = await api.get(rota);

                setItens(Array.isArray(data) ? data : []);

                setErro('');
            } catch (e) {
                setErro(e.response?.data?.message || 'Não foi possível consultar a fila.');
            } finally {
                setCarregando(false);
            }
        },
        [barbeiroId, modo]);

    useEffect(() => {
            if (modo === 'cliente' || barbeiroId) carregar();
        },
        [carregar, barbeiroId, modo]);

    async function entrar() {

        setOcupado('entrar');

        try {
            await api.post(
                `/fila-presencial/entrar/${barbeiroId}`);
            await carregar();

        } catch (e) {
            setErro(e.response?.data?.message || 'Não foi possível entrar na fila.');
        } finally {
            setOcupado(null);
        }

    }

    async function mudar(id, status) {
        setOcupado(id);
        try {
            await api.patch(`/fila-presencial/${id}/status`, null,
                {params: {status}});
            await carregar();
        } catch (e) {
            setErro(e.response?.data?.message || 'Não foi possível atualizar a fila.');
        } finally {
            setOcupado(null);
        }
    }

    return <section className="panel">
        <div style={{display: 'flex', justifyContent: 'space-between', gap: 12, alignItems: 'center'}}><h2>Fila
            presencial</h2>
            <button className="btn btn--ghost" onClick={carregar}>Atualizar</button>
        </div>
        {modo === 'cliente' && barbeiroId &&
            <button className="btn btn--primary" disabled={ocupado !== null} onClick={entrar}>Entrar na fila</button>}
        {erro && <p className="alert alert--erro">{erro}</p>}
        {carregando && <p className="muted">Carregando fila...</p>}
        {!carregando && itens.length === 0 && <p className="muted">Nenhuma pessoa na fila.</p>}
        <div className="lista">{itens.map(item => <div key={item.id} className="lista__item">
            <div className="lista__info"><strong>{modo === 'cliente' ? 'Minha posição' : item.clienteNome}</strong><span
                className="muted small">{statusTexto[item.status] || item.status} · Posição {item.posicao || '—'} · {item.pessoasNaFrente ?? 0} à frente · Espera estimada: {item.esperaEstimadaMinutos ?? 0} min</span>
            </div>
            {modo !== 'cliente' && item.status === 'AGUARDANDO' &&
                <button className="btn btn--primary" disabled={ocupado !== null}
                        onClick={() => mudar(item.id, 'EM_ATENDIMENTO')}>Iniciar</button>}
            {modo !== 'cliente' && item.status === 'EM_ATENDIMENTO' &&
                <button className="btn btn--primary" disabled={ocupado !== null}
                        onClick={() => mudar(item.id, 'CONCLUIDO')}>Concluir</button>}
            {['AGUARDANDO', 'EM_ATENDIMENTO'].includes(item.status) &&
                <button className="btn btn--ghost" disabled={ocupado !== null}
                        onClick={() => mudar(item.id, 'CANCELADO')}>Cancelar</button>}
        </div>)}</div>
    </section>;
}
