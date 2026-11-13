const DIA_EM_MS = 24 * 60 * 60 * 1000;

const contar = (lista, campo) => {
    const mapa = new Map();
    lista.forEach(item => mapa.set(item[campo], (mapa.get(item[campo]) || 0) + 1));
    return mapa;
};

const maisFrequente = mapa => {
    let melhor = null;
    mapa.forEach((quantidade, nome) => {
        if (!melhor || quantidade > melhor.quantidade) melhor = {nome, quantidade};
    });
    return melhor;
};

const meioDia = dia => new Date(`${dia}T12:00:00`).getTime();

export const plural = (n, singular, pluralTexto) => `${n} ${n === 1 ? singular : pluralTexto}`;

export const hora = valor => String(valor || '').slice(0, 5);

export const dataBr = iso => String(iso || '').slice(0, 10).split('-').reverse().join('/');

export function resumirCliente(agendamentos) {
    const concluidos = agendamentos.filter(a => a.status === 'CONCLUIDO');
    const cancelados = agendamentos.filter(a => a.status === 'CANCELADO');

    const barbeiroTop = maisFrequente(contar(concluidos, 'barbeiroNome'));
    const servicoTop = maisFrequente(contar(concluidos, 'servicoNome'));

    const ordenados = [...concluidos].sort((a, b) =>
        `${a.data} ${a.horario}`.localeCompare(`${b.data} ${b.horario}`));
    const ultimo = ordenados.length ? ordenados[ordenados.length - 1] : null;

    let frequenciaDias = null;
    if (ordenados.length >= 2) {
        const total = meioDia(ordenados[ordenados.length - 1].data) - meioDia(ordenados[0].data);
        frequenciaDias = Math.max(1, Math.round(total / DIA_EM_MS / (ordenados.length - 1)));
    }

    const datas = agendamentos.map(a => a.data).filter(Boolean).sort();

    return {
        concluidos: concluidos.length,
        cancelados: cancelados.length,
        barbeiroFavorito: barbeiroTop && {
            nome: barbeiroTop.nome,
            concluidos: barbeiroTop.quantidade,
            cancelados: cancelados.filter(a => a.barbeiroNome === barbeiroTop.nome).length
        },
        servicoFavorito: servicoTop ? servicoTop.nome : null,
        ultimo,
        frequenciaDias,
        clienteDesde: datas.length ? datas[0] : null
    };
}

export function dadosRepetir(agendamento, barbeiros) {
    const barbeiro = (barbeiros || []).find(b => b.nome === agendamento.barbeiroNome);
    const servico = barbeiro?.servicos?.find(s => s.nome === agendamento.servicoNome);
    return barbeiro && servico ? {repetir: {barbeiroId: barbeiro.id, servicoId: servico.id}} : null;
}
