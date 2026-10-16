import { Navigate } from 'react-router-dom';
import { lerSessao } from '../auth/session.js';

function RotaProtegida({ roles, children }) {
    const sessao = lerSessao();

    if (!sessao) return <Navigate to="/login" replace />;

    const temPermissao = roles.some((role) => sessao.roles.includes(role));
    if (!temPermissao) return <Navigate to="/" replace />;

    return children;
}

export default RotaProtegida;