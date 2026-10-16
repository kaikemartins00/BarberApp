import { Logo, BarberPole } from './Visuais.jsx';

function AuthLayout({ titulo, subtitulo, children }) {
    return (
        <div className="auth">
            <aside className="auth__visual">
                <Logo />
                <BarberPole />
                <p className="auth__frase">
                    Estilo, tempo e <em>precisão</em> em cada corte.
                </p>
            </aside>

            <main className="auth__painel">
                <div className="auth__form">
                    <div className="auth__logo-mobile"><Logo /></div>
                    <h1>{titulo}</h1>
                    {subtitulo && <p className="muted">{subtitulo}</p>}
                    {children}
                </div>
            </main>
        </div>
    );
}

export default AuthLayout;