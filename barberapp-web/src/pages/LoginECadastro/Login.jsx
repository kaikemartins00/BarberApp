import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom'; 
import api from '../../api/api.js';

function Login() {
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [erro, setErro] = useState('');
  const [carregando, setCarregando] = useState(false);
  const navigate = useNavigate();

  async function handleLogin(event) { // Função assíncrona para lidar com o envio do formulário de login
    event.preventDefault(); // Impede o comportamento padrão do navegador de recarregar a página ao enviar o formulário
    setErro(''); // Limpa qualquer mensagem de erro anterior
    setCarregando(true); // Define o estado de carregamento como verdadeiro para indicar que a requisição está em andamento

    try {
      const response = await api.post('/auth/login', { email, senha }); 
      const token = response.data.token;

      console.log('Token recebido:', token);

    } catch (error) {
      setErro('Email ou senha inválidos');
    } finally {
      setCarregando(false);
    }
  }

  return (
    <div style={{ maxWidth: '320px', margin: '80px auto' }}>
      <h1>BarberApp</h1>

      <form onSubmit={handleLogin}>
        <div>
          <input
            type="email"
            placeholder="Email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
        </div>

        <div style={{ marginTop: '12px' }}>
          <input
            type="password"
            placeholder="Senha"
            value={senha}
            onChange={(e) => setSenha(e.target.value)}
            required
          />
        </div>

        {erro && <p style={{ color: 'red' }}>{erro}</p>}

        <button type="submit" disabled={carregando} style={{ marginTop: '16px' }}>
          {carregando ? 'Entrando...' : 'Entrar'}
        </button>
      </form>
      <p style={{ marginTop: '16px' }}>
            Não tem conta? <Link to="/cadastro">Cadastrar</Link>
      </p>
    </div>
  );
}

export default Login;