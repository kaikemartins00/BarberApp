import {useEffect, useState} from 'react';
import {Headphones} from 'lucide-react';
import api from '../../api/api.js';
import './NotificacaoMensagens.css';

export default function NotificacaoChamados() {
    const [abertos, setAbertos] = useState(0);

    useEffect(() => {
        let ativo = true;

        async function atualizar() {
            try {
                const {data} = await api.get('/chamados', {semRedirect: true});
                if (!ativo) return;
                setAbertos((Array.isArray(data) ? data : []).filter(c => c.status === 'ABERTO').length);
            } catch {
            }
        }

        atualizar();
        const timer = window.setInterval(atualizar, 10000);
        window.addEventListener('barberapp:chamados-atualizados', atualizar);
        return () => {
            ativo = false;
            window.clearInterval(timer);
            window.removeEventListener('barberapp:chamados-atualizados', atualizar);
        };
    }, []);

    return <span className="nm-link"><Headphones size={18}/><span>Chamados</span>{abertos > 0 &&
        <span className="nm-badge" aria-label={`${abertos} chamados abertos`}>{abertos > 99 ? '99+' : abertos}</span>}</span>;
}
