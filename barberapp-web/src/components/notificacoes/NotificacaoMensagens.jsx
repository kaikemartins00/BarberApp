import {useEffect, useState} from 'react';
import {MessageSquare} from 'lucide-react';
import api from '../../api/api.js';
import './NotificacaoMensagens.css';

export default function NotificacaoMensagens({className = ''}) {
    const [naoLidas, setNaoLidas] = useState(0);

    useEffect(() => {
        let ativo = true;

        async function atualizar() {
            try {
                const {data} = await api.get('/conversas');
                if (!ativo) return;
                const total = (Array.isArray(data) ? data : []).reduce((soma, conversa) => soma + Math.max(0, Number(conversa.naoLidas ?? conversa.mensagensNaoLidas ?? conversa.quantidadeNaoLidas ?? 0) || 0), 0);
                setNaoLidas(total);
            } catch {
                if (ativo) setNaoLidas(0);
            }
        }

        atualizar();
        const timer = window.setInterval(atualizar, 8000);
        const visibilidade = () => {
            if (!document.hidden) atualizar();
        };
        document.addEventListener('visibilitychange', visibilidade);
        window.addEventListener('barberapp:mensagens-lidas', atualizar);
        return () => {
            ativo = false;
            window.clearInterval(timer);
            document.removeEventListener('visibilitychange', visibilidade);
            window.removeEventListener('barberapp:mensagens-lidas', atualizar);
        };
    }, []);

    return <span className={`nm-link ${className}`}><MessageSquare size={18}/><span>Mensagens</span>{naoLidas > 0 &&
        <span className="nm-badge"
              aria-label={`${naoLidas} mensagens não lidas`}>{naoLidas > 99 ? '99+' : naoLidas}</span>}</span>;
}
