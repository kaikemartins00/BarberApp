import {useEffect} from 'react';

const ESTILOS = `
.dlg-fundo { position:fixed; inset:0; z-index:1000; display:flex; align-items:center; justify-content:center; padding:20px; background:rgba(0,0,0,.6) }
.dlg { width:100%; max-width:440px; display:flex; flex-direction:column; gap:18px; padding:26px; border-radius:16px; color:var(--text, #fff); background:var(--surface-2, #17171c); border:1px solid var(--line, #2a2a31); box-shadow:0 20px 60px rgba(0,0,0,.5) }
.dlg h3 { margin:0; font-size:1.2rem }
.dlg-corpo { display:flex; flex-direction:column; gap:12px; font-size:.95rem; line-height:1.5; color:var(--muted, #a1a1aa) }
.dlg-corpo p { margin:0 }
.dlg-corpo strong { color:var(--text, #fff) }
.dlg-aviso { padding:10px 14px; border-radius:10px; color:var(--text, #fff); border:1px solid rgba(212,162,76,.45); background:rgba(212,162,76,.12) }
.dlg-acoes { display:flex; justify-content:flex-end; flex-wrap:wrap; gap:12px }
.dlg-perigo { color:#fff; background:#c0392b; border:1px solid #c0392b }
.dlg-perigo:hover:not(:disabled) { background:#a93226 }
`;

export default function ConfirmDialog({aberto, titulo, children, confirmar = 'Confirmar', cancelar = 'Voltar', perigo = false,
                                          carregando = false, desabilitarConfirmar = false, aoConfirmar, aoFechar}) {
    useEffect(() => {
        if (!aberto) return;
        const tecla = e => {
            if (e.key === 'Escape') aoFechar();
        };
        window.addEventListener('keydown', tecla);
        return () => window.removeEventListener('keydown', tecla);
    }, [aberto, aoFechar]);

    if (!aberto) return null;

    return <div className="dlg-fundo" role="presentation" onClick={carregando ? undefined : aoFechar}>
        <style>{ESTILOS}</style>
        <div className="dlg" role="dialog" aria-modal="true" aria-label={titulo} onClick={e => e.stopPropagation()}>
            <h3>{titulo}</h3>
            <div className="dlg-corpo">{children}</div>
            <div className="dlg-acoes">
                <button type="button" className="btn btn--ghost" disabled={carregando}
                        onClick={aoFechar}>{cancelar}</button>
                <button type="button" className={`btn ${perigo ? 'dlg-perigo' : 'btn--primary'}`}
                        disabled={carregando || desabilitarConfirmar}
                        onClick={aoConfirmar}>{carregando ? 'Aguarde...' : confirmar}</button>
            </div>
        </div>
    </div>;
}
