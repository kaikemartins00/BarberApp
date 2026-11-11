import { useState } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import api from '../../api/api.js';
import { salvarSessao, telaInicial } from '../../auth/session.js';
import AuthLayout from "../../components/AuthLayout.jsx";

function contaNaoVerificada(error) {
  const status = error.response?.status;
  const mensagem = error.response?.data?.message || '';
  return status === 403 || /verific/i.test(mensagem);
}

function Login() {
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [erro, setErro] = useState('');
  const [carregando, setCarregando] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const senhaAlterada = location.state?.senhaAlterada;

  async function handleLogin(event) {
    event.preventDefault();
    setErro('');
    setCarregando(true);

    try {
      const response = await api.post('/auth/login', { email, senha });
      const sessao = salvarSessao(response.data, email);

      navigate(telaInicial(sessao), { replace: true });

    } catch (error) {
      if (contaNaoVerificada(error)) {
        navigate('/verificar-conta', { state: { email } });
        return;
      }

      const mensagem = error.response?.data?.message;

      setErro(mensagem || 'Não foi possível realizar o login. Tente novamente.');
    } finally {
      setCarregando(false);
    }
  }

  return (
      <AuthLayout titulo="Entrar" subtitulo="Acesse sua conta para continuar.">
        <form className="form" onSubmit={handleLogin}>
          {senhaAlterada && (
              <p className="alert alert--ok">Senha alterada! Entre com a nova senha.</p>
          )}

          <label className="field">
            <span>Email</span>
            <input type="email" placeholder="voce@email.com" value={email}
                   onChange={(e) => setEmail(e.target.value)} required />
          </label>

          <label className="field">
            <span>Senha</span>
            <input type="password" placeholder="••••••••" value={senha}
                   onChange={(e) => setSenha(e.target.value)} required />
          </label>

          {erro && <p className="alert alert--erro">{erro}</p>}

          <button type="submit" className="btn btn--primary btn--block" disabled={carregando}>
            {carregando ? 'Entrando...' : 'Entrar'}
          </button>
        </form>

        <div className="auth__rodape">
          <span>Não tem conta? <Link to="/cadastro">Cadastrar</Link></span>
          <span>Esqueceu a senha? <Link to="/esqueci-senha" state={{ email }}>Redefinir</Link></span>
          {email && (
              <span>
            Primeiro acesso? <Link to="/verificar-conta" state={{ email }}>Verificar conta</Link>
          </span>
          )}
        </div>
      </AuthLayout>
  );
}

export default Login;