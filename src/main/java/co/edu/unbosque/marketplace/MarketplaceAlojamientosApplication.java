package co.edu.unbosque.marketplace;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Metadatos del contrato OpenAPI (SCRUM-59): el resto del contrato
// (endpoints, esquemas, codigos de respuesta) se infiere de los
// controllers y sus DTO, no hace falta una clase de configuracion aparte.
// "bearerAuth" es el esquema que referencia @SecurityRequirement en los
// controllers protegidos (UsuarioController): aqui se define una sola vez.
@OpenAPIDefinition(info = @Info(
        title = "Marketplace de alojamientos API",
        version = "v1",
        description = "Contrato REST del backend: autenticacion, perfil de usuario y servicios de alojamiento."
))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
@SpringBootApplication
public class MarketplaceAlojamientosApplication {

	public static void main(String[] args) {
		SpringApplication.run(MarketplaceAlojamientosApplication.class, args);
	}

}
