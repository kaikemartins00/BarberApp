import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { gravarConsentimento, lerConsentimento } from '../utils/consentimento.js';
import './BannerCookies.css';

export default function BannerCookies() {
    const [visivel, setVisivel] = useState(() => lerConsentimento() === null);
    const [personalizar, setPersonalizar] = useState(false);
    const [preferencias, setPreferencias] = useState(true);

    // permite reabrir pelo botão "Alterar minhas preferências" da Política de Cookies
    useEffect(() => {
        const reabrir = () => {
            setPersonalizar(true);
            setPreferencias(lerConsentimento()?.preferencias ?? true);
            setVisivel(true);
        };
        window.addEventListener('barberapp:abrir-cookies', reabrir);
        return () => window.removeEventListener('barberapp:abrir-cookies', reabrir);
    }, []);

    function salvar(valorPreferencias) {
        gravarConsentimento(valorPreferencias);
        setVisivel(false);
        setPersonalizar(false);
    }

    if (!visivel) return null;

    return (
        <section className="cookies" role="dialog" aria-live="polite" aria-label="Aviso de cookies e armazenamento">
            <div className="cookies__linha">
                <p className="cookies__texto">
                    <strong>Sua privacidade.</strong> Guardamos dados no navegador só para manter você conectado e lembrar o
                    tema. Sem publicidade nem rastreamento. <Link to="/cookies">Política de Cookies</Link> ·{' '}
                    <Link to="/privacidade">Privacidade</Link>
                </p>

                <div className="cookies__botoes">
                    {personalizar ? (
                        <button type="button" className="btn btn--primary" onClick={() => salvar(preferencias)}>
                            Salvar escolhas
                        </button>
                    ) : (
                        <>
                            <button type="button" className="cookies__link" onClick={() => setPersonalizar(true)}>Personalizar</button>
                            <button type="button" className="btn" onClick={() => salvar(false)}>Só essenciais</button>
                            <button type="button" className="btn btn--primary" onClick={() => salvar(true)}>Aceitar todos</button>
                        </>
                    )}
                </div>
            </div>

            {personalizar && (
                <div className="cookies__opcoes">
                    <label className="cookies__opcao">
                        <input type="checkbox" checked disabled />
                        <span><strong>Essenciais</strong> (sempre ativos): login e segurança.</span>
                    </label>
                    <label className="cookies__opcao">
                        <input type="checkbox" checked={preferencias} onChange={(e) => setPreferencias(e.target.checked)} />
                        <span><strong>Preferências</strong>: lembrar o tema.</span>
                    </label>
                </div>
            )}
        </section>
    );
}