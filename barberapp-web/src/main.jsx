import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Login from "./pages/LoginECadastro/Login.jsx";
import Home from "./pages/Home/Home.jsx";
import Cadastro from "./pages/LoginECadastro/Cadastro.jsx";
import Agendamento from "./pages/Agendamento/Agendamento.jsx";
import HistAgendamento from "./pages/Agendamento/HistAgendamento.jsx";
import VerificarConta from "./pages/LoginECadastro/VerificarConta.jsx";

createRoot(document.getElementById('root')).render(
  <StrictMode>
      <BrowserRouter>
          <Routes>
              <Route path="/" element={<Home />} />

              <Route path="/Home" element={<Home />} />

              <Route path="/agendamentos" element={<Agendamento />} />

              <Route path="/histAgendamentos" element={<HistAgendamento />} />

              <Route path="/login" element={<Login />} />

              <Route path="/cadastro" element={<Cadastro />} />

              <Route path="/verificar-conta" element={<VerificarConta />} />
          </Routes>
      </BrowserRouter>
  </StrictMode>,
)
