import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Login from './pages/Login';
import Cadastro from './pages/Cadastro';
import VerificarConta from './pages/VerificarConta';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Login />} />

        <Route path="/login" element={<Login />} />

        <Route path="/cadastro" element={<Cadastro />} />
        
        <Route path="/verificar-conta" element={<VerificarConta />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;