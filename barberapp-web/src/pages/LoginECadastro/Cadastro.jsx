import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import api from '../../api/api.js';
import AuthLayout from "../../components/AuthLayout.jsx";
import { VERSAO_TERMOS } from '../../legal/dadosLegais.js';

function Cadastro() {
  const navigate = useNavigate();
  const [nome, setNome] = useState('');
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [confirmarSenha, setConfirmarSenha] = useState('');
  const [erro, setErro] = useState('');
  const [carregando, setCarregando] = useState(false);
  const [aceitou, setAceitou] = useState(false);

  async function handleCadastro(event) { // Função assíncrona para lidar com o envio do formulário de cadastro
    event.preventDefault(); // Impede o comportamento padrão do navegador de recarregar a página ao enviar o formulário
    setErro('');

    const senhaForteRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{6,}$/;
    if (!senhaForteRegex.test(senha)) {
      setErro("A senha deve ter no mínimo 6 caracteres, com pelo menos uma letra maiúscula, uma minúscula e um número.");
      return;
    }

    if (senha !== confirmarSenha) {
      setErro('As senhas não coincidem.');
      return;
    }

    if (!aceitou) {
      setErro('Para criar a conta, aceite os Termos de Uso e a Política de Privacidade.');
      return;
    }

    setCarregando(true);

    try {
      await api.post('/auth/cadastro', { nome, email, senha, aceitouTermos: true, versaoTermos: VERSAO_TERMOS });
      navigate('/verificar-conta', {
        state: { email }
      });

    } catch (error) {
      setErro(error.response?.data?.message || "Não foi possível cadastrar.");
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
            <span>Repita a senha</span>
            <input type="password" placeholder="••••••••" value={confirmarSenha}
                   onChange={(e) => setConfirmarSenha(e.target.value)} required />
          </label>

          <label className="field" style={{ flexDirection: 'row', alignItems: 'flex-start', gap: 10 }}>
            <input type="checkbox" checked={aceitou} onChange={(e) => setAceitou(e.target.checked)} style={{ marginTop: 4 }} />
            <span style={{ fontSize: 14, lineHeight: 1.5 }}>
              Li e aceito os <Link to="/termos" target="_blank">Termos de Uso</Link>, a{' '}
              <Link to="/reembolso" target="_blank">Política de Cancelamento e Reembolso</Link> e a{' '}
              <Link to="/privacidade" target="_blank">Política de Privacidade</Link>.
            </span>
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