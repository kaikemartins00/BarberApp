import { useState } from 'react';
import api from '../api/api';

function Login() {
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [erro, setErro] = useState('');
  const [carregando, setCarregando] = useState(false);

  async function handleLogin(event) {
    event.preventDefault(); 
    setErro('');
    setCarregando(true);

    try {
      const response = await api.post('/auth/login', { email, senha });
      const token = response.data.token;

      console.log('Token recebido:', token);
      alert('Login realizado com sucesso!');

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
    </div>
  );
}

export default Login;