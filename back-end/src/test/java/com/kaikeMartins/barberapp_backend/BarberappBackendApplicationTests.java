package com.kaikeMartins.barberapp_backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:contexto;DB_CLOSE_DELAY=-1",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
		"spring.jwt.key=chave-secreta-de-teste-com-mais-de-32-bytes-0123456789",
		"spring.mail.username=teste@email.com",
		"spring.mail.password=senha-de-teste",
		"admin.email=admin@email.com",
		"admin.senha=Admin123",
		"admin.nome=Admin",
		"gemini.api_key=chave-de-teste",
		"gemini.url=http://localhost:9999",
		"pix.chave=chave@email.com",
		"pix.recebedor=Barbearia Teste",
		"pix.cidade=Aracaju",
		"barbearia.google.avaliacao-url=http://localhost/avaliacao",
		"mercadopago.access-token=token-de-teste",
		"mercadopago.webhook-url=http://localhost/webhooks/mercadopago",
		"mercadopago.webhook-secret=segredo-de-teste"
})
class BarberappBackendApplicationTests {

	@Test
	void contextLoads() {
	}
}