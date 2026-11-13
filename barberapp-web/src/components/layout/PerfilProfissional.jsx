import {useRef, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {X, KeyRound, ImagePlus} from 'lucide-react';
import api from '../../api/api.js';
import {lerSessao} from '../../auth/session.js';
import RecortarFoto from './RecortarFoto.jsx';
import './PerfilProfissional.css';

const abreComoImagem = (url, cors) => new Promise(resolve => {
    const img = new Image();
    if (cors) img.crossOrigin = 'anonymous';
    img.onload = () => resolve(true);
    img.onerror = () => resolve(false);
    img.src = url;
});

export default function PerfilProfissional({perfil, nome, admin, onFechar, onFoto}) {
    const sessao = lerSessao();
    const navigate = useNavigate();
    const [foto, setFoto] = useState(perfil?.fotoUrl?.startsWith('data:') ? '' : perfil?.fotoUrl || '');
    const [salvando, setSalvando] = useState(false);
    const [aviso, setAviso] = useState(null);
    const [arquivo, setArquivo] = useState(null);
    const seletor = useRef(null);

    function escolherArquivo(event) {
        const escolhido = event.target.files?.[0];
        event.target.value = '';
        if (!escolhido) return;
        if (!escolhido.type.startsWith('image/')) {
            setAviso({erro: true, texto: 'Escolha um arquivo de imagem (JPG, PNG, etc.).'});
            return;
        }
        setAviso(null);
        setArquivo(escolhido);
    }

    async function salvarFoto(event) {
        event.preventDefault();
        const url = foto.trim();
        if (!/^https?:\/\//i.test(url)) {
            setAviso({erro: true, texto: 'Use o endereço completo da imagem, começando com http:// ou https://'});
            return;
        }
        setSalvando(true);
        setAviso(null);
        if (!await abreComoImagem(url, false)) {
            setSalvando(false);
            setAviso({
                erro: true,
                texto: 'Não consegui carregar uma imagem desse endereço. Use o endereço da própria imagem, que costuma terminar em .jpg, .png ou .webp. Dica: clique com o botão direito na imagem e escolha "Copiar endereço da imagem".'
            });
            return;
        }
        // se o site permitir, abre o corte; senão salva o link como está
        if (await abreComoImagem(url, true)) {
            setSalvando(false);
            setArquivo(url);
        } else {
            await gravar(url);
        }
    }

    async function gravar(url) {
        setSalvando(true);
        setAviso(null);
        try {
            await api.put(`/barbeiros/${sessao.userId}`, {email: sessao.email, fotoUrl: url || null});
            onFoto(url || null);
            setFoto(url.startsWith('data:') ? '' : url);
            setAviso({erro: false, texto: 'Foto atualizada.'});
        } catch (error) {
            setAviso({erro: true, texto: error.response?.data?.message || 'Não foi possível salvar a foto.'});
        } finally {
            setSalvando(false);
        }
    }

    return (
        <div className="pp-fundo" onClick={onFechar}>
            <div className="pp-caixa" role="dialog" aria-label="Meu perfil" onClick={e => e.stopPropagation()}>
                <button type="button" className="pp-fechar" onClick={onFechar} aria-label="Fechar"><X size={18}/></button>

                <div className="pp-topo">
                    <span className="pp-avatar">{nome[0].toUpperCase()}{perfil?.fotoUrl &&
                        <img src={perfil.fotoUrl} alt="" onError={e => e.currentTarget.remove()}/>}</span>
                    <div>
                        <strong>{nome}</strong>
                        <small>{sessao.email}</small>
                    </div>
                </div>

                {!admin && (
                    <form className="pp-secao" onSubmit={salvarFoto}>
                        <label htmlFor="pp-foto">Foto de perfil</label>
                        <input ref={seletor} type="file" accept="image/*" hidden onChange={escolherArquivo}/>
                        <button type="button" className="adm-btn adm-btn--gold" disabled={salvando}
                                onClick={() => seletor.current?.click()}>
                            <ImagePlus size={15}/> Escolher da minha máquina
                        </button>
                        <input id="pp-foto" type="url" placeholder="ou cole o endereço de uma imagem (https://...)"
                               value={foto} onChange={e => setFoto(e.target.value)}/>
                        <button type="submit" className="adm-btn adm-btn--outline" disabled={salvando || !foto.trim()}>
                            {salvando ? 'Salvando...' : 'Salvar pelo endereço'}
                        </button>
                    </form>
                )}

                <div className="pp-secao">
                    <label>Senha</label>
                    <p>Enviamos um código para o seu e-mail para você criar uma senha nova com segurança.</p>
                    <button type="button" className="adm-btn adm-btn--outline"
                            onClick={() => navigate('/esqueci-senha', {state: {email: sessao.email}})}>
                        <KeyRound size={15}/> Redefinir senha
                    </button>
                </div>

                <p className="pp-nota">O e-mail é o seu login e só pode ser alterado pelo administrador.</p>

                {arquivo && <RecortarFoto arquivo={arquivo} onCancelar={() => setArquivo(null)}
                                          onConfirmar={url => {
                                              setArquivo(null);
                                              gravar(url);
                                          }}/>}

                {aviso && <div className={`adm-aviso ${aviso.erro ? 'adm-aviso--erro' : 'adm-aviso--ok'}`}>{aviso.texto}</div>}
            </div>
        </div>
    );
}
