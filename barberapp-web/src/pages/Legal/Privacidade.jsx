import { Link } from 'react-router-dom';
import { EMPRESA, identificacao } from '../../legal/dadosLegais.js';
import PaginaLegal from './PaginaLegal.jsx';

const secoes = [
    {
        id: 'controlador', titulo: 'Quem é o responsável pelos seus dados',
        conteudo: <>
            <p>O controlador dos dados pessoais é {identificacao()}.</p>
            <p>Canal para assuntos de privacidade e proteção de dados: {EMPRESA.emailPrivacidade}.</p>
        </>,
    },
    {
        id: 'dados', titulo: 'Quais dados coletamos',
        conteudo: <>
            <table className="legal__tabela">
                <thead><tr><th>Dado</th><th>Quando</th></tr></thead>
                <tbody>
                <tr><td>Nome, e-mail e senha (guardada de forma criptografada, nunca em texto)</td><td>No cadastro</td></tr>
                <tr><td>Código de verificação do e-mail</td><td>No cadastro e na recuperação de senha</td></tr>
                <tr><td>Data e versão do aceite dos Termos e da Política de Privacidade</td><td>No cadastro</td></tr>
                <tr><td>Agendamentos: serviço, profissional, data, horário, status, cancelamentos</td><td>Ao usar o serviço</td></tr>
                <tr><td>Pagamentos: valor, método, status e identificador da transação (não guardamos dados de cartão nem de conta bancária)</td><td>Ao pagar</td></tr>
                <tr><td>Preferências de corte (ficha de atendimento), se você preencher</td><td>Por escolha sua</td></tr>
                <tr><td>Mensagens do chat com o barbeiro, chamados de suporte e avaliações</td><td>Ao usar esses recursos</td></tr>
                <tr><td>Mensagens enviadas ao assistente virtual com IA e as respostas</td><td>Ao usar o assistente</td></tr>
                <tr><td>Dados técnicos de acesso gerados pelo servidor (como registros de uso e data/hora)</td><td>Ao acessar o site</td></tr>
                </tbody>
            </table>
            <p>Não coletamos o seu telefone nem dados sensíveis de forma intencional. Evite escrevê-los em
                mensagens, chamados ou na conversa com a IA.</p>
        </>,
    },
    {
        id: 'finalidades', titulo: 'Para que usamos e com qual base legal',
        conteudo: <table className="legal__tabela">
            <thead><tr><th>Finalidade</th><th>Base legal (LGPD, art. 7º)</th></tr></thead>
            <tbody>
            <tr><td>Criar e manter sua conta; agendar, cobrar e atender</td><td>Execução de contrato (V)</td></tr>
            <tr><td>Enviar e-mails do serviço: código, comprovantes, confirmações, reembolsos, avisos de fila e de chamados</td><td>Execução de contrato (V)</td></tr>
            <tr><td>Segurança, prevenção a fraudes, limite de cancelamentos e bloqueio</td><td>Legítimo interesse (IX)</td></tr>
            <tr><td>Guardar registros e dados fiscais e de pagamento pelo prazo exigido em lei</td><td>Obrigação legal (II)</td></tr>
            <tr><td>Defesa em processos judiciais, administrativos ou arbitrais</td><td>Exercício regular de direitos (VI)</td></tr>
            <tr><td>Lembrar suas preferências no navegador (tema)</td><td>Consentimento (I)</td></tr>
            </tbody>
        </table>,
    },
    {
        id: 'compartilhamento', titulo: 'Com quem compartilhamos',
        conteudo: <>
            <p>Não vendemos seus dados. Compartilhamos apenas o necessário, com:</p>
            <ul>
                <li><strong>O profissional que vai atender você:</strong> vê seu nome, seus agendamentos com ele, sua
                    ficha de preferências e as mensagens trocadas com ele.</li>
                <li><strong>Administração da Barbearia:</strong> acessa dados de agendamentos, pagamentos e chamados
                    para operar o serviço e resolver problemas.</li>
                <li><strong>Mercado Pago:</strong> processa o pagamento por Pix e o reembolso.</li>
                <li><strong>Provedor de envio de e-mails:</strong> entrega as mensagens do serviço.</li>
                <li><strong>Provedor de inteligência artificial (Google):</strong> recebe as mensagens que você escreve ao
                    assistente virtual com IA, para gerar as respostas.</li>
                <li><strong>Provedores de hospedagem e infraestrutura</strong> que mantêm o site e o banco de dados.</li>
                <li><strong>Autoridades</strong>, quando houver ordem legal ou para cumprir obrigação regulatória.</li>
            </ul>
        </>,
    },
    {
        id: 'ia', titulo: 'Assistente virtual com inteligência artificial',
        conteudo: <>
            <p>O uso da IA é opcional: o menu guiado funciona sem ela. Quando você escolhe a IA, o texto que você
                digita, junto com informações de serviços, preços e horários, é enviado ao provedor para gerar a
                resposta. A resposta é automática, pode errar e não toma decisões sobre você: ela não agenda, não
                cancela nem reembolsa.</p>
            <p>Guardamos o histórico dessa conversa na sua conta e há um limite diário de mensagens.</p>
        </>,
    },
    {
        id: 'internacional', titulo: 'Transferência internacional',
        conteudo: <p>Alguns provedores (por exemplo, o de IA e o de infraestrutura) podem processar dados em servidores
            fora do Brasil. Nesses casos, a transferência é feita com base na LGPD (art. 33) e com provedores que
            adotam medidas de segurança e proteção de dados.</p>,
    },
    {
        id: 'retencao', titulo: 'Por quanto tempo guardamos',
        conteudo: <ul>
            <li>Dados da conta e do histórico: enquanto a conta existir.</li>
            <li>Dados de pagamento e registros necessários a obrigações legais e fiscais: pelos prazos definidos em
                lei (em regra, 5 anos).</li>
            <li>Registros de acesso à aplicação: pelo prazo mínimo legal de 6 meses (Marco Civil da Internet).</li>
            <li>Após esses prazos, eliminamos ou anonimizamos os dados, salvo se a lei permitir ou exigir a guarda.</li>
        </ul>,
    },
    {
        id: 'seguranca', titulo: 'Como protegemos seus dados',
        conteudo: <p>Usamos medidas técnicas e administrativas adequadas: senhas criptografadas, conexão segura,
            controle de acesso por perfil (cliente, barbeiro e administrador), limites de tentativas e de envios de
            código, e acesso restrito ao banco de dados. Nenhum sistema é totalmente imune a falhas; se houver um
            incidente que possa causar risco ou dano relevante, comunicaremos você e a ANPD conforme a lei.</p>,
    },
    {
        id: 'imagem', titulo: 'Imagem e fotos',
        conteudo: <ul>
            <li>Tratamos a foto de perfil dos barbeiros (que eles mesmos enviam) apenas para identificá-los no site e no painel.</li>
            <li>Não coletamos foto de clientes.</li>
            <li>Não usamos imagens de clientes em propaganda sem consentimento específico.</li>
            <li>A foto fica guardada na conta do profissional. Você pode pedir a remoção ou a troca a qualquer momento.</li>
        </ul>,
    },
    {
        id: 'direitos', titulo: 'Seus direitos',
        conteudo: <>
            <p>Você pode, a qualquer momento (LGPD, art. 18), pedir:</p>
            <ul>
                <li>confirmação de que tratamos seus dados e acesso a eles;</li>
                <li>correção de dados incompletos, inexatos ou desatualizados;</li>
                <li>anonimização, bloqueio ou eliminação de dados desnecessários ou tratados em desconformidade;</li>
                <li>portabilidade dos dados;</li>
                <li>eliminação dos dados tratados com base no seu consentimento;</li>
                <li>informação sobre com quem compartilhamos os dados;</li>
                <li>revogação do consentimento e revisão de decisões tomadas apenas de forma automatizada.</li>
            </ul>
            <p>Para exercer esses direitos, escreva para {EMPRESA.emailPrivacidade} a partir do e-mail cadastrado. Podemos
                pedir uma confirmação de identidade. Responderemos no prazo legal. Alguns dados precisam ser mantidos por
                obrigação legal; nesse caso, explicaremos o motivo. Você também pode reclamar à Autoridade Nacional de
                Proteção de Dados (ANPD).</p>
        </>,
    },
    {
        id: 'automatizadas', titulo: 'Decisões automatizadas',
        conteudo: <p>O bloqueio de novos agendamentos por excesso de cancelamentos e a devolução automática do Pix dentro do
            prazo são decididos automaticamente pelo sistema, com base em regras objetivas (quantidade de cancelamentos e
            antecedência). Você tem direito de pedir a revisão por uma pessoa (LGPD, art. 20) pelo chamado ou por
            {' '}{EMPRESA.emailPrivacidade}.</p>,
    },
    {
        id: 'profissionais', titulo: 'Dados dos profissionais e visibilidade',
        conteudo: <ul>
            <li>Barbeiros e administradores têm acesso aos dados necessários ao trabalho deles, de acordo com o perfil.</li>
            <li>O barbeiro vê o nome, os agendamentos com ele, a ficha de preferências e as mensagens do chat dos clientes
                que têm agendamento com ele.</li>
            <li>A foto e o nome dos barbeiros aparecem para os clientes.</li>
            <li>As mensagens do chat e dos chamados podem ser lidas por quem participa da conversa.</li>
        </ul>,
    },
    {
        id: 'exclusao', titulo: 'Encerramento da conta e eliminação de dados',
        conteudo: <>
            <p>Para encerrar a conta, peça pelo e-mail do encarregado. Eliminamos ou anonimizamos os dados que não precisamos
                guardar. Podem ser mantidos, pelo prazo legal, os dados de agendamentos, pagamentos e reembolsos (obrigações
                fiscais e de prestação de contas) e os necessários à defesa em processos.</p>
            <p>Agendamentos futuros ativos serão cancelados no encerramento, seguindo a política de reembolso.</p>
        </>,
    },
    {
        id: 'criancas', titulo: 'Crianças e adolescentes',
        conteudo: <p>O serviço é voltado a maiores de 18 anos. Menores podem usá-lo apenas com autorização e
            acompanhamento de um responsável legal, que responde pelo cadastro. Se soubermos de dados de menor
            tratados sem essa autorização, vamos eliminá-los.</p>,
    },
    {
        id: 'cookies', titulo: 'Cookies',
        conteudo: <p>Veja o que guardamos no seu navegador e como controlar na <Link to="/cookies">Política de Cookies</Link>.</p>,
    },
    {
        id: 'alteracoes', titulo: 'Alterações desta política',
        conteudo: <p>Podemos atualizar esta política. A data e a versão da última atualização ficam no topo desta
            página, e mudanças relevantes serão avisadas no site.</p>,
    },
];

export default function Privacidade() {
    return <PaginaLegal atual="/privacidade" titulo="Política de Privacidade"
                        resumo="Como coletamos, usamos e protegemos os seus dados pessoais." secoes={secoes} />;
}
