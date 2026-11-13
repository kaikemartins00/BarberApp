import {useState} from 'react';
import api from '../../api/api.js';
import ConfirmDialog from '../ConfirmDialog.jsx';
import {mensagemErro} from '../../pages/Admin/util.js';
import './AdminExtras.css';

/** Pede o motivo e desliga o barbeiro (o histórico dele continua no banco). */
export default function DesligarBarbeiro({barbeiro, futuros, aoFechar, aoConcluir}) {
    const [motivo, setMotivo] = useState('');
    const [erro, setErro] = useState('');
    const [enviando, setEnviando] = useState(false);

    if (!barbeiro) return null;

    async function confirmar() {
        setEnviando(true);
        setErro('');
        try {
            const {data} = await api.post(`/barbeiros/${barbeiro.id}/desligar`, {motivo: motivo.trim()});
            setMotivo('');
            await aoConcluir(barbeiro, data);
        } catch (e) {
            setErro(mensagemErro(e, 'Não foi possível desligar o barbeiro.'));
        } finally {
            setEnviando(false);
        }
    }

    return <ConfirmDialog aberto titulo={`Desligar ${barbeiro.nome}?`} perigo confirmar="Desligar barbeiro"
                          carregando={enviando} desabilitarConfirmar={motivo.trim().length < 5}
                          aoConfirmar={confirmar} aoFechar={aoFechar}>
        <p>O acesso dele ao sistema é bloqueado e ele deixa de aparecer para os clientes. O histórico de
            atendimentos continua guardado.</p>
        {futuros > 0 && <p className="dlg-aviso"><strong>{futuros} agendamento(s) futuro(s)</strong> serão cancelados,
            com reembolso do Pix e e-mail avisando cada cliente.</p>}
        <label className="adm-x-field">Motivo do desligamento (fica salvo no banco)
            <textarea className="adm-x-textarea" maxLength={500} value={motivo} autoFocus
                      placeholder="Ex.: Pedido de demissão, mudança de cidade, desligamento por desempenho..."
                      onChange={e => setMotivo(e.target.value)}/>
        </label>
        {erro && <div className="adm-x-aviso adm-x-aviso--erro">{erro}</div>}
    </ConfirmDialog>;
}
