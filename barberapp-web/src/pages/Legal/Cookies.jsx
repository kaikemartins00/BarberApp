import { Link } from 'react-router-dom';
import { EMPRESA } from '../../legal/dadosLegais.js';
import PaginaLegal from './PaginaLegal.jsx';

const secoes = [
    {
        id: 'o-que-sao', titulo: 'O que são cookies e armazenamento local',
        conteudo: <>
            <p>Cookies são pequenos arquivos que um site grava no seu navegador. Tecnologias semelhantes, como o
                armazenamento local (<em>localStorage</em>), cumprem funções parecidas: guardar informações para o site
                funcionar e lembrar escolhas suas.</p>
            <p>Esta política explica o que o {EMPRESA.nomeFantasia} grava no seu navegador, para quê, e como você
                controla isso.</p>
        </>,
    },
    {
        id: 'o-que-usamos', titulo: 'O que usamos',
        conteudo: <>
            <table className="legal__tabela">
                <thead><tr><th>Item</th><th>Categoria</th><th>Para quê</th><th>Duração</th></tr></thead>
                <tbody>
                <tr><td>Sessão de login (token e dados básicos da conta)</td><td>Essencial</td>
                    <td>Manter você conectado e proteger o acesso à sua conta.</td>
                    <td>Até você sair da conta ou a sessão expirar.</td></tr>
                <tr><td>Registro da sua escolha de cookies</td><td>Essencial</td>
                    <td>Lembrar o que você decidiu neste aviso, para não perguntar de novo.</td>
                    <td>Até a política mudar de versão ou você limpar os dados do navegador.</td></tr>
                <tr><td>Tema (claro ou escuro)</td><td>Preferências</td>
                    <td>Lembrar o visual que você escolheu.</td>
                    <td>Até você limpar os dados do navegador. Só é gravado se você permitir.</td></tr>
                </tbody>
            </table>
            <p><strong>Não usamos</strong> cookies de publicidade, de análise de comportamento nem de rastreamento entre
                sites.</p>
        </>,
    },
    {
        id: 'base-legal', titulo: 'Base legal',
        conteudo: <p>Os itens <strong>essenciais</strong> são necessários para prestar o serviço que você pediu e não
            dependem de consentimento. Os de <strong>preferências</strong> dependem do seu consentimento, que você
            pode dar ou retirar quando quiser (LGPD, art. 7º, I e art. 8º, §5º).</p>,
    },
    {
        id: 'o-que-acontece', titulo: 'O que acontece se eu recusar',
        conteudo: <ul>
            <li><strong>Somente essenciais:</strong> o site funciona normalmente. O tema volta ao padrão a cada visita,
                porque não gravamos a sua preferência.</li>
            <li><strong>Aceitar todos:</strong> o site lembra o tema escolhido.</li>
            <li>Você pode mudar de ideia quando quiser: ao retirar o consentimento, apagamos a preferência gravada.</li>
            <li>Se a Política mudar de versão, o aviso aparece de novo para uma nova escolha.</li>
        </ul>,
    },
    {
        id: 'seguranca', titulo: 'Computadores compartilhados e e-mails',
        conteudo: <ul>
            <li>Em computador de uso compartilhado, saia da conta ao terminar (botão "Sair"), para apagar os dados da sessão.</li>
            <li>Nossos e-mails são enviados em texto simples e não usam imagens de rastreamento (pixels).</li>
            <li>O site não grava cookies HTTP próprios hoje. Os dados citados ficam no armazenamento local do navegador.</li>
        </ul>,
    },
    {
        id: 'terceiros', titulo: 'Cookies de terceiros',
        conteudo: <p>Ao pagar por Pix, o ambiente do Mercado Pago pode gravar os próprios cookies para segurança e
            prevenção a fraudes. Eles são controlados por essa empresa, conforme as políticas dela. O
            {' '}{EMPRESA.nomeFantasia} não tem acesso a esses cookies.</p>,
    },
    {
        id: 'gerenciar', titulo: 'Como gerenciar suas escolhas',
        conteudo: <>
            <ul>
                <li>Use o botão abaixo para reabrir o aviso e mudar suas preferências a qualquer momento.</li>
                <li>Você também pode apagar os dados do site nas configurações do seu navegador. Isso encerra sua
                    sessão e faz o aviso aparecer de novo.</li>
                <li>Bloquear os itens essenciais impede o login e o uso da área de cliente.</li>
            </ul>
            <p>Mais informações sobre o tratamento de dados estão na <Link to="/privacidade">Política de
                Privacidade</Link>. Dúvidas: {EMPRESA.emailPrivacidade}.</p>
        </>,
    },
];

export default function Cookies() {
    return (
        <PaginaLegal atual="/cookies" titulo="Política de Cookies"
                     resumo="O que guardamos no seu navegador e como você controla isso."
                     secoes={secoes}
                     extra={<div className="legal__acoes">
                         <button type="button" className="btn btn--primary"
                                 onClick={() => window.dispatchEvent(new Event('barberapp:abrir-cookies'))}>
                             Alterar minhas preferências de cookies
                         </button>
                     </div>} />
    );
}
