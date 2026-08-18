import { useState, useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { useNavigate, Link } from 'react-router-dom'; 
import api from '../api/api';

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

      console.log('Token recebido:', token);
      alert('Conta verificada com sucesso!');

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
    <div style={{ maxWidth: '320px', margin: '80px auto' }}>
      <h1>Verificar Conta</h1>

      <form onSubmit={handleVerificarConta}>
        <div style={{ marginTop: '12px' }}>
          <input
            type="text"
            placeholder="Código de Verificação"
            value={codigo}
            onChange={(e) => setCodigo(e.target.value)}
            required
          />
        </div>

        {erro && <p style={{ color: 'red' }}>{erro}</p>}
        {sucesso && <p style={{ color: 'green' }}>{sucesso}</p>}

        <button type="submit" disabled={carregando} style={{ marginTop: '16px' }}>
          {carregando ? 'Verificando...' : 'Verificar'}
        </button>

        <button
          type="button"
          onClick={handleReenviar}
          disabled={reenviando || cooldown > 0}
          style={{ marginTop: '12px' }}
        >
          {cooldown > 0
            ? `Reenviar em ${cooldown}s`
            : reenviando
              ? 'Enviando...'
              : 'Reenviar código'}
        </button>
      </form>
    </div>
  );
}

export default VerificarConta;