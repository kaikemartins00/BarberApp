import { Title } from '../LoginECadastro/Style.js';
import Navbar from '../../components/Navbar.jsx';

function HistAgendamento() {
    return (
        <>
            <Navbar />
            <main className="page">
                <header className="page__head">
                    <Title>Meus agendamentos</Title>
                    <p className="muted">Em breve: seu histórico e os próximos horários.</p>
                </header>
            </main>
        </>
    );
}

export default HistAgendamento;