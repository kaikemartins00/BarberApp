import {useCallback, useEffect, useState} from 'react';
import api from '../api/api.js';
import ConfirmDialog from './ConfirmDialog.jsx';

const formatarData = iso => String(iso || '').slice(0, 10).split('-').reverse().join('/');

export default function ClientesBloqueados() {
    const [lista, setLista] = useState(null);
    const [alvo, setAlvo] = useState(null);
    const [processando, setProcessando] = useState(false);
    const [erro, setErro] = useState('');
    const [aviso, setAviso] = useState('');

    const carregar = useCallback(() => api.get('/admin/clientes/bloqueados')
        .then(r => setLista(r.data || []))
        .catch(() => setErro('Não foi possível carregar os clientes bloqueados.')), []);

    useEffect(() => {
        carregar();
    }, [carregar]);

    async function desbloquear() {
        setProcessando(true);
        setErro('');
        try {
            await api.post(`/admin/clientes/${alvo.id}/desbloquear`);
            setAviso(`${alvo.nome} foi desbloqueado e recebeu um e-mail avisando.`);
            setAlvo(null);
            await carregar();
        } catch (e) {
            setErro(e.response?.data?.message || 'Não foi possível desbloquear o cliente.');
        } finally {
            setProcessando(false);
        }
    }

    return <section className="adm-card">
        <h3>Clientes bloqueados</h3>
        <p className="adm-muted">Clientes que atingiram o limite de cancelamentos do mês. Só o administrador pode
            liberar.</p>
        {erro && <div className="adm-aviso adm-aviso--erro">{erro}</div>}
        {aviso && <div className="adm-aviso adm-aviso--ok">{aviso}</div>}
        {lista && lista.length === 0 && <div className="adm-vazio">Nenhum cliente bloqueado no momento.</div>}
        {(lista || []).map(c => <div className="adm-summary-line" key={c.id} style={{gap: 12, flexWrap: 'wrap'}}>
            <div style={{flex: 1, minWidth: 200}}>
                <strong>{c.nome}</strong><br/>
                <small>{c.email} · {c.telefone} · bloqueado até {formatarData(c.bloqueadoAte)}</small>
            </div>
            <button type="button" className="adm-btn adm-btn--gold" onClick={() => setAlvo(c)}>Desbloquear</button>
        </div>)}
        <ConfirmDialog aberto={!!alvo} titulo="Desbloquear cliente?" confirmar="Desbloquear" cancelar="Voltar"
                       carregando={processando} aoConfirmar={desbloquear} aoFechar={() => setAlvo(null)}>
            {alvo && <p>Liberar <strong>{alvo.nome}</strong> para novos agendamentos agora? O contador de
                cancelamentos dele será zerado e ele receberá um e-mail avisando.</p>}
        </ConfirmDialog>
    </section>;
}
