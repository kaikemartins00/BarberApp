import { Link } from 'react-router-dom';
import { EMPRESA, identificacao } from '../../legal/dadosLegais.js';
import PaginaLegal from './PaginaLegal.jsx';

const secoes = [
    {
        id: 'aceite', titulo: 'Quem somos e aceite destes termos',
        conteudo: <>
            <p>O {EMPRESA.nomeFantasia} é a plataforma de agendamento operada por {identificacao()} ("Barbearia",
                "nós").</p>
            <p>Ao criar uma conta ou usar o site, você declara que leu e concorda com estes Termos de Uso e com a{' '}
                <Link to="/privacidade">Política de Privacidade</Link>. Se não concordar, não utilize o serviço.</p>
        </>,
    },
    {
        id: 'servico', titulo: 'O que o serviço oferece',
        conteudo: <>
            <p>O site permite consultar barbeiros, serviços e preços, agendar horários, pagar por Pix ou no atendimento,
                acompanhar e cancelar agendamentos, entrar em fila de espera, conversar com o barbeiro, abrir chamados de
                suporte e avaliar atendimentos.</p>
            <p>Os serviços de barbearia são prestados presencialmente pelos profissionais da Barbearia. O site é o meio
                de organização da agenda e dos pagamentos.</p>
        </>,
    },
    {
        id: 'conta', titulo: 'Conta e cadastro',
        conteudo: <ul>
            <li>O cadastro exige nome, e-mail e senha, e a confirmação do e-mail por um código enviado a você.</li>
            <li>Você deve informar dados verdadeiros e mantê-los atualizados.</li>
            <li>A senha é pessoal e secreta. Você é responsável pelas ações feitas com a sua conta; avise-nos
                imediatamente se suspeitar de uso indevido.</li>
            <li>Menores de 18 anos devem usar o serviço com o conhecimento e a autorização de um responsável legal.</li>
        </ul>,
    },
    {
        id: 'agendamento', titulo: 'Agendamento e pagamento',
        conteudo: <>
            <p>Os preços exibidos no momento do agendamento são os valores do serviço. O pagamento pode ser feito:</p>
            <ul>
                <li><strong>Por Pix</strong>, processado pelo Mercado Pago. A confirmação do pagamento é automática.
                    Não armazenamos dados de cartão nem de conta bancária.</li>
                <li><strong>No atendimento</strong>, diretamente na Barbearia, pelos meios que ela aceitar.</li>
            </ul>
            <p>Chegue com pontualidade: atrasos podem reduzir o tempo do atendimento ou exigir remarcação, a critério
                do profissional.</p>
        </>,
    },
    {
        id: 'cancelamento', titulo: 'Cancelamento, reembolso e limite mensal',
        conteudo: <>
            <p>O resumo está abaixo; as regras completas estão na{' '}
                <Link to="/reembolso">Política de Cancelamento e Reembolso</Link>, que faz parte destes Termos e deve ser
                aceita ao agendar.</p>
            <ul>
                <li><strong>Cancelamento com 2 horas ou mais de antecedência:</strong> se o horário foi pago por Pix, o
                    valor é devolvido automaticamente, pelo mesmo meio, e você recebe o comprovante por e-mail. O prazo
                    para o valor aparecer na sua conta depende do seu banco.</li>
                <li><strong>Cancelamento com menos de 2 horas de antecedência:</strong> não há reembolso automático,
                    porque o horário já não pode ser reaproveitado por outro cliente.</li>
                <li><strong>Imprevistos graves</strong> (por exemplo, acidente ou emergência de saúde): você pode abrir um
                    chamado, na área "Problemas", pedindo a análise do caso. A decisão sobre o reembolso nesses casos
                    é da administração da Barbearia, tomada caso a caso, e você é avisado da resposta no próprio chamado.</li>
                <li><strong>Cancelamento feito pela Barbearia</strong> (por exemplo, ausência do profissional): o valor pago
                    por Pix é devolvido integralmente.</li>
                <li><strong>Limite:</strong> cada cliente pode cancelar até 3 horários por mês. Ao atingir o 3º cancelamento,
                    a conta fica impedida de fazer novos agendamentos por 7 dias. O objetivo é evitar horários
                    reservados e não usados, que prejudicam outros clientes e os profissionais.</li>
            </ul>
            <p>Estas regras não afastam direitos que a lei garante a você como consumidor.</p>
        </>,
    },
    {
        id: 'regras', titulo: 'Regras de agendamento',
        conteudo: <p>Como agendar, chegar, atrasar, trocar de horário e o que acontece em cada situação estão nas{' '}
            <Link to="/regras-de-agendamento">Regras de Agendamento</Link>, que fazem parte destes Termos.</p>,
    },
    {
        id: 'fila', titulo: 'Fila de espera',
        conteudo: <p>Quando o dia está lotado, você pode entrar na fila de espera. Se um horário for liberado por
            cancelamento, movemos você automaticamente, conforme a ordem da fila e os horários que você aceitou, e
            avisamos por e-mail. A fila não garante vaga.</p>,
    },
    {
        id: 'conduta', titulo: 'Chat, chamados e conduta',
        conteudo: <>
            <p>O chat com o barbeiro fica disponível apenas para quem tem agendamento ativo com aquele profissional.
                Ao usar o chat, os chamados e as avaliações, você se compromete a:</p>
            <ul>
                <li>tratar as pessoas com respeito, sem ofensas, ameaças, discriminação ou assédio;</li>
                <li>não publicar conteúdo ilegal, falso ou que viole direitos de terceiros;</li>
                <li>não tentar acessar contas, dados ou áreas que não são suas, nem prejudicar o funcionamento do site.</li>
            </ul>
            <p>Podemos remover conteúdo e suspender contas que descumpram estas regras.</p>
        </>,
    },
    {
        id: 'imagem', titulo: 'Imagem, fotos e conteúdo enviado',
        conteudo: <>
            <ul>
                <li><strong>Fotos dos profissionais:</strong> a foto de perfil do barbeiro é usada com a autorização dele para
                    identificá-lo no site. Ele pode trocá-la ou pedir a remoção a qualquer momento.</li>
                <li><strong>Seu conteúdo</strong> (mensagens, chamados e avaliações): continua sendo seu, mas você nos autoriza a
                    armazená-lo e exibi-lo às pessoas envolvidas, apenas para prestar o serviço.</li>
                <li>Você garante que tem direito sobre qualquer imagem ou texto que enviar e que ele não viola direitos de
                    terceiros (imagem, autoria, honra ou privacidade).</li>
                <li>Não usamos fotos, nome ou avaliações de clientes em divulgação ou propaganda sem o seu consentimento
                    expresso e específico.</li>
                <li>Em caso de conteúdo que viole direitos, a pessoa prejudicada pode pedir a remoção pelo e-mail
                    {' '}{EMPRESA.emailContato}, e analisaremos e removeremos quando cabível.</li>
            </ul>
        </>,
    },
    {
        id: 'ia', titulo: 'Assistente virtual e inteligência artificial',
        conteudo: <>
            <p>O assistente do site tem duas partes: um <strong>menu guiado</strong>, que mostra informações reais do
                sistema e permite agendar pelos botões; e, quando disponível, um <strong>assistente virtual com
                    inteligência artificial</strong>.</p>
            <ul>
                <li>As respostas da IA são geradas automaticamente e <strong>podem conter erros</strong>. Confirme
                    informações importantes com o barbeiro ou na tela de agendamento.</li>
                <li>A IA apenas informa (serviços, preços, barbeiros e horários); ela não agenda, não cancela e não
                    reembolsa.</li>
                <li>Há um limite diário de mensagens por cliente.</li>
                <li>As mensagens enviadas à IA são processadas por um provedor externo de IA. Não envie dados
                    sensíveis (senhas, documentos, dados de saúde) nessa conversa.</li>
            </ul>
        </>,
    },
    {
        id: 'bloqueio', titulo: 'Bloqueio automático da conta',
        conteudo: <p>Ao atingir o limite de cancelamentos do mês, o sistema bloqueia novos agendamentos por 7 dias. É uma
            decisão automática, baseada apenas na contagem de cancelamentos. Você pode pedir a revisão por uma pessoa pelo
            chamado ou pelo e-mail {EMPRESA.emailContato}, e a administração pode liberar a conta antes do prazo.</p>,
    },
    {
        id: 'comunicacoes', titulo: 'Comunicações por e-mail',
        conteudo: <p>Enviamos e-mails do serviço: código de verificação, confirmações, comprovantes de pagamento e de
            reembolso, avisos de cancelamento, de fila de espera, de bloqueio e de respostas aos chamados. Eles são necessários
            ao serviço. Não enviamos propaganda sem o seu consentimento. Mantenha seu e-mail atualizado e confira a caixa de spam.</p>,
    },
    {
        id: 'comunicacao', titulo: 'Chat, chamados e assistente',
        conteudo: <p>As regras de uso, moderação e privacidade do chat, dos chamados e do assistente estão na{' '}
            <Link to="/comunicacao">Política de Chat, Chamados e Assistente</Link>, que faz parte destes Termos.</p>,
    },
    {
        id: 'avaliacoes', titulo: 'Avaliações',
        conteudo: <p>Você pode avaliar atendimentos concluídos. A avaliação deve refletir sua experiência real. Podemos
            exibi-la associada ao profissional e remover avaliações que violem a seção de conduta.</p>,
    },
    {
        id: 'responsabilidade', titulo: 'Disponibilidade e responsabilidades',
        conteudo: <>
            <p>Trabalhamos para manter o site disponível, mas ele pode ficar indisponível por manutenção, falhas de
                internet ou de serviços de terceiros (como o provedor de pagamentos ou de e-mail). Nesses casos, você
                pode falar com a Barbearia diretamente ou abrir um chamado.</p>
            <p>Na medida permitida pela lei, não nos responsabilizamos por danos decorrentes de uso indevido da sua
                conta por terceiros por falta de cuidado com a senha, nem por indisponibilidades fora do nosso
                controle. Nada nestes termos limita responsabilidades que a lei não permite limitar.</p>
        </>,
    },
    {
        id: 'propriedade', titulo: 'Propriedade intelectual',
        conteudo: <p>A marca, o layout, os textos e o código do {EMPRESA.nomeFantasia} pertencem à Barbearia ou são
            usados com autorização. Você não pode copiá-los ou explorá-los comercialmente sem permissão.</p>,
    },
    {
        id: 'encerramento', titulo: 'Suspensão e encerramento da conta',
        conteudo: <p>Você pode pedir o encerramento da sua conta a qualquer momento (veja a Política de Privacidade).
            Podemos suspender ou encerrar contas em caso de fraude, descumprimento destes termos ou exigência legal,
            preservando seus direitos de defesa e de reembolso previstos nestes termos.</p>,
    },
    {
        id: 'alteracoes', titulo: 'Alterações destes termos',
        conteudo: <p>Podemos atualizar estes termos. Quando a mudança for relevante, avisaremos no site e, se
            necessário, pediremos um novo aceite. A versão em vigor e a data da última atualização aparecem no
            topo desta página.</p>,
    },
    {
        id: 'foro', titulo: 'Lei aplicável, contato e foro',
        conteudo: <>
            <p>Estes termos seguem as leis brasileiras. Para dúvidas, reclamações ou pedidos, fale conosco em
                {' '}{EMPRESA.emailContato} ou pelo chamado no site. Para questões de dados pessoais, veja a Política
                de Privacidade.</p>
            <p>Fica eleito o foro do domicílio do consumidor para resolver conflitos decorrentes deste contrato, nos
                termos do Código de Defesa do Consumidor.</p>
        </>,
    },
];

export default function Termos() {
    return <PaginaLegal atual="/termos" titulo="Termos de Uso"
                        resumo="As regras para usar o site, agendar, pagar e cancelar." secoes={secoes} />;
}
