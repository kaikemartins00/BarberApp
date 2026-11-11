
import { useState } from 'react';
import { useLocation, Link } from 'react-router-dom';
import api from '../../api/api.js';
import AuthLayout from '../../components/AuthLayout.jsx';

function EsqueciSenha() {
    const location = useLocation();

    const [etapa, setEtapa] = useState(1);

    const [email, setEmail] = useState(
        location.state?.email || ''
    );

    const [codigo, setCodigo] = useState('');
    const [novaSenha, setNovaSenha] = useState('');
    const [confirmarSenha, setConfirmarSenha] = useState('');

    const [erro, setErro] = useState('');
    const [sucesso, setSucesso] = useState('');
    const [carregando, setCarregando] = useState(false);

    async function handleEnviarCodigo(event) {
        event.preventDefault();

        setErro('');
        setSucesso('');
        setCarregando(true);

        try {
            await api.post('/auth/esqueci-senha', {
                email: email.trim()
            });

            setSucesso('');
            setEtapa(2);

        } catch (error) {
            console.error('Erro ao solicitar código:', error);

            setErro(
                error.response?.data?.message ||
                'Não foi possível solicitar o código. Tente novamente.'
            );

        } finally {
            setCarregando(false);
        }
    }

    async function handleRedefinirSenha(event) {
        event.preventDefault();

        setErro('');
        setSucesso('');

        if (codigo.trim().length !== 6) {
            setErro('Informe o código de 6 dígitos recebido por e-mail.');
            return;
        }

        const senhaForteRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{6,}$/;
        if (!senhaForteRegex.test(novaSenha)) {
            setErro('A senha deve ter no mínimo 6 caracteres, com pelo menos uma letra maiúscula, uma minúscula e um número.');
            return;
        }

        if (novaSenha !== confirmarSenha) {
            setErro('As senhas não coincidem.');
            return;
        }

        setCarregando(true);

        try {
            await api.post('/auth/redefinir-senha', {
                email: email.trim(),
                codigo: codigo.trim(),
                novaSenha: novaSenha
            });

            setSucesso('Senha alterada com sucesso! Você já pode entrar.');

            setCodigo('');
            setNovaSenha('');
            setConfirmarSenha('');

        } catch (error) {
            console.error('Erro ao redefinir senha:', error);

            setErro(
                error.response?.data?.message ||
                'Código inválido ou expirado. Tente novamente.'
            );

        } finally {
            setCarregando(false);
        }
    }

    return (
        <AuthLayout
            titulo={
                etapa === 1
                    ? 'Recuperar senha'
                    : 'Criar nova senha'
            }
            subtitulo={
                etapa === 1
                    ? 'Informe seu e-mail para receber o código de recuperação.'
                    : `Enviamos um código para ${email}. Informe o código e sua nova senha.`
            }
        >
            {etapa === 1 ? (

                <form
                    className="form"
                    onSubmit={handleEnviarCodigo}
                >
                    <label className="field">
                        <span>E-mail cadastrado</span>

                        <input
                            type="email"
                            placeholder="voce@email.com"
                            value={email}
                            onChange={(event) =>
                                setEmail(event.target.value)
                            }
                            required
                        />
                    </label>

                    {erro && (
                        <p className="alert alert--erro">
                            {erro}
                        </p>
                    )}

                    <button
                        type="submit"
                        className="btn btn--primary btn--block"
                        disabled={carregando}
                    >
                        {carregando
                            ? 'Enviando código...'
                            : 'Enviar código'}
                    </button>
                </form>

            ) : (

                <form
                    className="form"
                    onSubmit={handleRedefinirSenha}
                >
                    <label className="field">
                        <span>Código recebido por e-mail</span>

                        <input
                            type="text"
                            placeholder="000000"
                            value={codigo}
                            onChange={(event) =>
                                setCodigo(
                                    event.target.value
                                        .replace(/\D/g, '')
                                        .slice(0, 6)
                                )
                            }
                            maxLength={6}
                            inputMode="numeric"
                            required
                        />
                    </label>

                    <label className="field">
                        <span>Nova senha</span>

                        <input
                            type="password"
                            placeholder="Digite sua nova senha"
                            value={novaSenha}
                            onChange={(event) =>
                                setNovaSenha(event.target.value)
                            }
                            required
                        />
                    </label>

                    <label className="field">
                        <span>Confirmar nova senha</span>

                        <input
                            type="password"
                            placeholder="Digite novamente sua senha"
                            value={confirmarSenha}
                            onChange={(event) =>
                                setConfirmarSenha(event.target.value)
                            }
                            required
                        />
                    </label>

                    {erro && (
                        <p className="alert alert--erro">
                            {erro}
                        </p>
                    )}

                    {sucesso && (
                        <p className="alert alert--ok">
                            {sucesso}
                        </p>
                    )}

                    <button
                        type="submit"
                        className="btn btn--primary btn--block"
                        disabled={carregando || !!sucesso}
                    >
                        {carregando
                            ? 'Alterando senha...'
                            : 'Redefinir senha'}
                    </button>

                    {sucesso && (
                        <Link
                            to="/login"
                            className="btn btn--ghost btn--block"
                        >
                            Ir para o login
                        </Link>
                    )}

                    {!sucesso && (
                        <button
                            type="button"
                            className="btn btn--ghost btn--block"
                            onClick={() => {
                                setEtapa(1);
                                setCodigo('');
                                setErro('');
                                setSucesso('');
                            }}
                        >
                            Voltar
                        </button>
                    )}
                </form>

            )}

            <div className="auth__rodape">
                <Link to="/login">
                    Voltar ao login
                </Link>
            </div>
        </AuthLayout>
    );
}

export default EsqueciSenha;
