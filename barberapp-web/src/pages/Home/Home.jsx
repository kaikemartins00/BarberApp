import { useNavigate } from 'react-router-dom';
import Navbar from '../../components/Navbar.jsx';
import { BarberPole } from '../../components/Visuais.jsx';
import {lerSessao} from "../../auth/session.js";

//Blocos/Card que aparecem na tela
const ACOES_GERAIS = [
    { titulo: 'Agendar', texto: 'Escolha o barbeiro, o serviço e o horário.', rota: '/agendamento', destaque: true }, // destaque: true serve para aplicar o tamanho maior ou estilo diferente
    { titulo: 'Meus agendamentos', texto: 'Acompanhe seus horários marcados.', rota: '/histAgendamento' },
    { titulo: 'Como agendar', texto: 'Um passo a passo rápido.' },
    { titulo: 'Dúvidas', texto: 'Perguntas frequentes.' },
    { titulo: 'Problemas', texto: 'Fale com a gente.' },
];

const ACOES_ADMIN = [
    { titulo: 'Painel Admin', texto: 'Gerenciar barbeiros, relatórios e horários.', rota: '/admin' }
];

function Home() {
    const navigate = useNavigate();
    const sessao = lerSessao();
    console.log("Conteúdo da minha sessão no Front:", sessao);
    const isAdmin = sessao?.roles?.includes('ROLE_ADMIN');
    const acoesVisiveis = isAdmin ? [...ACOES_GERAIS, ...ACOES_ADMIN] : ACOES_GERAIS;

    return (
        <>
            {/* Renderiza a barra de navegação no topo da página. */}
            <Navbar />
            <main className="page"> {/* Abre a tag principal do layout (<main>) com a classe CSS "page" */}
                <section className="hero">
                    <div>
                        <h1>Seu horário, do seu <em>jeito</em>.</h1>
                        <p>Agende em segundos com o barbeiro que você prefere. Sem fila e sem ligação.</p>
                        <button className="btn btn--primary" onClick={() => navigate('/agendamento')}> {/* Cria o botão dourado "Agendar agora". */}
                            Agendar agora
                        </button>
                    </div>
                    <BarberPole />
                </section>

                <section className="acoes">
                    {/*  o método .map(). vai percorrer a lista ACOES  item por item. Para cada item encontrado, ele vai gerar a estrutura abaixo automaticamente.*/}

                    {acoesVisiveis.map((acao) => (
                        <button
                            key={acao.titulo}
                            className={`acao ${acao.destaque ? 'acao--destaque' : ''}`}
                            disabled={!acao.rota}
                            onClick={() => acao.rota && navigate(acao.rota)}
                        >
                            <strong>{acao.titulo}</strong>
                            <span>{acao.texto}</span>
                            <span className="acao__seta">{acao.rota ? 'Abrir →' : 'Em breve'}</span>
                        </button>
                    ))}
                </section>
            </main>
        </>
    );
}

export default Home;