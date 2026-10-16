import { useState, useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { useNavigate, Link } from 'react-router-dom'; 
import api from '../../api/api.js';
import AuthLayout from "../../components/AuthLayout.jsx";

function VerificarConta() {
  const [codigo, setCodigo] = useState('');
  const location = useLocation();
  const [erro, setErro] = useState('');
  const [carregando, setCarregando] = useState(false);
  const email = location.state?.email || '';
  const [reenviando, setReenviando] = useState(false);
  const [cooldown, setCooldown] = useState(0);
  const [sucesso, setSucesso] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    if (cooldown === 0) return;

    const timer = setTimeout(() => {
      setCooldown(cooldown - 1);
    }, 1000);

    return () => clearTimeout(timer);
  }, [cooldown]);

  async function handleVerificarConta(event) {
    event.preventDefault();
    setErro('');
    setCarregando(true);

    try {
      const response = await api.post('/auth/validar-codigo', { email, codigo });
      const token = response.data.token;

      alert('Conta verificada com sucesso! Faça login para entrar.');
      navigate('/login', { replace: true });

    } catch (error) {
      const status = error.response?.status;
      const mensagem = error.response?.data?.message;

      if (status === 400 && mensagem) {
        setErro(mensagem);
      } else if (status === 500) {
        setErro('Erro no servidor. Tente novamente em instantes.');
      } else if (status === undefined) {
        setErro('Não foi possível conectar ao servidor. Verifique sua conexão.');
      } else {
        setErro('Erro ao validar código. Por favor, tente novamente.');
      }
    } finally {
      setCarregando(false);
    }
  }

  async function handleReenviar() {
    setErro('');
    setSucesso('');
    setReenviando(true);

    try {
      await api.post(`/auth/reenviar-codigo?email=${email}`);
      setSucesso('Novo código enviado!');
      setCooldown(30);

    } catch (error) {
      const status = error.response?.status;
      const mensagem = error.response?.data?.message;

      if (status === 400 && mensagem) {
        setErro(mensagem);
      } else if (status === undefined) {
        setErro('Não foi possível conectar ao servidor.');
      } else {
        setErro('Erro ao reenviar código.');
      }
    } finally {
      setReenviando(false);
    }
  }

  return (
      <AuthLayout
          titulo="Verificar conta"
          subtitulo={email ? `Enviamos um código para ${email}.` : 'Digite o código que recebeu por e-mail.'}
      >
        <form className="form" onSubmit={handleVerificarConta}>
          <label className="field">
            <span>Código de verificação</span>
            <input type="text" placeholder="000000" value={codigo}
                   onChange={(e) => setCodigo(e.target.value)} required />
          </label>

          {erro && <p className="alert alert--erro">{erro}</p>}
          {sucesso && <p className="alert alert--ok">{sucesso}</p>}

          <button type="submit" className="btn btn--primary btn--block" disabled={carregando}>
            {carregando ? 'Verificando...' : 'Verificar'}
          </button>

          <button type="button" className="btn btn--ghost btn--block"
                  onClick={handleReenviar} disabled={reenviando || cooldown > 0}>
            {cooldown > 0 ? `Reenviar em ${cooldown}s` : reenviando ? 'Enviando...' : 'Reenviar código'}
          </button>
        </form>

        <div className="auth__rodape">
          <Link to="/login">Voltar ao login</Link>
        </div>
      </AuthLayout>
  );
}

export default VerificarConta;