import {useEffect, useRef, useState} from 'react';
import {X} from 'lucide-react';
import './RecortarFoto.css';

const AREA = 280;   // tamanho da janela de corte na tela
const SAIDA = 256;  // tamanho final da foto (quadrada)

// Corte de foto no estilo WhatsApp: arrastar para posicionar, controle para dar zoom.
export default function RecortarFoto({arquivo, onCancelar, onConfirmar}) {
    const imgRef = useRef(null);
    const arraste = useRef(null);
    const porEndereco = typeof arquivo === 'string';
    const [url, setUrl] = useState(porEndereco ? arquivo : '');
    const [tam, setTam] = useState(null);       // {w, h} da imagem original
    const [zoom, setZoom] = useState(1);
    const [centro, setCentro] = useState({x: 0, y: 0}); // ponto da imagem que fica no meio da janela

    useEffect(() => {
        if (porEndereco) return;
        let ativo = true;
        const leitor = new FileReader();
        leitor.onload = () => {
            if (ativo) setUrl(String(leitor.result));
        };
        leitor.readAsDataURL(arquivo);
        return () => {
            ativo = false;
        };
    }, [arquivo, porEndereco]);

    const base = tam ? AREA / Math.min(tam.w, tam.h) : 1;
    const escala = base * zoom;

    const limitar = (c, z = zoom) => {
        const e = base * z;
        const meio = AREA / 2 / e;
        return {
            x: Math.min(Math.max(c.x, meio), tam.w - meio),
            y: Math.min(Math.max(c.y, meio), tam.h - meio),
        };
    };

    function aoCarregar(e) {
        const w = e.currentTarget.naturalWidth;
        const h = e.currentTarget.naturalHeight;
        setTam({w, h});
        setCentro({x: w / 2, y: h / 2});
    }

    function mudarZoom(valor) {
        setZoom(valor);
        setCentro(c => limitar(c, valor));
    }

    function comecar(e) {
        e.currentTarget.setPointerCapture(e.pointerId);
        arraste.current = {x: e.clientX, y: e.clientY};
    }

    function mover(e) {
        if (!arraste.current || !tam) return;
        const dx = e.clientX - arraste.current.x;
        const dy = e.clientY - arraste.current.y;
        arraste.current = {x: e.clientX, y: e.clientY};
        setCentro(c => limitar({x: c.x - dx / escala, y: c.y - dy / escala}));
    }

    function soltar() {
        arraste.current = null;
    }

    function confirmar() {
        const lado = AREA / escala;
        const canvas = document.createElement('canvas');
        canvas.width = SAIDA;
        canvas.height = SAIDA;
        const ctx = canvas.getContext('2d');
        ctx.fillStyle = '#fff';
        ctx.fillRect(0, 0, SAIDA, SAIDA);
        ctx.drawImage(imgRef.current, centro.x - lado / 2, centro.y - lado / 2, lado, lado, 0, 0, SAIDA, SAIDA);
        onConfirmar(canvas.toDataURL('image/jpeg', 0.85));
    }

    return (
        <div className="rf-fundo">
            <div className="rf-caixa" role="dialog" aria-label="Ajustar foto">
                <div className="rf-topo">
                    <strong>Ajustar foto</strong>
                    <button type="button" onClick={onCancelar} aria-label="Cancelar"><X size={18}/></button>
                </div>

                <div className="rf-janela" style={{width: AREA, height: AREA}}
                     onPointerDown={comecar} onPointerMove={mover} onPointerUp={soltar} onPointerCancel={soltar}>
                    {url && <img ref={imgRef} src={url} alt="" draggable={false} onLoad={aoCarregar}
                                 crossOrigin={porEndereco ? 'anonymous' : undefined}
                                 style={tam ? {
                                     width: tam.w * escala,
                                     height: tam.h * escala,
                                     left: AREA / 2 - centro.x * escala,
                                     top: AREA / 2 - centro.y * escala,
                                 } : {visibility: 'hidden'}}/>}
                    <span className="rf-mascara"/>
                </div>

                <input className="rf-zoom" type="range" min="1" max="3" step="0.01" value={zoom}
                       onChange={e => mudarZoom(Number(e.target.value))} aria-label="Zoom"/>
                <p>Arraste para posicionar e use o controle para aproximar.</p>

                <div className="rf-acoes">
                    <button type="button" className="adm-btn adm-btn--outline" onClick={onCancelar}>Cancelar</button>
                    <button type="button" className="adm-btn adm-btn--gold" onClick={confirmar} disabled={!tam}>Usar foto</button>
                </div>
            </div>
        </div>
    );
}
