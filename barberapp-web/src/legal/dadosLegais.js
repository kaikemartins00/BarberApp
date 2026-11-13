// Dados de quem opera o site (aparecem nos Termos e nas Políticas).
// Pessoa física ou empresa: preencha o que se aplica. Campos vazios ('') não aparecem nos textos.
// Se o sistema for vendido a uma barbearia, ela troca estes dados pelos dela.
export const EMPRESA = {
    nomeFantasia: 'BarberApp',
    // nome de quem opera o serviço (pessoa física ou razão social)
    razaoSocial: 'Kaique [SOBRENOME]',
    // 'CPF' ou 'CNPJ' e o número. Deixe em branco para não exibir.
    tipoDocumento: '',
    documento: '',
    // cidade/UF ou endereço completo. Deixe em branco para não exibir.
    endereco: '',
    emailContato: '[SEU E-MAIL DE CONTATO]',
    emailPrivacidade: '[SEU E-MAIL DE CONTATO]',
};

// Texto de identificação usado nos documentos: "Kaique X, CPF 000..., com endereço em ..."
export const identificacao = () => [
    EMPRESA.razaoSocial,
    EMPRESA.documento ? `${EMPRESA.tipoDocumento || 'documento'} ${EMPRESA.documento}` : '',
    EMPRESA.endereco ? `localizado em ${EMPRESA.endereco}` : '',
].filter(Boolean).join(', ');

// Mude a versão sempre que os textos mudarem: o banner de cookies volta a perguntar
// e o cadastro passa a gravar a nova versão aceita.
export const VERSAO_TERMOS = '2026-11-13';
export const DATA_ATUALIZACAO = '13 de novembro de 2026';
