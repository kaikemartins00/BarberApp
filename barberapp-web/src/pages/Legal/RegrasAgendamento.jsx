import { Link } from 'react-router-dom';
import { EMPRESA } from '../../legal/dadosLegais.js';
import PaginaLegal from './PaginaLegal.jsx';

const secoes = [
    {
        id: 'quem', titulo: 'Quem pode agendar',
        conteudo: <ul>
            <li>Clientes com conta criada e e-mail confirmado pelo código enviado.</li>
            <li>Contas bloqueadas por excesso de cancelamentos não podem fazer novos agendamentos até o fim do bloqueio.</li>
            <li>O agendamento é pessoal. Se for para outra pessoa, o titular da conta continua responsável por ele.</li>
        </ul>,
    },
    {
        id: 'como', titulo: 'Como agendar',
        conteudo: <>
            <p>Pela tela de agendamento ou pelo assistente do chat: escolha o barbeiro, o serviço, o dia e o horário, leia e
                aceite a <Link to="/reembolso">Política de Cancelamento e Reembolso</Link>, confirme e escolha a forma de
                pagamento (Pix ou no atendimento).</p>
            <ul>
                <li>Só aparecem horários livres, futuros e de barbeiros que fazem o serviço escolhido.</li>
                <li>Um horário ocupado por outro cliente no meio do processo deixa de estar disponível, e você escolhe outro.</li>
                <li>O preço exibido na confirmação é o preço do seu agendamento.</li>
            </ul>
        </>,
    },
    {
        id: 'status', titulo: 'Situações do agendamento',
        conteudo: <table className="legal__tabela">
            <thead><tr><th>Situação</th><th>O que significa</th></tr></thead>
            <tbody>
            <tr><td>Pendente</td><td>Agendado, aguardando o pagamento ou a confirmação.</td></tr>
            <tr><td>Confirmado</td><td>Horário garantido (Pix pago ou pagamento no atendimento escolhido).</td></tr>
            <tr><td>Concluído</td><td>O atendimento foi realizado e registrado pelo profissional.</td></tr>
            <tr><td>Cancelado</td><td>Cancelado por você ou pela Barbearia. O horário volta a ficar livre.</td></tr>
            <tr><td>Não compareceu</td><td>O horário passou sem atendimento e o profissional registrou a ausência.</td></tr>
            </tbody>
        </table>,
    },
    {
        id: 'pagamento', titulo: 'Pagamento',
        conteudo: <ul>
            <li><strong>Pix:</strong> o sistema gera um QR Code e um código "copia e cola". A confirmação é automática e você
                recebe o comprovante por e-mail. Um Pix não pago não confirma o horário.</li>
            <li><strong>No atendimento:</strong> você paga na Barbearia, depois do serviço. O profissional confirma o recebimento
                no sistema.</li>
            <li>Os preços podem mudar para novos agendamentos, nunca para os já feitos.</li>
        </ul>,
    },
    {
        id: 'chegada', titulo: 'Chegada e atrasos',
        conteudo: <>
            <p>Chegue com pontualidade. Por respeito aos clientes seguintes, atrasos podem fazer com que o serviço seja
                reduzido, adaptado ou, se não houver tempo, não possa ser feito, a critério do profissional.</p>
            <p>Se for atrasar, avise pelo chat com o barbeiro (disponível para quem tem agendamento ativo com ele).
                Atrasos e ausências não alteram a política de reembolso.</p>
        </>,
    },
    {
        id: 'alteracoes', titulo: 'Remarcar ou trocar o horário',
        conteudo: <p>Não há remarcação direta: para trocar, cancele o horário (respeitando a{' '}
            <Link to="/reembolso">política de reembolso</Link> e o limite mensal) e agende outro. Se o dia desejado estiver
            lotado, entre na fila de espera.</p>,
    },
    {
        id: 'fila', titulo: 'Fila de espera',
        conteudo: <ul>
            <li>Quando o dia está lotado, você escolhe os horários que aceita e entra na fila.</li>
            <li>Se um horário for liberado por cancelamento, você é movido automaticamente e avisado por e-mail.</li>
            <li>Você pode reservar um horário em outro momento enquanto aguarda. Ao ser movido, o pagamento acompanha o novo
                agendamento.</li>
            <li>A fila não garante vaga e pode ser deixada a qualquer momento.</li>
        </ul>,
    },
    {
        id: 'barbearia', titulo: 'Mudanças feitas pela Barbearia',
        conteudo: <p>A Barbearia pode alterar a agenda de trabalho dos profissionais. Se isso afetar um horário já agendado,
            você é avisado por e-mail e o agendamento é cancelado com reembolso integral do Pix, sem contar no seu limite
            mensal. Você pode escolher outro horário ou entrar na fila.</p>,
    },
    {
        id: 'comparecimento', titulo: 'Conclusão, não comparecimento e avaliação',
        conteudo: <ul>
            <li>Depois do horário, o profissional registra se o atendimento foi concluído ou se você não compareceu.</li>
            <li>Após o atendimento concluído, você recebe um e-mail convidando a avaliar o profissional.</li>
            <li>Veja os efeitos financeiros na <Link to="/reembolso">Política de Cancelamento e Reembolso</Link>.</li>
        </ul>,
    },
    {
        id: 'cancelamento', titulo: 'Cancelamento e limite mensal',
        conteudo: <p>Você pode cancelar em "Meus agendamentos" até o início do horário. Cada cliente cancela até 3 horários por
            mês; no 3º cancelamento, a conta fica 7 dias sem poder agendar. Regras de reembolso na{' '}
            <Link to="/reembolso">Política de Cancelamento e Reembolso</Link>.</p>,
    },
    {
        id: 'chat', titulo: 'Chat, chamados e assistente',
        conteudo: <ul>
            <li>O chat com o barbeiro só existe para quem tem agendamento ativo com ele.</li>
            <li>Dúvidas e problemas com o sistema, pagamentos ou reembolsos: abra um chamado em{' '}
                <Link to="/problemas">Problemas</Link>.</li>
            <li>O assistente virtual informa serviços, preços e horários; as respostas da IA podem errar. Quem agenda é você,
                pelos botões.</li>
        </ul>,
    },
    {
        id: 'forca-maior', titulo: 'Falhas técnicas e força maior',
        conteudo: <p>Se o site ficar indisponível ou houver evento fora do nosso controle (queda de energia ou de internet,
            falha de terceiros, caso fortuito), faremos o possível para reagendar sem custo ou devolver valores pagos. Fale
            conosco por {EMPRESA.emailContato} ou presencialmente na Barbearia.</p>,
    },
];

export default function RegrasAgendamento() {
    return <PaginaLegal atual="/regras-de-agendamento" titulo="Regras de Agendamento"
                        resumo="Como agendar, pagar, trocar de horário, ser atendido e o que fazer quando algo foge do previsto."
                        secoes={secoes} />;
}
