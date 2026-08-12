package com.coffeecommits.brakket;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BrakketApplication {

	/**
	 * Brakket trabaja siempre en hora de Costa Rica: las columnas de fecha son
	 * {@code timestamp} sin zona y guardan reloj de pared, así que la zona por
	 * defecto del JVM es la que decide qué hora se escribe en la base.
	 *
	 * <p>Hasta ahora eso solo lo garantizaba la variable {@code TZ} de
	 * docker-compose. Quien levantara el backend con {@code mvnw spring-boot:run}
	 * —que es lo que dice el README— heredaba la zona del sistema operativo, y
	 * desde otro país escribía sus horas locales en datos compartidos.</p>
	 *
	 * <p>Se fija antes de {@code SpringApplication.run} a propósito: Flyway y los
	 * beans de arranque ya pueden generar timestamps, y un {@code @PostConstruct}
	 * llegaría tarde.</p>
	 */
	private static final String ZONA = "America/Costa_Rica";

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone(ZONA));
		SpringApplication.run(BrakketApplication.class, args);
	}

}
