import { useEffect, useState } from 'react';
import api from '../api/api.js';

export default function NotaBarbeiro({ barbeiroId }) {
    const [resumo, setResumo] = useState(null);

    useEffect(() => {
        if (!barbeiroId) return;

        let ativo = true;

        api.get(`/avaliacoes/barbeiros/${barbeiroId}/resumo`)
            .then((r) => {
                if (ativo) setResumo(r.data);
            })
            .catch(() => {});

        return () => {
            ativo = false;
        };
    }, [barbeiroId]);

    if (!resumo) return null;

    if (!resumo.quantidade) {
        return <span className="muted small">Sem avaliações</span>;
    }

    return (
        <span className="muted small">
      ★ {Number(resumo.media).toFixed(1)} · {resumo.quantidade} avaliações
    </span>
    );
}
