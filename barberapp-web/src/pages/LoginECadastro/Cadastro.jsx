import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Title } from './Style.js';
import api from '../../api/api.js';

function Cadastro() {
  const navigate = useNavigate();
  const [nome, setNome] = useState('');
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [telefone, setTelefone] = useState('');
  const [erro, setErro] = useState('');
  const [carregando, setCarregando] = useState(false);

  async function handleCadastro(event) { // Função assíncrona para lidar com o envio do formulário de cadastro
    event.preventDefault(); // Impede o comportamento padrão do navegador de recarregar a página ao enviar o formulário
    setErro('');
    setCarregando(true);

    try {
      const response = await api.post('/auth/cadastro', { nome, email, senha, telefone });
      const token = response.data.token;

      navigate('/verificar-conta', {
          state: { email } 
      });

    } catch (error) {
      setErro('Email ou senha inválidos');
    } finally {
      setCarregando(false);
    }
  }

  return (
    <div style={{ maxWidth: '320px', margin: '80px auto' }}>
      <Title>Cadastro</Title>

      <form onSubmit={handleCadastro}>
        
        <div style={{ marginTop: '12px' }}>
          <input
            type="text"
            placeholder="Nome"
            value={nome}
            onChange={(e) => setNome(e.target.value)}
            required
          />
        </div>
        
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

        <div style={{ marginTop: '12px' }}>
          <input
            type="text"
            placeholder="Telefone"
            value={telefone}
            onChange={(e) => setTelefone(e.target.value)}
            required
          />
        </div>

        {erro && <p style={{ color: 'red' }}>{erro}</p>}

        <button type="submit" disabled={carregando} style={{ marginTop: '16px' }}>
          {carregando ? 'Cadastrando...' : 'Cadastrar'}
        </button>
      </form>
       <p style={{ marginTop: '16px' }}>
            Já tem conta? <Link to="/login">Entrar</Link>
       </p>
    </div>
  );
}

export default Cadastro;