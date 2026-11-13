import {useEffect, useState} from 'react';
import {Link, useLocation, useNavigate} from 'react-router-dom';
import {Scissors, LayoutDashboard, CalendarDays, Users, Wallet, Star, LogOut, Settings, Clock3} from 'lucide-react';
import api from '../../api/api.js';
import {lerSessao, sair} from '../../auth/session.js';
import PerfilProfissional from './PerfilProfissional.jsx';
import NotificacaoMensagens from '../notificacoes/NotificacaoMensagens.jsx';
import NotificacaoChamados from '../notificacoes/NotificacaoChamados.jsx';
import BotaoTema from "../layout/BotaoTema.jsx";
import MensagemFlutuante from '../notificacoes/MensagemFlutuante.jsx';
import '../../pages/Admin/Admin.css';
import './PainelProfissional.css';

export default function SidebarProfissional({children}) {
    const sessao = lerSessao();
    const navigate = useNavigate();
    const location = useLocation();
    const admin = sessao?.roles?.includes('ROLE_ADMIN');
    const barbeiro = sessao?.roles?.includes('ROLE_BARBEIRO');
    const [perfil, setPerfil] = useState(null);
    const [perfilAberto, setPerfilAberto] = useState(false);

    useEffect(() => {
        if (!barbeiro || !sessao?.userId) return;
        let ativo = true;
        api.get(`/barbeiros/${sessao.userId}`).then(r => {
            if (ativo) setPerfil(r.data);
        }).catch(() => {
        });
        return () => {
            ativo = false;
        };
    }, [barbeiro, sessao?.userId]);

    if (!admin && !barbeiro) return children;

    const nomeExibido = admin ? 'Administrador' : (perfil?.nome || 'Barbeiro');

    const inicio = admin ? '/admin' : '/barbeiro';
    // "Visão geral" só fica marcada sem ?secao=; as demais comparam o endereço inteiro
    const ativo = destino => destino.includes('?')
        ? location.pathname + location.search === destino
        : location.pathname === destino && !location.search;
    const item = (destino, Icone, texto) => (
        <Link key={destino} to={destino} className={`adm-nav-item ${ativo(destino) ? 'active' : ''}`}>
            <Icone size={18}/><span>{texto}</span>
        </Link>
    );

    function handleSair() {
        sair();
        navigate('/login');
    }

    return (
        <div className="adm-layout">
            <aside className="adm-sidebar">
                <div className="adm-sidebar-header">
                    <div className="adm-sidebar-topo">
                        <div className="adm-brand"><Scissors size={23}/><strong>Barber<span>App</span></strong></div>
                        <BotaoTema className="botao-tema--sidebar"/>
                    </div>
                    <small>{admin ? 'PAINEL ADMINISTRATIVO' : 'PAINEL DO BARBEIRO'}</small>
                </div>
                <nav className="adm-nav">
                    <span className="adm-nav-label">{admin ? 'GERENCIAMENTO' : 'MEU TRABALHO'}</span>
                    {item(inicio, LayoutDashboard, 'Visão geral')}
                    {admin ? <>
                        {item('/admin?secao=barbeiros', Users, 'Barbeiros')}
                        {item('/admin?secao=servicos', Scissors, 'Serviços e preços')}
                        {item('/admin?secao=horarios', Clock3, 'Horários')}
                        {item('/admin?secao=financeiro', Wallet, 'Financeiro')}
                        {item('/admin?secao=avaliacoes', Star, 'Avaliações')}
                        {item('/admin?secao=configuracoes', Settings, 'Configurações')}
                    </> : <>
                        {item('/barbeiro?secao=agenda', CalendarDays, 'Minha agenda')}
                        {item('/barbeiro?secao=clientes', Users, 'Meus clientes')}
                        {item('/barbeiro?secao=servicos', Scissors, 'Serviços e horários')}
                        {item('/barbeiro?secao=financeiro', Wallet, 'Meu financeiro')}
                        {item('/barbeiro?secao=avaliacoes', Star, 'Avaliações')}
                    </>}
                    <span className="adm-nav-label adm-nav-label--extra">COMUNICAÇÃO</span>
                    {!admin && <Link to="/mensagens"
                                     className={`adm-nav-item ${location.pathname === '/mensagens' ? 'active' : ''}`}><NotificacaoMensagens/></Link>}
                    <Link to="/chamados" className={`adm-nav-item ${location.pathname === '/chamados' ? 'active' : ''}`}><NotificacaoChamados/></Link>
                </nav>
                <div className="adm-sidebar-footer">
                    <button type="button" className="adm-perfil-btn" onClick={() => setPerfilAberto(true)}
                            title="Abrir meu perfil">
                        <span className="adm-admin-avatar">{nomeExibido[0].toUpperCase()}{perfil?.fotoUrl &&
                            <img src={perfil.fotoUrl} alt="" onError={e => e.currentTarget.remove()}/>}</span>
                        <div>
                            <strong>{nomeExibido}</strong><small>{sessao.email}</small>
                        </div>
                    </button>
                    <button className="adm-btn adm-btn--outline" type="button" onClick={handleSair}><LogOut
                        size={16}/> Sair
                    </button>
                </div>
            </aside>
            <main className="adm-main">{children}</main>
            {!admin && location.pathname !== '/mensagens' && <MensagemFlutuante/>}
            {perfilAberto && <PerfilProfissional perfil={perfil} nome={nomeExibido} admin={admin}
                                                 onFechar={() => setPerfilAberto(false)}
                                                 onFoto={fotoUrl => setPerfil(p => ({...p, fotoUrl}))}/>}
        </div>
    );
}
