import { Link } from 'react-router-dom';
import Navbar from '../../components/Navbar.jsx';
import { Title } from '../LoginECadastro/Style.js';

const PASSOS = [
    { titulo: 'Entre na sua conta', texto: 'Faça login ou crie uma conta. Você confirma o e-mail com um código.' },
    { titulo: 'Escolha o barbeiro', texto: 'Veja quem atende e escolha o profissional que você prefere.' },
    { titulo: 'Escolha o serviço', texto: 'Cada barbeiro mostra os serviços que faz e o preço.' },
    { titulo: 'Escolha dia e horário', texto: 'Selecione o dia e um dos horários do barbeiro.' },
    { titulo: 'Confirme', texto: 'Pronto. O agendamento aparece em "Meus agendamentos".' },
];

function ComoAgendar() {
    return (
        <>
            <Navbar />
            <main className="page">
                <header className="page__head">
                    <Title>Como agendar</Title>
                    <p className="muted">Em cinco passos rápidos.</p>
                </header>

                <section className="panel">
                    <ol className="steps">
                        {PASSOS.map((passo) => (
                            <li key={passo.titulo}>
                                <div>
                                    <strong>{passo.titulo}</strong>
                                    <p className="muted" style={{ margin: '4px 0 0' }}>{passo.texto}</p>
                                </div>
                            </li>
                        ))}
                    </ol>
                </section>

                <Link to="/agendamento" className="btn btn--primary">Agendar agora</Link>
            </main>
        </>
    );
}

export default ComoAgendar;