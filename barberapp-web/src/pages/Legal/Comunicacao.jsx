import { Link } from 'react-router-dom';
import { EMPRESA } from '../../legal/dadosLegais.js';
import PaginaLegal from './PaginaLegal.jsx';

const secoes = [
    {
        id: 'canais', titulo: 'Canais de comunicação',
        conteudo: <ul>
            <li><strong>Chat com o barbeiro:</strong> para combinar detalhes do atendimento. Disponível para quem tem agendamento
                ativo (não cancelado) com aquele profissional.</li>
            <li><strong>Chamados (Problemas):</strong> para dúvidas e problemas com o sistema, pagamentos, reembolsos e pedidos
                de análise. São atendidos pela administração ou pelos barbeiros.</li>
            <li><strong>Assistente virtual:</strong> menu guiado e, quando disponível, IA que informa serviços, preços e horários.</li>
            <li><strong>E-mail:</strong> avisos do serviço (códigos, confirmações, comprovantes, cancelamentos e respostas).</li>
        </ul>,
    },
    {
        id: 'uso', titulo: 'Regras de uso',
        conteudo: <>
            <p>Ao usar o chat, os chamados e as avaliações, você se compromete a:</p>
            <ul>
                <li>tratar as pessoas com respeito, sem ofensas, ameaças, discriminação, assédio ou conteúdo sexual;</li>
                <li>não enviar conteúdo ilegal, falso, enganoso, de spam ou que viole direitos de terceiros;</li>
                <li>não pedir ou oferecer pagamentos por fora do sistema para fugir das políticas de cancelamento;</li>
                <li>não compartilhar senhas, códigos de verificação ou dados bancários pelo chat. Nunca pedimos isso;</li>
                <li>não se passar por outra pessoa nem tentar acessar conversas que não são suas.</li>
            </ul>
            <p>Os barbeiros seguem as mesmas regras e devem usar o chat apenas para assuntos do atendimento.</p>
        </>,
    },
    {
        id: 'quem-ve', titulo: 'Quem vê as suas mensagens',
        conteudo: <ul>
            <li>As mensagens do chat são vistas por você e pelo barbeiro da conversa.</li>
            <li>Os chamados são vistos por você e por quem atende (administração e barbeiros).</li>
            <li>A administração pode acessar mensagens e chamados quando necessário para segurança, resolução de denúncias,
                cumprimento de obrigação legal ou defesa em processos.</li>
            <li>Veja mais na <Link to="/privacidade">Política de Privacidade</Link>.</li>
        </ul>,
    },
    {
        id: 'moderacao', titulo: 'Moderação e denúncias',
        conteudo: <>
            <p>Podemos remover mensagens, restringir o chat e suspender contas que descumprirem estas regras, com aviso quando
                possível. Em caso de ameaça, crime ou risco, podemos guardar as mensagens e comunicá-las às autoridades.</p>
            <p>Se alguém se comportar de forma inadequada, abra um chamado em <Link to="/problemas">Problemas</Link> ou escreva
                para {EMPRESA.emailContato}, informando o que aconteceu.</p>
        </>,
    },
    {
        id: 'chamados', titulo: 'Como funcionam os chamados',
        conteudo: <ul>
            <li>Você escolhe o assunto, descreve o problema e, se for sobre um agendamento cancelado, seleciona o agendamento.</li>
            <li>A ordem de atendimento considera a gravidade do assunto (por exemplo, erro de pagamento), o tempo de espera e a
                quantidade de clientes com o mesmo problema.</li>
            <li>Você recebe e-mail quando o chamado é aberto, assumido por alguém, respondido e finalizado.</li>
            <li>Só a administração aprova reembolsos fora das regras (veja a{' '}
                <Link to="/reembolso">Política de Reembolso</Link>).</li>
            <li>Não garantimos prazo fixo de resposta, mas buscamos responder o quanto antes.</li>
        </ul>,
    },
    {
        id: 'ia', titulo: 'Assistente virtual e IA',
        conteudo: <ul>
            <li>As respostas da IA são automáticas e podem conter erros. Para algo importante, fale com o barbeiro ou abra um chamado.</li>
            <li>A IA não agenda, não cancela e não reembolsa. Esses passos são feitos por você, pelos botões.</li>
            <li>Há um limite diário de mensagens e um limite de tamanho por mensagem.</li>
            <li>As mensagens enviadas à IA são processadas por um provedor externo e ficam no histórico da sua conta. Não envie
                dados sensíveis, como senhas, documentos ou dados de saúde.</li>
            <li>O uso da IA é opcional: o menu guiado funciona sem ela.</li>
        </ul>,
    },
    {
        id: 'retencao', titulo: 'Por quanto tempo guardamos',
        conteudo: <p>As conversas e os chamados ficam guardados enquanto a sua conta existir e pelo tempo necessário para
            resolver disputas e cumprir obrigações legais. Você pode pedir a eliminação pelo e-mail
            {' '}{EMPRESA.emailPrivacidade}; dados que a lei exige manter serão preservados.</p>,
    },
];

export default function Comunicacao() {
    return <PaginaLegal atual="/comunicacao" titulo="Política de Chat, Chamados e Assistente"
                        resumo="Regras de convivência, privacidade e uso dos canais de comunicação." secoes={secoes} />;
}
