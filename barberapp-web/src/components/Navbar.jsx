import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Logo } from './Visuais.jsx';
import { lerSessao, sair } from '../auth/session.js';
import BotaoTema from './layout/BotaoTema.jsx';
import MensagemFlutuante from './notificacoes/MensagemFlutuante.jsx';

function Navbar({ children }) {
    const navigate = useNavigate();
    const sessao = lerSessao();
    const location = useLocation();
    const cliente = sessao?.roles?.includes('ROLE_CLIENTE');

    function handleSair() {
        sair();
        navigate('/login');
    }

    return (
        <>
            <header className="nav">
                <Link to="/" className="nav__logo">
                    <Logo />
                </Link>

                <nav className="nav__links">
                    {children}
                </nav>

                <div
                    className="nav__user"
                    style={{ display: 'flex', alignItems: 'center', gap: 14 }}
                >
                    {sessao && (
                        <span className="muted small">{sessao.email}</span>
                    )}

                    <BotaoTema />

                    {sessao ? (
                        <button
                            className="btn btn--ghost"
                            onClick={handleSair}
                        >
                            Sair
                        </button>
                    ) : (
                        <Link to="/login" className="btn btn--primary">
                            Entrar
                        </Link>
                    )}
                </div>
            </header>
            {cliente && location.pathname !== '/mensagens' && <MensagemFlutuante/>}
        </>
    );
}

export default Navbar;