package com.coffeecommits.brakket.twitch.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.coffeecommits.brakket.twitch.model.MensajeChat;

/**
 * Mensajes de chat capturados (RF-38).
 *
 * <p>Las dos consultas son nativas porque JPQL no tiene funciones de ventana ni
 * búsqueda de texto completo, y ambas cosas son justamente lo que evita traer
 * cientos de miles de filas a memoria para después filtrarlas en Java.</p>
 */
public interface MensajeChatRepository extends JpaRepository<MensajeChat, Long> {

    /**
     * Muestra repartida por todo el rango, como máximo {@code tope} mensajes.
     *
     * <p>No son los primeros ni los últimos: se toma uno cada N para que la
     * muestra describa el período entero. Con los últimos, preguntar por una
     * transmisión de seis horas devolvería solo el final.</p>
     */
    @Query(value = """
            select id, transmision_twitch_id, texto, fecha_hora from (
                select m.*,
                       row_number() over (order by m.fecha_hora) as fila,
                       count(*) over () as total
                from mensaje_chat m
                where m.transmision_twitch_id = :transmisionId
                  and m.fecha_hora between :desde and :hasta
            ) t
            where fila % greatest(1, total / :tope) = 0
            order by fecha_hora
            limit :tope
            """, nativeQuery = true)
    List<MensajeChat> muestraRepartida(@Param("transmisionId") Long transmisionId,
                                       @Param("desde") LocalDateTime desde,
                                       @Param("hasta") LocalDateTime hasta,
                                       @Param("tope") int tope);

    /**
     * Mensajes que coinciden con lo que preguntó el administrador.
     *
     * <p>Se usa {@code websearch_to_tsquery} y no {@code to_tsquery} porque
     * acepta texto libre sin romperse: la consulta es una pregunta escrita por
     * una persona, y {@code to_tsquery} lanza error ante cualquier signo suelto.</p>
     */
    @Query(value = """
            select id, transmision_twitch_id, texto, fecha_hora
            from mensaje_chat
            where transmision_twitch_id = :transmisionId
              and fecha_hora between :desde and :hasta
              and to_tsvector('spanish', texto) @@ websearch_to_tsquery('spanish', :consulta)
            order by fecha_hora
            limit :tope
            """, nativeQuery = true)
    List<MensajeChat> buscar(@Param("transmisionId") Long transmisionId,
                             @Param("desde") LocalDateTime desde,
                             @Param("hasta") LocalDateTime hasta,
                             @Param("consulta") String consulta,
                             @Param("tope") int tope);
}
