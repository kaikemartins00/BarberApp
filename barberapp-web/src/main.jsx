import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter, Routes, Route } from 'react-router-dom';

import Login from './pages/LoginECadastro/Login.jsx';
import Home from './pages/Home/Home.jsx';
import Cadastro from './pages/LoginECadastro/Cadastro.jsx';
import VerificarConta from './pages/LoginECadastro/VerificarConta.jsx';
import Agendamento from './pages/Agendamento/Agendamento.jsx';
import HistAgendamento from './pages/Agendamento/HistAgendamento.jsx';
import Admin from './pages/Admin/Admin.jsx';
import PainelBarbeiro from './pages/Barbeiro/PainelBarbeiro.jsx';
import ComoAgendar from './pages/Info/ComoAgendar.jsx';
import Duvidas from './pages/Info/Duvidas.jsx';
import Problemas from './pages/Info/Problemas.jsx';
import Chat from './pages/Chat/Chat.jsx';
import Chamados from './pages/Chamados/Chamados.jsx';
import Mensagens from './pages/Mensagens/Mensagens.jsx';
import RotaProtegida from './components/RotaProtegida.jsx';
import EsqueciSenha from './pages/LoginECadastro/EsqueciSenha.jsx';

import './index.css';

createRoot(document.getElementById('root')).render(
    <StrictMode>
        <BrowserRouter>
            <Routes>
                <Route path="/" element={<Home />} />
                <Route path="/home" element={<Home />} />
                <Route path="/login" element={<Login />} />
                <Route path="/cadastro" element={<Cadastro />} />
                <Route path="/verificar-conta" element={<VerificarConta />} />
                <Route path="/como-agendar" element={<ComoAgendar />} />
                <Route path="/duvidas" element={<Duvidas />} />
                <Route
                    path="/admin"
                    element={
                        <RotaProtegida roles={['ROLE_ADMIN']}>
                            <Admin />
                        </RotaProtegida>
                    }
                />
                <Route
                    path="/chamados"
                    element={
                        <RotaProtegida roles={['ROLE_BARBEIRO', 'ROLE_ADMIN']}>
                            <Chamados />
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
                <Route
                    path="/verificarConta"
                    element={
                        <RotaProtegida>
                            <VerificarConta />
                        </RotaProtegida>
                    }
                />
                <Route
                    path="/esqueci-senha"
                    element={<EsqueciSenha />}
                />
            </Routes>

        </BrowserRouter>
    </StrictMode>
);
