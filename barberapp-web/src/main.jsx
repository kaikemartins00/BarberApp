import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Login from "./pages/LoginECadastro/Login.jsx";
import Home from "./pages/Home/Home.jsx";
import Cadastro from "./pages/LoginECadastro/Cadastro.jsx";
import Agendamento from "./pages/Agendamento/Agendamento.jsx";
import HistAgendamento from "./pages/Agendamento/HistAgendamento.jsx";
import VerificarConta from "./pages/LoginECadastro/VerificarConta.jsx";
import RotaProtegida from "./components/RotaProtegida.jsx";
import Admin from "./pages/Admin/Admin.jsx";
import PainelBarbeiro from "./pages/Barbeiro/PainelBarbeiro.jsx";
import './index.css';

createRoot(document.getElementById('root')).render(
  <StrictMode>
      <BrowserRouter>
          <Routes>
              <Route path="/" element={<Home />} />

              <Route path="/Home" element={<Home />} />

              <Route path="/agendamento" element={<Agendamento />} />

              <Route path="/histAgendamento" element={<HistAgendamento />} />

              <Route path="/login" element={<Login />} />

              <Route path="/cadastro" element={<Cadastro />} />

              <Route path="/verificar-conta" element={<VerificarConta />} />

              <Route path="/admin" element={
                                        <RotaProtegida roles={['ROLE_ADMIN']}>
                                           <Admin />
                                       </RotaProtegida>
                                   } />

              <Route path="/barbeiro" element={
                                           <RotaProtegida roles={['ROLE_BARBEIRO', 'ROLE_ADMIN']}>
                                              <PainelBarbeiro />
                                          </RotaProtegida>} />
          </Routes>
      </BrowserRouter>
  </StrictMode>,
)
