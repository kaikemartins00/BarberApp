import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import api from '../../api/api.js';
import AuthLayout from "../../components/AuthLayout.jsx";

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
      <AuthLayout titulo="Criar conta" subtitulo="Leva menos de um minuto.">
        <form className="form" onSubmit={handleCadastro}>
          <label className="field">
            <span>Nome</span>
            <input type="text" placeholder="Seu nome" value={nome}
                   onChange={(e) => setNome(e.target.value)} required />
          </label>

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

          <label className="field">
            <span>Telefone</span>
            <input type="text" placeholder="(79) 90000-0000" value={telefone}
                   onChange={(e) => setTelefone(e.target.value)} required />
          </label>

          {erro && <p className="alert alert--erro">{erro}</p>}

          <button type="submit" className="btn btn--primary btn--block" disabled={carregando}>
            {carregando ? 'Cadastrando...' : 'Cadastrar'}
          </button>
        </form>

        <div className="auth__rodape">
          <span>Já tem conta? <Link to="/login">Entrar</Link></span>
        </div>
      </AuthLayout>
  );
}

export default Cadastro;