import { Link } from 'react-router-dom';
import Navbar from '../../components/Navbar.jsx';
import { Title } from '../LoginECadastro/Style.js';

// Troque as respostas pelas regras reais da sua barbearia.
const PERGUNTAS = [
    { p: 'Preciso de conta para agendar?', r: 'Sim. Com a conta você acompanha e consulta seus horários.' },
    { p: 'Posso agendar para hoje?', r: 'O agendamento é feito a partir do dia seguinte.' },
    { p: 'Por que preciso verificar o e-mail?', r: 'Para garantir que a conta é sua. Enviamos um código para o e-mail cadastrado.' },
    { p: 'Não recebi o código. E agora?', r: 'Na tela de verificação, use "Reenviar código". Confira também a caixa de spam.' },
    { p: 'O horário que eu queria não aparece.', r: 'Cada barbeiro define seus horários. Tente outro dia ou outro barbeiro.' },
    { p: 'Como vejo meus agendamentos?', r: 'Na página inicial, em "Meus agendamentos".' },
];

function Duvidas() {
    return (
        <>
            <Navbar />
            <main className="page">
                <header className="page__head">
                    <Title>Dúvidas</Title>
                    <p className="muted">As perguntas mais comuns.</p>
                </header>

                <section className="panel faq">
                    {PERGUNTAS.map((item) => (
                        <details key={item.p}>
                            <summary>{item.p}</summary>
                            <p>{item.r}</p>
                        </details>
                    ))}
                </section>

                <p className="muted">
                    Não achou o que procurava? <Link to="/problemas">Fale com a gente</Link>.
                </p>
            </main>
        </>
    );
}

export default Duvidas;