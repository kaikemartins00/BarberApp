
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter, Routes, Route } from 'react-router-dom';

import Home from './pages/Home/Home.jsx';
import Login from './pages/LoginECadastro/Login.jsx';
import Cadastro from './pages/LoginECadastro/Cadastro.jsx';
import VerificarConta from './pages/LoginECadastro/VerificarConta.jsx';
import EsqueciSenha from './pages/LoginECadastro/EsqueciSenha.jsx';

import Admin from './pages/Admin/Admin.jsx';
import PainelBarbeiro from './pages/Barbeiro/PainelBarbeiro.jsx';

import Agendamento from './pages/Agendamento/Agendamento.jsx';
import HistAgendamento from './pages/Agendamento/HistAgendamento.jsx';

import ComoAgendar from './pages/Info/ComoAgendar.jsx';
import Duvidas from './pages/Info/Duvidas.jsx';
import Problemas from './pages/Info/Problemas.jsx';

import Chat from './pages/Chat/Chat.jsx';
import Chamados from './pages/Chamados/Chamados.jsx';
import Mensagens from './pages/Mensagens/Mensagens.jsx';

import Termos from './pages/Legal/Termos.jsx';
import Privacidade from './pages/Legal/Privacidade.jsx';
import Cookies from './pages/Legal/Cookies.jsx';
import Reembolso from './pages/Legal/Reembolso.jsx';
import BannerCookies from './components/BannerCookies.jsx';
import RodapeLegal from './components/RodapeLegal.jsx';
import RegrasAgendamento from './pages/Legal/RegrasAgendamento.jsx';
import Comunicacao from './pages/Legal/Comunicacao.jsx';

import RotaProtegida from './components/RotaProtegida.jsx';
import SidebarProfissional from './components/layout/SidebarProfissional.jsx';
import { ThemeProvider } from './theme/ThemeContext.jsx';

import './index.css';
import './theme/Theme.css';

createRoot(document.getElementById('root')).render(
    <StrictMode>
        <ThemeProvider>
            <BrowserRouter>
                <Routes>
                    <Route path="/" element={<Home />} />
                    <Route path="/home" element={<Home />} />
                    <Route path="/login" element={<Login />} />
                    <Route path="/cadastro" element={<Cadastro />} />
                    <Route path="/verificar-conta" element={<VerificarConta />} />
                    <Route path="/verificarConta" element={<VerificarConta />} />
                    <Route path="/esqueci-senha" element={<EsqueciSenha />} />
                    <Route path="/como-agendar" element={<ComoAgendar />} />
                    <Route path="/duvidas" element={<Duvidas />} />
                    <Route path="/termos" element={<Termos />} />
                    <Route path="/privacidade" element={<Privacidade />} />
                    <Route path="/cookies" element={<Cookies />} />
                    <Route path="/reembolso" element={<Reembolso />} />
                    <Route path="/regras-de-agendamento" element={<RegrasAgendamento />} />
                    <Route path="/comunicacao" element={<Comunicacao />} />

                    <Route
                        path="/admin"
                        element={
                            <RotaProtegida roles={['ROLE_ADMIN']}>
                                <Admin />
                            </RotaProtegida>
                        }
                    />

                    <Route
                        path="/barbeiro"
                        element={
                            <RotaProtegida roles={['ROLE_BARBEIRO']}>
                                <PainelBarbeiro />
                            </RotaProtegida>
                        }
                    />

                    <Route
                        path="/chamados"
                        element={
                            <RotaProtegida roles={['ROLE_BARBEIRO', 'ROLE_ADMIN']}>
                                <SidebarProfissional>
                                    <Chamados />
                                </SidebarProfissional>
                            </RotaProtegida>
                        }
                    />

                    <Route
                        path="/agendamento"
                        element={
                            <RotaProtegida roles={['ROLE_CLIENTE']}>
                                <Agendamento />
                            </RotaProtegida>
                        }
                    />

                    <Route
                        path="/histAgendamento"
                        element={
                            <RotaProtegida roles={['ROLE_CLIENTE']}>
                                <HistAgendamento />
                            </RotaProtegida>
                        }
                    />

                    <Route
                        path="/problemas"
                        element={
                            <RotaProtegida roles={['ROLE_CLIENTE']}>
                                <Problemas />
                            </RotaProtegida>
                        }
                    />

                    <Route
                        path="/chat"
                        element={
                            <RotaProtegida roles={['ROLE_CLIENTE']}>
                                <Chat />
                            </RotaProtegida>
                        }
                    />

                    <Route
                        path="/mensagens"
                        element={
                            <RotaProtegida roles={['ROLE_CLIENTE', 'ROLE_BARBEIRO']}>
                                <Mensagens />
                            </RotaProtegida>
                        }
                    />
                </Routes>
                <RodapeLegal />
                <BannerCookies />
            </BrowserRouter>
        </ThemeProvider>
    </StrictMode>
);
