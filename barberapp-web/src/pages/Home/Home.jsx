import {Title} from "../LoginECadastro/Style.js";
import {useState} from "react";
import {useNavigate} from "react-router-dom";


function Home(){
    const navigate = useNavigate();
    const [erro, setErro] = useState('');
    const [carregando, setCarregando] = useState(false);

    return (
        <div style={{ maxWidth: '320px', margin: '80px auto' }}>
            <Title>BEM-VINDO AO BARBERWEB</Title>


                {erro && <p style={{ color: 'red' }}>{erro}</p>}


            <button onClick={function handleClique(){navigate('/Agendamento/agendamento')}} type="submit" disabled={carregando} style={{ marginTop: '16px' }}>
                    {carregando ? 'Cadastrando...' : 'Agendar'}
            </button>
            <button onClick={function handleClique(){navigate('/Agendamento/histAgendamento')}} type="submit" disabled={carregando} style={{ marginTop: '16px' }}>
                {carregando ? 'Cadastrando...' : 'Ver Agendamentos'}
            </button>
            <button type="submit" disabled={carregando} style={{ marginTop: '16px' }}>
                {carregando ? 'Cadastrando...' : 'Duvidas'}
            </button>
            <button type="submit" disabled={carregando} style={{ marginTop: '16px' }}>
                {carregando ? 'Cadastrando...' : 'Como Agendar'}
            </button>
            <button type="submit" disabled={carregando} style={{ marginTop: '16px' }}>
                {carregando ? 'Cadastrando...' : 'Problemas' }
            </button>
        </div>
    );
}
export default Home;
