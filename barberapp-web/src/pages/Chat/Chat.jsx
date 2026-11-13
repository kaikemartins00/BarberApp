import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../api/api.js';
import Navbar from '../../components/Navbar.jsx';
import { Title } from '../LoginECadastro/Style.js';
import { dadosRepetir, dataBr, hora, resumirCliente } from '../../utils/resumoCliente.js';

// Assistente de menu: responde com regras fixas e dados reais do sistema (serviços, preços,
// barbeiros e horários do próprio cliente). Não depende de IA externa, então nunca "cai".
// Agenda pelo chat com pagamento no atendimento; para Pix, leva à tela de agendamento.

const moeda = (v) => Number(v ?? 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
const horaAgora = () => new Date().toTimeString().slice(0, 5);
const isoDia = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
const rotuloDia = (iso) => {
    const d = new Date(`${iso}T12:00:00`);
    return `${d.toLocaleDateString('pt-BR', { weekday: 'short' }).replace('.', '')} ${dataBr(iso).slice(0, 5)}`;
};
const proximosDias = (n) => Array.from({ length: n }, (_, i) => {
    const d = new Date();
    d.setDate(d.getDate() + i);
    return isoDia(d);
});
const futuro = (dia, h) => new Date(`${dia}T${hora(h)}:00`).getTime() > Date.now();
const lista = (dados) => (Array.isArray(dados) ? dados : []);
const msgErro = (e, padrao) => e?.response?.data?.message || padrao;

const VOLTAR = { rotulo: 'Voltar ao menu', intencao: 'menu' };
const CHIP_IA = { rotulo: 'Assistente virtual (IA)', intencao: 'ia_inicio' };
const MENU = [
    { rotulo: 'Agendar pelo chat', intencao: 'ag_inicio' },
    { rotulo: 'Serviços e preços', intencao: 'servicos' },
    { rotulo: 'Nossos barbeiros', intencao: 'barbeiros' },
    { rotulo: 'Meu próximo horário', intencao: 'proximo' },
    { rotulo: 'Repetir meu último corte', intencao: 'ultimo' },
    { rotulo: 'Cancelar e reembolso', intencao: 'cancelar' },
    { rotulo: 'Pagamento (Pix)', intencao: 'pagamento' },
    { rotulo: 'Fila de espera', intencao: 'fila' },
    { rotulo: 'Falar com meu barbeiro', intencao: 'conversar' },
    { rotulo: 'Abrir chamado', intencao: 'chamado' },
];

const PALAVRAS = [
    ['cancelar', ['cancel', 'reembolso', 'estorno', 'devolu', 'bloque']],
    ['proximo', ['proximo', 'próximo', 'meu horario', 'meu horário', 'quando é']],
    ['ultimo', ['ultimo', 'último', 'repetir']],
    ['ag_inicio', ['agendar', 'marcar', 'reservar', 'horario', 'horário']],
    ['servicos', ['servico', 'serviço', 'preco', 'preço', 'valor', 'quanto', 'corte', 'barba', 'tabela']],
    ['barbeiros', ['barbeiro', 'profissional', 'quem corta']],
    ['pagamento', ['pix', 'pagar', 'pagamento', 'cobran']],
    ['fila', ['fila', 'vaga', 'lista de espera']],
    ['conversar', ['mensagem', 'conversar', 'falar', 'chat']],
    ['chamado', ['chamado', 'problema', 'erro', 'bug', 'suporte', 'ajuda humana']],
];

const detectar = (texto) => {
    const t = texto.toLowerCase();
    return PALAVRAS.find(([, chaves]) => chaves.some((c) => t.includes(c)))?.[0] ?? null;
};

const BOAS_VINDAS = {
    autor: 'bot',
    texto: 'Oi! Eu sou o assistente da barbearia. Escolha uma opção ou escreva sua dúvida.',
    acoes: MENU,
};

function Chat() {
    const navigate = useNavigate();
    const [mensagens, setMensagens] = useState([BOAS_VINDAS]);
    const [texto, setTexto] = useState('');
    const [pensando, setPensando] = useState(false);
    const [ia, setIa] = useState(false);
    const [statusIa, setStatusIa] = useState({ disponivel: false, restantes: 0 });
    const corpo = useRef(null);

    // rola só a caixa de mensagens, sem mexer na rolagem da página
    useEffect(() => {
        const el = corpo.current;
        if (el) el.scrollTo({ top: el.scrollHeight, behavior: 'smooth' });
    }, [mensagens, pensando]);

    // a IA só aparece no menu quando o Gemini está respondendo
    useEffect(() => {
        let ativo = true;
        api.get('/api/assistant/status', { semRedirect: true }).then((r) => {
            if (ativo) setStatusIa({ disponivel: !!r.data?.disponivel, restantes: r.data?.restantes ?? 0 });
        }).catch(() => {});
        return () => { ativo = false; };
    }, []);

    // saudação com o próximo horário do cliente
    useEffect(() => {
        let ativo = true;
        api.get('/agendamentos/meus', { semRedirect: true }).then((r) => {
            const prox = proximosHorarios(lista(r.data))[0];
            if (!ativo || !prox) return;
            setMensagens((m) => [{
                ...m[0],
                texto: `Oi! Eu sou o assistente da barbearia.\nSeu próximo horário: ${rotuloDia(prox.data)} às ${hora(prox.horario)}, ${prox.servicoNome} com ${prox.barbeiroNome}.\nEscolha uma opção ou escreva sua dúvida.`,
            }, ...m.slice(1)]);
        }).catch(() => {});
        return () => { ativo = false; };
    }, []);

    const proximosHorarios = (ags) => ags
        .filter((a) => ['PENDENTE', 'CONFIRMADO'].includes(a.status) && futuro(a.data, a.horario))
        .sort((a, b) => `${a.data} ${a.horario}`.localeCompare(`${b.data} ${b.horario}`));

    // horários já ocupados (futuros) do barbeiro em um dia: indicam que ele atende nesse dia mas está lotado
    const ocupadosNoDia = (barbeiro, dia) => lista(barbeiro?.horarios).filter((h) => {
        const dh = Array.isArray(h)
            ? `${h[0]}-${String(h[1]).padStart(2, '0')}-${String(h[2]).padStart(2, '0')}T${String(h[3] ?? 0).padStart(2, '0')}:${String(h[4] ?? 0).padStart(2, '0')}`
            : String(h);
        return dh.slice(0, 10) === dia && futuro(dia, dh.slice(11, 16));
    }).length;

    // só mostra os dias em que o barbeiro atende: com vaga livre, ou lotado (aí dá para entrar na fila de espera)
    const diasDisponiveis = async (dados) => {
        const [barbs, ...livres] = await Promise.all([
            api.get('/barbeiros', { semRedirect: true }),
            ...proximosDias(7).map((d) => api.get('/agendamentos/horarios-livres', { params: { barbeiroId: dados.barbeiroId, data: d } })),
        ]);
        const barbeiro = lista(barbs.data).find((b) => b.id === dados.barbeiroId);
        const acoes = [];
        proximosDias(7).forEach((d, i) => {
            const livre = lista(livres[i].data).map(hora).some((h) => futuro(d, h));
            if (livre) acoes.push({ rotulo: rotuloDia(d), intencao: 'ag_dia', dados: { ...dados, data: d } });
            else if (ocupadosNoDia(barbeiro, d)) acoes.push({ rotulo: `${rotuloDia(d)} (lotado)`, intencao: 'ag_dia', dados: { ...dados, data: d } });
        });
        if (!acoes.length) {
            return { texto: `${dados.barbeiroNome} não tem dias disponíveis nos próximos 7 dias.`, acoes: [VOLTAR] };
        }
        return { texto: `Para qual dia? (${dados.servicoNome} com ${dados.barbeiroNome})`, acoes: [...acoes, VOLTAR] };
    };

    async function responder(intencao, dados) {
        switch (intencao) {
            case 'menu':
                return { texto: 'Como posso ajudar?', acoes: MENU };

            case 'ia_inicio':
                return {
                    texto: `Você está falando com o assistente virtual (IA). As respostas são geradas por IA e podem errar; para algo importante, fale com o barbeiro. Ele consulta serviços, preços e horários, mas não agenda: para isso use o menu. Você tem ${statusIa.restantes} mensagens hoje.\nPode escrever sua pergunta.`,
                    acoes: [VOLTAR],
                };

            case 'servicos': {
                const { data } = await api.get('/servicos');
                const itens = lista(data).map((s) => `• ${s.nome} — ${moeda(s.preco)}`).join('\n');
                return {
                    texto: itens ? `Nossos serviços e preços:\n${itens}` : 'Ainda não há serviços cadastrados.',
                    acoes: [{ rotulo: 'Agendar pelo chat', intencao: 'ag_inicio' }, VOLTAR],
                };
            }

            case 'barbeiros': {
                const { data } = await api.get('/barbeiros');
                const itens = lista(data).map((b) => {
                    const servicos = lista(b.servicos).map((s) => `${s.nome} (${moeda(s.preco)})`).join(', ');
                    return `• ${b.nome}${servicos ? ` — ${servicos}` : ''}`;
                }).join('\n');
                return {
                    texto: itens ? `Nossos barbeiros:\n${itens}` : 'Ainda não há barbeiros cadastrados.',
                    acoes: [{ rotulo: 'Agendar pelo chat', intencao: 'ag_inicio' }, VOLTAR],
                };
            }

            case 'proximo': {
                const { data } = await api.get('/agendamentos/meus');
                const prox = proximosHorarios(lista(data))[0];
                return prox
                    ? {
                        texto: `Seu próximo horário é ${rotuloDia(prox.data)} às ${hora(prox.horario)}: ${prox.servicoNome} com ${prox.barbeiroNome} (${moeda(prox.servicoPreco)}).`,
                        acoes: [{ rotulo: 'Ir para Meus horários', ir: '/histAgendamento' }, VOLTAR],
                    }
                    : { texto: 'Você não tem horário marcado.', acoes: [{ rotulo: 'Agendar pelo chat', intencao: 'ag_inicio' }, VOLTAR] };
            }

            case 'ultimo': {
                const [meus, barbeiros] = await Promise.all([api.get('/agendamentos/meus'), api.get('/barbeiros')]);
                const ultimo = resumirCliente(lista(meus.data)).ultimo;
                if (!ultimo) {
                    return { texto: 'Você ainda não tem atendimento concluído para repetir.', acoes: [{ rotulo: 'Agendar pelo chat', intencao: 'ag_inicio' }, VOLTAR] };
                }
                const repetir = dadosRepetir(ultimo, lista(barbeiros.data))?.repetir;
                const acoes = [];
                if (repetir) {
                    acoes.push({
                        rotulo: 'Agendar igual', intencao: 'ag_barbeiro',
                        dados: { ...repetir, servicoNome: ultimo.servicoNome, barbeiroNome: ultimo.barbeiroNome, preco: ultimo.servicoPreco },
                    });
                }
                acoes.push({ rotulo: 'Escolher outro', intencao: 'ag_inicio' }, VOLTAR);
                return { texto: `Seu último atendimento foi ${ultimo.servicoNome} com ${ultimo.barbeiroNome}, em ${dataBr(ultimo.data)}.`, acoes };
            }

            case 'cancelar': {
                const [meus, canc] = await Promise.all([
                    api.get('/agendamentos/meus'),
                    api.get('/agendamentos/meus/cancelamentos', { semRedirect: true }).catch(() => ({ data: null })),
                ]);
                const regra = 'Cancelando com 2 horas ou mais de antecedência, o Pix é devolvido na hora. Com menos de 2 horas não há reembolso. Cada cliente pode cancelar até 3 vezes por mês; no 3º cancelamento a conta fica bloqueada por 7 dias para novos agendamentos.';
                const prox = proximosHorarios(lista(meus.data))[0];
                let extra = '';
                if (prox) {
                    const horas = (new Date(`${prox.data}T${hora(prox.horario)}:00`).getTime() - Date.now()) / 3600000;
                    extra += `\n\nSeu próximo horário é ${rotuloDia(prox.data)} às ${hora(prox.horario)}. ` +
                        (horas >= 2 ? 'Se cancelar agora, o Pix é reembolsado.' : 'Faltam menos de 2 horas, então não haveria reembolso.');
                }
                if (canc.data) extra += `\nVocê já usou ${canc.data.usados} de ${canc.data.limite} cancelamentos.`;
                return { texto: regra + extra, acoes: [{ rotulo: 'Ir para Meus horários', ir: '/histAgendamento' }, VOLTAR] };
            }

            case 'pagamento':
                return {
                    texto: 'Você pode pagar por Pix (QR Code ou "copia e cola", confirmado na hora) ou no atendimento, direto com o barbeiro.',
                    acoes: [VOLTAR],
                };
            case 'fila':
                return {
                    texto: 'Se o horário que você quer já está ocupado, entre na fila de espera dele. Se alguém cancelar, você é avisado. Você não pode entrar na fila de um dia em que já tem horário marcado.',
                    acoes: [VOLTAR],
                };
            case 'conversar': {
                const [bs, ags] = await Promise.all([api.get('/barbeiros'), api.get('/agendamentos/meus')]);
                const meus = new Set(lista(ags.data).filter((a) => a.status !== 'CANCELADO').map((a) => a.barbeiroNome));
                const nomes = lista(bs.data).map((b) => b.nome).filter((n) => meus.has(n));
                return nomes.length
                    ? { texto: `Você pode conversar com: ${nomes.join(', ')}.`, acoes: [{ rotulo: 'Abrir mensagens', ir: '/mensagens' }, VOLTAR] }
                    : { texto: 'Você só pode conversar com o barbeiro depois de agendar um horário com ele.', acoes: [{ rotulo: 'Agendar pelo chat', intencao: 'ag_inicio' }, VOLTAR] };
            }
            case 'chamado':
                return {
                    texto: 'Para problemas no site, no pagamento ou no código de verificação, abra um chamado. Você recebe um e-mail a cada atualização.',
                    acoes: [{ rotulo: 'Abrir chamado', ir: '/problemas' }, VOLTAR],
                };

            // ---------- agendamento guiado ----------
            case 'ag_inicio': {
                const { data } = await api.get('/barbeiros');
                const servicos = new Map();
                lista(data).forEach((b) => lista(b.servicos).forEach((s) => servicos.set(s.id, s)));
                if (!servicos.size) return { texto: 'Ainda não há serviços disponíveis para agendar.', acoes: [VOLTAR] };
                return {
                    texto: 'Qual serviço você quer?',
                    acoes: [...Array.from(servicos.values()).map((s) => ({
                        rotulo: `${s.nome} · ${moeda(s.preco)}`, intencao: 'ag_servico',
                        dados: { servicoId: s.id, servicoNome: s.nome, preco: s.preco },
                    })), VOLTAR],
                };
            }

            case 'ag_servico': {
                const { data } = await api.get('/barbeiros');
                const barbeiros = lista(data).filter((b) => lista(b.servicos).some((s) => s.id === dados.servicoId));
                if (!barbeiros.length) return { texto: 'Nenhum barbeiro faz esse serviço agora.', acoes: [VOLTAR] };
                if (barbeiros.length === 1) {
                    return await diasDisponiveis({ ...dados, barbeiroId: barbeiros[0].id, barbeiroNome: barbeiros[0].nome });
                }
                return {
                    texto: 'Com qual barbeiro?',
                    acoes: [...barbeiros.map((b) => ({
                        rotulo: b.nome, intencao: 'ag_barbeiro', dados: { ...dados, barbeiroId: b.id, barbeiroNome: b.nome },
                    })), VOLTAR],
                };
            }

            case 'ag_barbeiro':
                return await diasDisponiveis(dados);

            case 'ag_dia': {
                const { data } = await api.get('/agendamentos/horarios-livres', { params: { barbeiroId: dados.barbeiroId, data: dados.data } });
                const horarios = lista(data).map(hora).filter((h) => futuro(dados.data, h)).slice(0, 12);
                if (!horarios.length) {
                    const outros = await diasDisponiveis(dados);
                    return {
                        texto: `${dados.barbeiroNome} está lotado em ${rotuloDia(dados.data)}. Você pode entrar na fila de espera: se alguém cancelar, você é avisado. Ou escolha outro dia.`,
                        acoes: [
                            { rotulo: 'Entrar na fila de espera', ir: '/agendamento', estado: { repetir: { barbeiroId: dados.barbeiroId, servicoId: dados.servicoId, data: dados.data } } },
                            ...outros.acoes.filter((a) => a.intencao === 'ag_dia' && a.dados.data !== dados.data),
                            VOLTAR,
                        ],
                    };
                }
                return {
                    texto: `Horários livres em ${rotuloDia(dados.data)} com ${dados.barbeiroNome}:`,
                    acoes: [...horarios.map((h) => ({ rotulo: h, intencao: 'ag_hora', dados: { ...dados, horario: h } })),
                        { rotulo: 'Outro dia', intencao: 'ag_barbeiro', dados }, VOLTAR],
                };
            }

            case 'ag_hora':
                return {
                    texto: `${dados.servicoNome} com ${dados.barbeiroNome}, ${rotuloDia(dados.data)} às ${dados.horario}: ${moeda(dados.preco)}.\nAntes de continuar, leia a política de cancelamento e reembolso: cancelando com 2 horas ou mais de antecedência, o Pix é devolvido; com menos de 2 horas, não há reembolso; cada cliente pode cancelar até 3 vezes por mês.`,
                    acoes: [
                        { rotulo: 'Li e concordo, continuar', intencao: 'ag_pagamento', dados },
                        { rotulo: 'Ler a política completa', ir: '/reembolso' },
                        VOLTAR,
                    ],
                };

            case 'ag_pagamento':
                return {
                    texto: `Como prefere pagar ${moeda(dados.preco)}?`,
                    acoes: [
                        { rotulo: 'Pagar no atendimento', intencao: 'ag_presencial', dados },
                        { rotulo: 'Pagar com Pix', intencao: 'ag_pix', dados },
                        VOLTAR,
                    ],
                };

            case 'ag_presencial': {
                try {
                    await api.post('/agendamentos/checkout', {
                        agendamento: { barbeiroId: dados.barbeiroId, servicoId: dados.servicoId, data: dados.data, horario: dados.horario },
                        metodo: 'PRESENCIAL',
                    });
                } catch (e) {
                    return { erro: true, texto: msgErro(e, 'Não consegui marcar esse horário. Tente outro.'), acoes: [{ rotulo: 'Escolher outro horário', intencao: 'ag_barbeiro', dados }, VOLTAR] };
                }
                return {
                    texto: `Pronto! Horário marcado: ${rotuloDia(dados.data)} às ${dados.horario}, ${dados.servicoNome} com ${dados.barbeiroNome}. O pagamento (${moeda(dados.preco)}) é no atendimento. Você recebe o comprovante por e-mail.`,
                    acoes: [{ rotulo: 'Ir para Meus horários', ir: '/histAgendamento' }, VOLTAR],
                };
            }

            case 'ag_pix':
                return {
                    texto: 'O Pix é finalizado na tela de agendamento, onde aparece o QR Code. Já deixo serviço, barbeiro, dia e horário escolhidos; é só conferir e continuar.',
                    acoes: [
                        { rotulo: 'Continuar com Pix', ir: '/agendamento', estado: { repetir: { barbeiroId: dados.barbeiroId, servicoId: dados.servicoId, data: dados.data, horario: dados.horario } } },
                        { rotulo: 'Pagar no atendimento', intencao: 'ag_presencial', dados },
                        VOLTAR,
                    ],
                };

            default:
                return {
                    texto: 'Não entendi muito bem. Escolha uma das opções abaixo ou abra um chamado para falar com uma pessoa.',
                    acoes: MENU,
                };
        }
    }

    async function perguntarIa(fala) {
        setMensagens((m) => [...m, { autor: 'eu', texto: fala, hora: horaAgora() }]);
        setPensando(true);
        try {
            const r = await api.post('/api/assistant', { mensagem: fala });
            const resposta = typeof r.data === 'string' ? r.data : r.data?.resposta ?? r.data?.message ?? '';
            setStatusIa((s) => ({ ...s, restantes: Math.max(0, s.restantes - 1) }));
            setMensagens((m) => [...m, { autor: 'bot', hora: horaAgora(), texto: resposta || 'Sem resposta.', acoes: [VOLTAR] }]);
        } catch (e) {
            setIa(false);
            setStatusIa((s) => ({ ...s, disponivel: e?.response?.status === 400 ? s.disponivel : false }));
            setMensagens((m) => [...m, {
                autor: 'bot', erro: true, hora: horaAgora(), acoes: MENU,
                texto: msgErro(e, 'O assistente virtual está indisponível agora. O menu continua funcionando.'),
            }]);
        } finally {
            setPensando(false);
        }
    }

    async function perguntar(intencao, fala, dados) {
        if (pensando) return;
        if (intencao === 'ia_inicio') setIa(true);
        if (intencao === 'menu') setIa(false);
        setMensagens((m) => [...m, { autor: 'eu', texto: fala, hora: horaAgora() }]);
        setPensando(true);
        try {
            const r = await responder(intencao, dados);
            setMensagens((m) => [...m, { autor: 'bot', hora: horaAgora(), ...r }]);
        } catch (e) {
            setMensagens((m) => [...m, {
                autor: 'bot', erro: true, hora: horaAgora(),
                texto: msgErro(e, 'Não consegui buscar essa informação agora. Tente de novo em instantes.'),
                acoes: [VOLTAR],
            }]);
        } finally {
            setPensando(false);
        }
    }

    function enviar(event) {
        event.preventDefault();
        const fala = texto.trim();
        if (!fala || pensando) return;
        setTexto('');
        if (ia) perguntarIa(fala);
        else perguntar(detectar(fala), fala);
    }

    function aoDigitar(event) {
        if (event.key === 'Enter' && !event.shiftKey) enviar(event);
    }

    function acionar(acao) {
        if (acao.ir) navigate(acao.ir, acao.estado ? { state: acao.estado } : undefined);
        else perguntar(acao.intencao, acao.rotulo, acao.dados);
    }

    return (
        <>
            <Navbar />
            <main className="page">
                <header className="page__head">
                    <Title>Assistente</Title>
                    <p className="muted">Tire dúvidas e agende pelos botões.</p>
                </header>

                <section className="panel">
                    <h2>Assistente da barbearia</h2>

                    <div className="msgs__corpo" ref={corpo}>
                        {mensagens.map((m, i) => (
                            <div key={i}
                                 className={`balao ${m.autor === 'eu' ? 'balao--minha' : ''} ${m.erro ? 'balao--erro' : ''}`}
                                 style={{ whiteSpace: 'pre-line' }}>
                                {m.texto}
                                {m.acoes && i === mensagens.length - 1 && (
                                    <div className="chips" style={{ marginTop: 10 }}>
                                        {(m.acoes === MENU && statusIa.disponivel ? [...MENU, CHIP_IA] : m.acoes).map((a) => (
                                            <button key={`${a.rotulo}-${a.intencao ?? a.ir}`} type="button" className="chip" onClick={() => acionar(a)}>
                                                {a.rotulo}
                                            </button>
                                        ))}
                                    </div>
                                )}
                                {m.hora && <span className="balao__hora">{m.hora}</span>}
                            </div>
                        ))}
                        {pensando && <div className="balao balao--digitando">Digitando...</div>}
                    </div>

                    <form className="msgs__form" onSubmit={enviar}>
                        <textarea rows={1} value={texto} placeholder={ia ? 'Pergunte ao assistente virtual' : 'Escreva sua dúvida'}
                                  maxLength={300}
                                  onChange={(e) => setTexto(e.target.value)} onKeyDown={aoDigitar} />
                        <button type="submit" className="btn btn--primary" disabled={pensando || !texto.trim()}>
                            Enviar
                        </button>
                    </form>
                </section>
            </main>
        </>
    );
}

export default Chat;
