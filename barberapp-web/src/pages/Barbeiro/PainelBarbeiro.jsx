import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Title } from '../LoginECadastro/Style.js';
import api from '../../api/api.js';
import { lerSessao, sair } from '../../auth/session.js';
import Navbar from "../../components/Navbar.jsx";

function PainelBarbeiro() {
    const navigate = useNavigate();
    const sessao = lerSessao();
    const [barbeiro, setBarbeiro] = useState(null);
    const [erro, setErro] = useState('');

    useEffect(() => {
        if (sessao?.userId == null) return;

        api.get(`/barbeiros/${sessao.userId}`)
            .then((r) => setBarbeiro(r.data))
            .catch(() => setErro('Não foi possível carregar seus dados.'));
    }, []);

    function handleSair() {
        sair();
        navigate('/login');
    }

    return (
        <>
            <Navbar />
            <main className="page">
                <header className="page__head">
                    <Title>{barbeiro ? `Olá, ${barbeiro.nome}` : 'Minha área'}</Title>
                    <p className="muted">Estes são os serviços e horários definidos pelo administrador.</p>
                </header>

                {erro && <p className="alert alert--erro">{erro}</p>}
                {sessao?.userId == null && (
                    <p className="alert alert--erro">Não encontrei o seu id no login. Confira a função salvarSessao.</p>
                )}

                {barbeiro && (
                    <>
                        <section className="panel">
                            <h2>Meus serviços</h2>
                            <div className="chips">
                                {(barbeiro.servicos || []).length === 0 && (
                                    <span className="muted">Nenhum serviço atribuído ainda.</span>
                                )}
                                {(barbeiro.servicos || []).map((s) => (
                                    <span key={s.id} className="chip chip--on">{s.nome}</span>
                                ))}
                            </div>
                        </section>

                        <section className="panel">
                            <h2>Meus horários</h2>
                            <div className="chips">
                                {(barbeiro.horarios || []).length === 0 && (
                                    <span className="muted">Nenhum horário definido ainda.</span>
                                )}
                                {(barbeiro.horarios || []).map((h) => (
                                    <span key={h} className="chip chip--on">{String(h).slice(0, 5)}</span>
                                ))}
                            </div>
                        </section>

                        <p className="muted small">Para alterar serviços ou horários, fale com o administrador.</p>
                    </>
                )}
            </main>
        </>
    );
}

export default PainelBarbeiro;