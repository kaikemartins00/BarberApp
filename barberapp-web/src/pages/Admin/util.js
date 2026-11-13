export const dois = n => String(n).padStart(2, '0');

export const paraISO = d => `${d.getFullYear()}-${dois(d.getMonth() + 1)}-${dois(d.getDate())}`;

export const hojeISO = () => paraISO(new Date());

export const deISO = iso => {
    const [a, m, d] = String(iso).slice(0, 10).split('-').map(Number);
    return new Date(a, m - 1, d);
};

export const somarDias = (iso, dias) => {
    const d = deISO(iso);
    d.setDate(d.getDate() + dias);
    return paraISO(d);
};

export const moeda = valor =>
    Number(valor || 0).toLocaleString('pt-BR', {style: 'currency', currency: 'BRL'});

export const dataBR = iso => String(iso || '').slice(0, 10).split('-').reverse().join('/');

export const dataHoraBR = valor => {
    const [data, hora] = String(valor || '').slice(0, 16).split('T');
    return hora ? `${dataBR(data)} às ${hora}` : dataBR(data);
};

export const variacao = (atual, anterior) => {
    if (!anterior) return atual > 0 ? null : 0;
    return ((atual - anterior) / anterior) * 100;
};

export const textoVariacao = pct => {
    if (pct == null) return 'sem base de comparação';
    const sinal = pct > 0 ? '+' : '';
    return `${sinal}${pct.toFixed(1).replace('.', ',')}%`;
};

export const valorAg = a => Number(a.servicoPreco || 0);

export const ehConcluido = a => a.status === 'CONCLUIDO';

export const mensagemErro = (error, padrao) => error.response?.data?.message || padrao;
