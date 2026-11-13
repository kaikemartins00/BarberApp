import {useEffect, useState} from 'react';
import api from '../../api/api.js';
import {dataHoraBR} from '../../pages/Admin/util.js';
import './AdminExtras.css';

/** Histórico de barbeiros desligados e o motivo registrado. */
export default function EquipeDesligada({versao}) {
    const [lista, setLista] = useState(null);
    const [falhou, setFalhou] = useState(false);

    useEffect(() => {
        let ativo = true;
        api.get('/barbeiros/desligados')
            .then(r => ativo && (setLista(r.data || []), setFalhou(false)))
            .catch(() => ativo && setFalhou(true));
        return () => {
            ativo = false;
        };
    }, [versao]);

    if (falhou || !lista || lista.length === 0) return null;

    return <section className="adm-x-card">
        <h3>Ex-barbeiros</h3>
        <p className="adm-x-sub">Registro de quem já saiu da equipe e o motivo informado.</p>
        <div className="adm-x-lista">
            {lista.map(b => <div className="adm-x-item" key={b.id}>
                <div className="adm-x-item-txt">
                    <strong>{b.nome}</strong>
                    <small>{b.email} · desligado em {dataHoraBR(b.desligadoEm)}</small>
                    <p className="adm-x-motivo">Motivo: {b.motivo}</p>
                </div>
            </div>)}
        </div>
    </section>;
}
