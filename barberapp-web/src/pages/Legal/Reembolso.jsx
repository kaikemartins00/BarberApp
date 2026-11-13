import { Link } from 'react-router-dom';
import { EMPRESA } from '../../legal/dadosLegais.js';
import PaginaLegal from './PaginaLegal.jsx';

const secoes = [
    {
        id: 'resumo', titulo: 'Resumo',
        conteudo: <>
            <p>Esta política vale para todo agendamento feito no {EMPRESA.nomeFantasia} e faz parte dos{' '}
                <Link to="/termos">Termos de Uso</Link>. Ao agendar (e ao cancelar), você declara que leu e concorda com ela.</p>
            <ul>
                <li><strong>Cancelou com 2 horas ou mais de antecedência:</strong> o Pix pago é devolvido integralmente.</li>
                <li><strong>Cancelou com menos de 2 horas de antecedência:</strong> o valor pago não é devolvido, salvo
                    causa grave analisada pela administração.</li>
                <li><strong>A Barbearia cancelou:</strong> devolução integral do Pix, sempre.</li>
                <li><strong>Pagamento no atendimento:</strong> não há cobrança antecipada e, portanto, nada a devolver.</li>
            </ul>
        </>,
    },
    {
        id: 'definicoes', titulo: 'Definições',
        conteudo: <ul>
            <li><strong>Horário:</strong> a data e a hora do atendimento que você agendou.</li>
            <li><strong>Antecedência:</strong> o tempo entre o momento em que o cancelamento é confirmado no sistema e o
                horário do atendimento. Vale o relógio do servidor, no horário de Brasília.</li>
            <li><strong>Prazo de reembolso:</strong> até 2 horas antes do horário. Quem cancela exatamente no limite
                ainda está dentro do prazo. O sistema mostra a data e a hora exatas antes de você confirmar.</li>
            <li><strong>Reembolso:</strong> devolução do valor pago por Pix, pelo mesmo meio e para a mesma conta.</li>
            <li><strong>Causa grave:</strong> evento sério e imprevisível que impediu você de comparecer ou de cancelar
                a tempo (veja a seção própria).</li>
        </ul>,
    },
    {
        id: 'cenarios', titulo: 'Todas as situações possíveis',
        conteudo: <>
            <table className="legal__tabela">
                <thead><tr><th>Situação</th><th>Reembolso do Pix</th><th>Conta no limite mensal?</th></tr></thead>
                <tbody>
                <tr><td>Você cancela com 2 horas ou mais de antecedência</td><td>Sim, integral e automático</td><td>Sim</td></tr>
                <tr><td>Você cancela com menos de 2 horas</td><td>Não (salvo causa grave aprovada)</td><td>Sim (exceto se aprovada como exceção)</td></tr>
                <tr><td>Você não comparece e não cancela</td><td>Sim, conforme registro do atendimento (veja "Não comparecimento")</td><td>Não</td></tr>
                <tr><td>A Barbearia ou o profissional cancela</td><td>Sim, integral, a qualquer momento</td><td>Não</td></tr>
                <tr><td>Dia de trabalho cancelado pela Barbearia</td><td>Sim, integral</td><td>Não</td></tr>
                <tr><td>Pix gerado, mas não pago</td><td>Não há valor a devolver</td><td>Sim, se você cancelar o agendamento</td></tr>
                <tr><td>Pix pago, mas o agendamento não foi confirmado por erro do sistema</td><td>Sim, integral</td><td>Não</td></tr>
                <tr><td>Cobrança em duplicidade</td><td>Sim, do valor cobrado a mais</td><td>Não</td></tr>
                <tr><td>Pagamento no atendimento (dinheiro, cartão, etc.)</td><td>Não se aplica</td><td>Sim, se você cancelar</td></tr>
                <tr><td>Você foi movido da fila de espera para um horário anterior</td><td>O pagamento acompanha o novo agendamento</td><td>Não</td></tr>
                <tr><td>Cancelamento por causa grave aprovada pela administração</td><td>Sim, integral</td><td>Não</td></tr>
                <tr><td>Conta bloqueada por excesso de cancelamentos</td><td>Agendamentos já feitos continuam valendo</td><td>-</td></tr>
                </tbody>
            </table>
        </>,
    },
    {
        id: 'cancelamento-cliente', titulo: 'Quando você cancela',
        conteudo: <>
            <p>Você cancela em "Meus agendamentos". Antes de confirmar, o sistema mostra se o cancelamento ainda está
                dentro do prazo, quantos cancelamentos você já usou no mês e pede o seu aceite desta política.</p>
            <ul>
                <li><strong>Dentro do prazo:</strong> o reembolso é solicitado automaticamente no momento do cancelamento.
                    O horário volta a ficar livre para outros clientes e para a fila de espera.</li>
                <li><strong>Fora do prazo:</strong> o valor pago não é devolvido, porque o horário deixou de ser
                    reaproveitável. O cancelamento continua possível e conta no seu limite mensal.</li>
                <li><strong>Depois do horário:</strong> o agendamento passa a ser encerrado pelo profissional (atendimento concluído ou
                    não comparecimento). Um cancelamento nesse momento está fora do prazo de 2 horas.</li>
                <li><strong>Cancelamentos acima do limite:</strong> ao atingir 3 cancelamentos no mês, não é possível
                    cancelar mais um pela plataforma; fale com a Barbearia.</li>
            </ul>
            <p>Cancelar não gera multa nem taxa. O que não é devolvido é apenas o valor já pago por Pix, nos casos acima.</p>
        </>,
    },
    {
        id: 'pix', titulo: 'Como funciona o reembolso do Pix',
        conteudo: <ul>
            <li>O reembolso é feito pelo Mercado Pago, <strong>integralmente</strong> e para a mesma conta que fez o pagamento.</li>
            <li>Você recebe um comprovante por e-mail. O prazo para o valor aparecer na sua conta depende do seu banco.</li>
            <li>Se o Mercado Pago estiver indisponível no momento, o reembolso fica <strong>pendente</strong> e é tentado de
                novo automaticamente, em intervalos curtos. Você não precisa pedir de novo, mas pode abrir um chamado se
                demorar.</li>
            <li>Não há desconto de taxas, multas ou "custos administrativos" sobre os valores devolvidos nos casos em que o
                reembolso é devido.</li>
            <li>O reembolso é feito apenas para o pagamento original. Não fazemos devolução em dinheiro nem em conta de
                terceiros.</li>
            <li>Valores de Pix pagos por terceiros em seu nome são devolvidos à conta pagadora.</li>
        </ul>,
    },
    {
        id: 'limite', titulo: 'Limite de cancelamentos e bloqueio',
        conteudo: <>
            <p>Cada cliente pode cancelar até <strong>3 horários por mês</strong> (o mês é o do calendário). Ao fazer o 3º
                cancelamento do mês, a conta fica impedida de fazer <strong>novos agendamentos por 7 dias</strong>. Os
                agendamentos que você já tem continuam valendo e você segue podendo conversar com a Barbearia.</p>
            <p>O bloqueio existe para evitar horários reservados e não usados, que prejudicam outros clientes e os
                profissionais. É aplicado automaticamente pelo sistema. Você pode pedir a revisão a uma pessoa pelo chamado
                ou por e-mail. A administração pode liberar a conta antes do prazo, e você é avisado por e-mail.</p>
            <p>Não contam no limite: cancelamentos feitos pela Barbearia, cancelamentos aprovados como exceção e erros do
                sistema.</p>
        </>,
    },
    {
        id: 'barbearia', titulo: 'Quando a Barbearia cancela',
        conteudo: <>
            <p>Se o profissional ficar indisponível (doença, imprevisto, folga ou fechamento) e a Barbearia cancelar o seu
                horário, ou o dia de trabalho inteiro:</p>
            <ul>
                <li>você é avisado por e-mail, com o motivo, assim que o cancelamento é feito;</li>
                <li>o valor pago por Pix é devolvido integralmente, a qualquer momento, sem depender do prazo de 2 horas;</li>
                <li>o cancelamento não conta no seu limite mensal;</li>
                <li>você pode agendar de novo em outro horário ou entrar na fila de espera, sem custo.</li>
            </ul>
        </>,
    },
    {
        id: 'grave', titulo: 'Causas graves (análise excepcional)',
        conteudo: <>
            <p>Se você precisou cancelar fora do prazo, ou faltou, por um motivo grave e imprevisível, como acidente,
                emergência de saúde sua ou de familiar próximo, internação, falecimento na família, desastre natural ou
                impedimento por ordem de autoridade, abra um chamado em <Link to="/problemas">Problemas</Link>, escolha
                "Problema com meu agendamento ou reembolso", selecione o agendamento e conte o que aconteceu.</p>
            <ul>
                <li>A decisão é da administração, analisada caso a caso. Não é automática e não é garantida.</li>
                <li>A Barbearia pode pedir informações ou um comprovante razoável, e você não é obrigado a expor dados de saúde
                    além do necessário.</li>
                <li>Se aprovado: o Pix é devolvido integralmente, o cancelamento deixa de contar no limite mensal, o bloqueio por
                    esse cancelamento é desfeito e você recebe a resposta no chamado e o comprovante por e-mail.</li>
                <li>Se negado: você recebe a explicação no chamado. Você pode responder e pedir nova análise com novas
                    informações.</li>
                <li>Peça a análise o quanto antes. Pedidos feitos muito depois do atendimento podem ser negados por falta de
                    como verificar o ocorrido.</li>
            </ul>
        </>,
    },
    {
        id: 'ausencia', titulo: 'Não comparecimento',
        conteudo: <>
            <p>O profissional só registra o não comparecimento depois do início do horário. Nesse caso, o valor pago por
                Pix é devolvido integralmente e você recebe o comprovante por e-mail. O horário perdido não pode ser
                recuperado.</p>
            <p>Para respeitar os outros clientes, cancele sempre que não puder ir. A administração pode avaliar casos de
                faltas reiteradas e, se necessário, orientar o uso do serviço.</p>
        </>,
    },
    {
        id: 'falhas', titulo: 'Falhas, cobrança em duplicidade e erros',
        conteudo: <>
            <p>Se você pagou por Pix e o agendamento não foi confirmado, se foi cobrado mais de uma vez, se o valor cobrado é
                diferente do exibido, ou se houve qualquer erro do sistema, abra um chamado em{' '}
                <Link to="/problemas">Problemas</Link> informando o ocorrido. Cobranças indevidas por falha nossa são
                devolvidas integralmente, independentemente do prazo de 2 horas, após conferência do pagamento.</p>
            <p>O preço que vale é o exibido no momento do agendamento. Mudanças de preço depois disso não alteram agendamentos
                já feitos.</p>
        </>,
    },
    {
        id: 'presencial', titulo: 'Pagamento no atendimento',
        conteudo: <p>Se você escolher pagar no atendimento, nada é cobrado antecipadamente. O pagamento é feito na Barbearia,
            depois do serviço, e o profissional confirma o recebimento no sistema. Cancelamentos nessa modalidade seguem o limite
            mensal e o bloqueio descritos acima, e não geram reembolso porque não houve pagamento antecipado.</p>,
    },
    {
        id: 'fila', titulo: 'Fila de espera',
        conteudo: <p>Entrar na fila de espera não gera cobrança por si só. Se você reservou um horário enquanto aguarda uma
            vaga melhor e pagou por Pix, vale esta política para essa reserva. Se for movido para um horário anterior, o
            pagamento acompanha o novo agendamento. Se você sair da fila, a reserva segue as regras normais de cancelamento.</p>,
    },
    {
        id: 'contestacao', titulo: 'Contestação no banco ou no Mercado Pago',
        conteudo: <p>Antes de contestar uma cobrança diretamente no seu banco, pedimos que fale conosco por chamado: é mais rápido
            e evita bloqueios. Isso não limita o seu direito de procurar o banco, o Mercado Pago ou os órgãos de defesa do
            consumidor.</p>,
    },
    {
        id: 'direitos', titulo: 'Seus direitos como consumidor',
        conteudo: <p>Esta política não limita os direitos que a lei garante a você, incluindo os previstos no Código de Defesa
            do Consumidor (como o direito de arrependimento do art. 49, quando aplicável). Se você entender que um desses
            direitos se aplica ao seu caso, abra um chamado ou escreva para {EMPRESA.emailContato}. Também pode procurar o
            Procon ou o Juizado Especial da sua cidade.</p>,
    },
    {
        id: 'contato', titulo: 'Como pedir ajuda ou contestar',
        conteudo: <p>Abra um chamado em <Link to="/problemas">Problemas</Link> ou escreva para {EMPRESA.emailContato}. Informe o
            agendamento (serviço, dia e horário) e o que aconteceu. Respondemos pelo próprio chamado e por e-mail.</p>,
    },
];

export default function Reembolso() {
    return <PaginaLegal atual="/reembolso" titulo="Política de Cancelamento e Reembolso"
                        resumo="Prazos, regras e como funciona a devolução do seu Pix, em todas as situações." secoes={secoes} />;
}
