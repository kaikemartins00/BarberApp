import {useEffect, useState} from 'react';
import api from '../api/api.js';
import {lerSessao} from '../auth/session.js';

const moeda = valor =>
    Number(valor || 0)
        .toLocaleString('pt-BR'
            , {style: 'currency', currency: 'BRL'}
        );

export default function FinanceiroAdmin() {

    const sessao = lerSessao();
    const autorizado = sessao?.roles?.some(r => (typeof r === 'string' ? r : r.authority) === 'ROLE_ADMIN');
    const [dados, setDados] = useState(null);
    const [erro, setErro] = useState('');

    useEffect(() => {

        if (!autorizado) return;
        api.get('/financeiro/admin/resumo').then(r => setDados(r.data)).catch(e => setErro(e.response?.data?.message || 'Não foi possível consultar o financeiro.'));
    }, [autorizado]);

    if (!autorizado) return null;

    return <section className="panel"><h2>Financeiro da barbearia</h2>{erro &&
        <p className="alert alert--erro">{erro}</p>}{dados && <div className="financeiro-grid">
        <div><span className="muted small">Recebido</span><strong>{moeda(dados.totalBruto)}</strong></div>
        <div><span className="muted small">Receita da loja (20%)</span><strong>{moeda(dados.taxaLoja)}</strong></div>
        <div><span
            className="muted small">Repasse aos barbeiros (80%)</span><strong>{moeda(dados.totalLiquido)}</strong></div>
        <div><span
            className="muted small">Pagamentos confirmados</span><strong>{dados.quantidadePagamentos ?? 0}</strong>
        </div>
    </div>}</section>;
}
