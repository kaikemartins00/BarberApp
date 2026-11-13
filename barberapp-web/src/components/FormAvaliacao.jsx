import { useEffect, useState } from 'react';
import api from '../api/api.js';

export default function FormAvaliacao({ agendamentoId, onAvaliada }) {
    const [nota, setNota] = useState(5);
    const [comentario, setComentario] = useState('');
    const [caracteristicas, setCaracteristicas] = useState([]);
    const [jaAvaliado, setJaAvaliado] = useState(null);
    const [erro, setErro] = useState('');
    const [enviando, setEnviando] = useState(false);

    const opcoes = [
        'Pontual',
        'Caprichoso',
        'Bom atendimento',
        'Ambiente agradável'
    ];

    useEffect(() => {
        let ativo = true;

        api.get(`/avaliacoes/agendamentos/${agendamentoId}`)
            .then((r) => {
                if (ativo) setJaAvaliado(Boolean(r.data?.avaliado));
            })
            .catch(() => {
                if (ativo) {
                    setJaAvaliado(null);
                    setErro('Não foi possível verificar a avaliação.');
                }
            });

        return () => {
            ativo = false;
        };
    }, [agendamentoId]);

    async function enviar(event) {
        event.preventDefault();
        if (jaAvaliado !== false || enviando) return;

        setErro('');
        setEnviando(true);

        try {
            await api.post('/avaliacoes', {
                agendamentoId,
                nota,
                comentario: comentario.trim(),
                caracteristicas
            });

            setJaAvaliado(true);
            onAvaliada?.();
        } catch (error) {
            setErro(
                error.response?.data?.message ||
                'Não foi possível enviar a avaliação.'
            );
        } finally {
            setEnviando(false);
        }
    }

    if (jaAvaliado === true) {
        return <p className="muted small">★ Atendimento já avaliado.</p>;
    }

    return (
        <form className="avaliacao-form" onSubmit={enviar}>
            <strong>Avalie este barbeiro</strong>

            <div className="avaliacao-estrelas">
                {[1, 2, 3, 4, 5].map((estrela) => (
                    <button
                        key={estrela}
                        type="button"
                        aria-label={`${estrela} estrelas`}
                        aria-pressed={nota === estrela}
                        onClick={() => setNota(estrela)}
                    >
                        {estrela <= nota ? '★' : '☆'}
                    </button>
                ))}
            </div>

            <div className="avaliacao-tags">
                {opcoes.map((opcao) => (
                    <label key={opcao}>
                        <input
                            type="checkbox"
                            checked={caracteristicas.includes(opcao)}
                            onChange={(event) =>
                                setCaracteristicas((atual) =>
                                    event.target.checked
                                        ? [...atual, opcao]
                                        : atual.filter((item) => item !== opcao)
                                )
                            }
                        />
                        {opcao}
                    </label>
                ))}
            </div>

            <label className="field">
                <span>Comentário (opcional)</span>
                <textarea
                    rows={2}
                    maxLength={500}
                    value={comentario}
                    onChange={(event) => setComentario(event.target.value)}
                    placeholder="Como foi o atendimento?"
                />
            </label>

            {erro && <p className="alert alert--erro">{erro}</p>}

            <button
                type="submit"
                className="btn btn--primary"
                disabled={enviando || jaAvaliado !== false}
            >
                {enviando ? 'Enviando...' : 'Enviar avaliação'}
            </button>
        </form>
    );
}
