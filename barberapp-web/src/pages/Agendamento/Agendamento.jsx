import { Title } from '../LoginECadastro/Style.js';
import Navbar from '../../components/Navbar.jsx';

function Agendamento() {
    return (
        <>
            <Navbar />
            <main className="page">
                <header className="page__head">
                    <Title>Agende seu horário</Title>
                    <p className="muted">Em breve: escolha barbeiro, serviço e horário.</p>
                </header>
            </main>
        </>


    );

}

export default Agendamento;