package ajudantefamiliar.RAG;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	/**
	 * Imagem "slim-faststart": o banco já vem criado dentro da imagem e sobe em segundos.
	 * A imagem "latest" cria o banco a cada execução e costuma passar de 1 minuto (o tempo
	 * padrão de espera do Testcontainers), principalmente no Docker Desktop do Windows.
	 */
	@Bean
	@ServiceConnection
	OracleContainer oracleFreeContainer() {
		return new OracleContainer(DockerImageName.parse("gvenzl/oracle-free:23-slim-faststart"))
				.withStartupTimeout(Duration.ofMinutes(5));
	}

	@Bean
	@ServiceConnection
	RabbitMQContainer rabbitContainer() {
		return new RabbitMQContainer(DockerImageName.parse("rabbitmq:latest"));
	}

}
