import { useState, useEffect } from "react";
import api from "../../api/api.js";

function Admin() {
    const [barbeiros, setBarbeiros] = useState([]);
    const [nome, setNome] = useState('');
    const [email, setEmail] = useState('');
    const [telefone, setTelefone] = useState('');
    const [senha, setSenha] = useState('');
    const [fotoUrl, setFotoUrl] = useState('');
    const [erro, setErro] = useState('');
    const [sucesso, setSucesso] = useState('');
    const [carregando, setCarregando] = useState(false);

    async function carregarBarbeiros() {
        try {
            const response = await api.get('/barbeiros');
            setBarbeiros(response.data);
        } catch (erro) {
            console.error("Erro ao carregar barbeiros:", erro);
        }
    }

    useEffect(() => {
        carregarBarbeiros();
    }, []);

    async function handleCadastroBarbeiro(event) {
        event.preventDefault();
        setErro('');
        setSucesso('');
        setCarregando(true);

        const fotoEnviar = fotoUrl.trim() === '' ? null : fotoUrl;

        try {
            await api.post('/auth/cadastro-barbeiro', { nome, email, senha, telefone, fotoUrl: fotoEnviar });

            setSucesso('Barbeiro cadastrado com sucesso!');

            setNome('');
            setEmail('');
            setSenha('');
            setFotoUrl('');

            // SINCRONIZAÇÃO: Recarrega a lista de baixo automaticamente sem atualizar a página
            carregarBarbeiros();

        } catch (erro) {
            const mensagemErro = erro.response?.data?.message || 'Erro ao cadastrar barbeiro. Verifique os dados.';
            setErro(mensagemErro);
            setErro('Erro ao cadastrar barbeiro. Verifique os dados.', erro);
        } finally {
            setCarregando(false);
        }
    }

    return (
        <div className="page" style={{ maxWidth: '900px', paddingTop: '60px', display: 'flex', flexDirection: 'column', gap: '24px' }}>

            <section className="panel">
                <h2>Criar Barbeiro</h2>
                <form onSubmit={handleCadastroBarbeiro}>
                    <div className="form-grid">
                        <label className="field">
                            <span>Nome</span>
                            <input type="text" placeholder="Seu nome" value={nome}
                                   onChange={(e) => setNome(e.target.value)} />
                        </label>

                        <label className="field">
                            <span>Email</span>
                            <input type="email" placeholder="seu@email.com" value={email}
                                   onChange={(e) => setEmail(e.target.value)} />
                        </label>

                        <label className="field">
                            <span>Senha</span>
                            <input type="password" placeholder="••••••••" value={senha}
                                   onChange={(e) => setSenha(e.target.value)} />
                        </label>

                        <label className="field">
                            <span>Telefone</span>
                            <input type="text" placeholder="(99) 9 9999-9999" value={telefone}
                                   onChange={(e) => setTelefone(e.target.value)} />
                        </label>

                        <label className="field">
                            <span>FotoUrl</span>
                            <input type="text" placeholder="URL da foto" value={fotoUrl}
                                   onChange={(e) => setFotoUrl(e.target.value)} />
                        </label>
                    </div>

                    {erro && <p className="alert alert--erro full" style={{ marginTop: '14px' }}>{erro}</p>}
                    {sucesso && <p className="alert alert--ok full" style={{ marginTop: '14px' }}>{sucesso}</p>}

                    <button type="submit" className="btn btn--primary btn--block full" disabled={carregando} style={{ marginTop: '14px' }}>
                        {carregando ? 'Criando...' : 'Criar'}
                    </button>
                </form>
            </section>

            {/* SEGUNDA CAIXA: LISTAGEM DO MÉTODO GET */}
            <section className="panel">
                <h2>Barbeiros Cadastrados</h2>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', marginTop: '14px' }}>
                    {barbeiros.length === 0 ? (
                        <p className="muted">Nenhum barbeiro cadastrado no momento.</p>
                    ) : (
                        barbeiros.map((barbeiro) => (
                            <div key={barbeiro.id} style={{
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'space-between',
                                padding: '14px',
                                background: 'var(--surface-2)',
                                borderRadius: '10px',
                                border: '1px solid var(--line)'
                            }}>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
                                    <img
                                        src={barbeiro.fotoUrl || 'https://placeholder.com'}
                                        alt={barbeiro.nome}
                                        style={{ width: '44px', height: '44px', borderRadius: '50%', objectFit: 'cover' }}
                                    />
                                    <div style={{ display: 'flex', flexDirection: 'column' }}>
                                        <strong style={{ color: 'var(--text)' }}>{barbeiro.nome}</strong>
                                        <span className="muted" style={{ fontSize: '0.85rem' }}>{barbeiro.email}</span>
                                    </div>
                                </div>
                                <span className="small muted">ID: {barbeiro.id}</span>
                            </div>
                        ))
                    )}
                </div>
            </section>

        </div>
    );
}

export default Admin;