import {useEffect, useRef, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {MessageSquare} from 'lucide-react';
import api from '../../api/api.js';
import './MensagemFlutuante.css';

const naoLidasDe = c => Math.max(0, Number(c.naoLidas ?? c.mensagensNaoLidas ?? c.quantidadeNaoLidas ?? 0) || 0);

export default function MensagemFlutuante() {
    const navigate = useNavigate();
    const [total, setTotal] = useState(0);
    const [previa, setPrevia] = useState(null);
    const anterior = useRef(null);
    const timerPrevia = useRef(null);

    useEffect(() => {
        let ativo = true;

        async function atualizar() {
            try {
                const {data} = await api.get('/conversas', {semRedirect: true});
                if (!ativo) return;
                const lista = Array.isArray(data) ? data : [];
                const soma = lista.reduce((s, c) => s + naoLidasDe(c), 0);
                if (anterior.current !== null && soma > anterior.current) {
                    const nova = lista.find(c => naoLidasDe(c) > 0);
                    if (nova) {
                        setPrevia({nome: nova.interlocutorNome, texto: nova.ultimaMensagem});
                        window.clearTimeout(timerPrevia.current);
                        timerPrevia.current = window.setTimeout(() => setPrevia(null), 7000);
                    }
                }
                anterior.current = soma;
                setTotal(soma);
            } catch {
                // tenta de novo no próximo ciclo
            }
        }

        atualizar();
        const timer = window.setInterval(atualizar, 8000);
        window.addEventListener('barberapp:mensagens-lidas', atualizar);
        return () => {
            ativo = false;
            window.clearInterval(timer);
            window.clearTimeout(timerPrevia.current);
            window.removeEventListener('barberapp:mensagens-lidas', atualizar);
        };
    }, []);

    if (total <= 0) return null;

    return (
        <div className="mf-wrap">
            {previa && (
                <button type="button" className="mf-previa" onClick={() => navigate('/mensagens')}>
                    <strong>{previa.nome}</strong>
                    <span>{previa.texto}</span>
                </button>
            )}
            <button type="button" className="mf-botao" onClick={() => navigate('/mensagens')}
                    aria-label={`${total} mensagens não lidas`} title="Abrir mensagens">
                <MessageSquare size={24}/>
                <span className="mf-badge">{total > 99 ? '99+' : total}</span>
            </button>
        </div>
    );
}
