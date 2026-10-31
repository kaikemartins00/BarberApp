import { useEffect, useRef, useState } from 'react';
import api from '../../api/api.js';
import Navbar from '../../components/Navbar.jsx';
import { Title } from '../LoginECadastro/Style.js';

// POST { mensagem } -> resposta do assistente (Gemini via LangChain4j).
// Aceita o retorno como texto puro ou como { resposta } / { message }.
const ENDPOINT_CHAT = '/api/assistant';

const BOAS_VINDAS = {
    autor: 'ia',
    texto: 'Oi! Eu sou o assistente da barbearia. Posso ajudar com serviços, horários e agendamentos.',
};

const extrairTexto = (dados) =>
    typeof dados === 'string' ? dados : dados?.resposta ?? dados?.message ?? '';

// "2026-10-25T14:30:12.123" -> "14:30"
const horaDe = (dh) => String(dh).slice(11, 16);
const horaAgora = () => new Date().toTimeString().slice(0, 5);

function Chat() {
    const [mensagens, setMensagens] = useState([BOAS_VINDAS]);
    const [texto, setTexto] = useState('');
    const [digitando, setDigitando] = useState(false);
    const corpo = useRef(null);

    // rola só a caixa de mensagens, sem mexer na rolagem da página
    useEffect(() => {
        const el = corpo.current;
        if (el) el.scrollTo({ top: el.scrollHeight, behavior: 'smooth' });
    }, [mensagens, digitando]);

    useEffect(() => {
        api.get('/api/assistant/historico')
            .then((r) => {
                const antigas = r.data.map((h) => ({
                    autor: h.role === 'user' ? 'eu' : 'ia',
                    texto: h.msg,
                    hora: horaDe(h.msgDate),
                }));
                setMensagens([BOAS_VINDAS, ...antigas]);
            })
            .catch(() => {});
    }, []);

    async function enviar(event) {
        event.preventDefault();
        const pergunta = texto.trim();
        if (!pergunta || digitando) return;

        setMensagens((m) => [...m, { autor: 'eu', texto: pergunta, hora: horaAgora() }]);
        setTexto('');
        setDigitando(true);

        try {
            const r = await api.post(ENDPOINT_CHAT, { mensagem: pergunta });
            setMensagens((m) => [...m, {
                autor: 'ia',
                texto: extrairTexto(r.data) || 'Sem resposta.',
                hora: horaAgora(),
            }]);
        } catch (error) {
            const aviso = error.response?.data?.message || 'Não consegui responder agora. Tente de novo em instantes.';
            setMensagens((m) => [...m, { autor: 'ia', texto: aviso, erro: true, hora: horaAgora() }]);
        } finally {
            setDigitando(false);
        }
    }

    function aoDigitar(event) {
        if (event.key === 'Enter' && !event.shiftKey) enviar(event);
    }

    return (
        <>
            <Navbar />
            <main className="page">
                <header className="page__head">
                    <Title>Assistente</Title>
                    <p className="muted">Tire dúvidas e agende conversando.</p>
                </header>

                <section className="panel">
                    <h2>Assistente da barbearia</h2>

                    <div className="msgs__corpo" ref={corpo}>
                        {mensagens.map((m, i) => (
                            <div key={i}
                                 className={`balao ${m.autor === 'eu' ? 'balao--minha' : ''} ${m.erro ? 'balao--erro' : ''}`}>
                                {m.texto}
                                {m.hora && <span className="balao__hora">{m.hora}</span>}
                            </div>
                        ))}
                        {digitando && <div className="balao balao--digitando">Digitando...</div>}
                    </div>

                    <form className="msgs__form" onSubmit={enviar}>
                        <textarea rows={1} value={texto} placeholder="Escreva sua mensagem"
                                  maxLength={500}
                                  onChange={(e) => setTexto(e.target.value)} onKeyDown={aoDigitar} />
                        <button type="submit" className="btn btn--primary"
                                disabled={digitando || !texto.trim()}>
                            Enviar
                        </button>
                    </form>
                </section>
            </main>
        </>
    );
}

export default Chat;