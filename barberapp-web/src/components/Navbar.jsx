import { Link, useNavigate } from 'react-router-dom';
import { Logo } from './Visuais.jsx';
import { lerSessao, sair } from '../auth/session.js';

function Navbar({ children }) {
    const navigate = useNavigate();
    const sessao = lerSessao();

    function handleSair() {
        sair();
        navigate('/login');
    }

    return (
        <header className="nav">
            <Link to="/" className="nav__logo"><Logo /></Link>

            <nav className="nav__links">{children}</nav>

            <div className="nav__user">
                {sessao ? (
                    <>
                        <span className="muted small">{sessao.email}</span>
                        <button className="btn btn--ghost" onClick={handleSair}>Sair</button>
                    </>
                ) : (
                    <Link to="/login" className="btn btn--primary">Entrar</Link>
                )}
            </div>
        </header>
    );
}

export default Navbar;