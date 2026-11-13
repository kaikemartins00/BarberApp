import {useEffect, useState} from 'react';
import api from '../api/api.js';

export default function OcupacaoAgenda({barbeiroId}) {
    const [data, setData] = useState(() => {
        const d = new Date();
        return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
    });

    const [resumo, setResumo] = useState(null);
    const [erro, setErro] = useState('');

    useEffect(() => {

            if (!barbeiroId) return;
            let ativo = true;
            setResumo(null);

            api.get(`/ocupacao/barbeiro/${barbeiroId}`, {params: {data}})
                .then(r => {
                    if (ativo) {
                        setResumo(r.data);
                        setErro('');
                    }
                })
                .catch(e => {
                    if (ativo) setErro(e.response?.data?.message || 'Ocupação indisponível.');
                });
            return () => {
                ativo = false;
            };
        },

        [barbeiroId, data]);

    return <section className="panel"><h2>Ocupação da agenda</h2><label className="field"><span>Data</span><input
        type="date" value={data} onChange={e => setData(e.target.value)}/></label>
        {erro && <p className="alert alert--erro">{erro}</p>}
        {resumo && <><p><strong>{Number(resumo.percentualOcupacao || 0).toFixed(1)}%</strong> dos horários reservados
        </p>
            <progress value={resumo.percentualOcupacao || 0} max="100" style={{width: '100%'}}/>
            <p className="muted small">{resumo.horariosOcupados ?? 0} ocupados
                de {resumo.horariosDisponibilizados ?? 0} horários disponíveis</p></>}
    </section>;
}
