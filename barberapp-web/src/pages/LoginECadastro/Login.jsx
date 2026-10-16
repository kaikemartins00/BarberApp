import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import api from '../../api/api.js';
import { salvarSessao, telaInicial } from '../../auth/session.js';
import AuthLayout from "../../components/AuthLayout.jsx";

// O backend precisa responder de forma DIFERENTE quando a conta não foi verificada
// (ex.: status 403 com a mensagem "conta não verificada").
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
      setErro('Email ou senha inválidos');
    } finally {
      setCarregando(false);
    }
  }

  return (
      <AuthLayout titulo="Entrar" subtitulo="Acesse sua conta para continuar.">
        <form className="form" onSubmit={handleLogin}>
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