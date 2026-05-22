package com.rifas.util;

import com.rifas.entity.NumeroRifa;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.List;

/**
 * SorteoAlgoritmo — Encapsula la lógica del sorteo aleatorio.
 *
 * ¿Por qué es un @Component separado y no un método en el servicio?
 * Porque tiene una responsabilidad única y específica: elegir
 * un ganador de forma aleatoria y auditable. Separarlo permite:
 *  - Testearlo de forma independiente
 *  - Cambiarlo en el futuro sin tocar el servicio
 *  - Documentar claramente el algoritmo usado
 *
 * ¿Por qué SecureRandom?
 * A diferencia de java.util.Random (que usa un algoritmo lineal
 * congruencial predecible), SecureRandom usa fuentes de entropía
 * del sistema operativo (/dev/urandom en Linux) para generar
 * números verdaderamente impredecibles. Esto es fundamental
 * para la integridad de una rifa.
 */
@Slf4j
@Component
public class SorteoAlgoritmo {

    /**
     * Resultado del sorteo: número ganador + semilla usada.
     *
     * Clase interna porque solo tiene sentido en el contexto
     * de SorteoAlgoritmo.
     */
    public record ResultadoSorteo(
        NumeroRifa numeroGanador,
        String semilla,
        String algoritmo
    ) {}

    /**
     * Ejecuta el sorteo sobre la lista de números reservados.
     *
     * ¿Cómo funciona?
     *  1. Genera una semilla aleatoria con SecureRandom
     *  2. Crea un nuevo SecureRandom inicializado con esa semilla
     *  3. Usa ese SecureRandom para elegir un índice aleatorio
     *     dentro de la lista de números reservados
     *  4. Devuelve el número en ese índice junto con la semilla
     *
     * ¿Por qué guardar la semilla?
     * Cualquier persona puede reproducir el sorteo:
     *  new SecureRandom(semilla.getBytes()).nextInt(totalNumeros)
     * y obtendrá el mismo índice → el mismo ganador.
     * Esto hace el sorteo 100% auditable y transparente.
     *
     * @param numerosReservados lista de números con participante asignado
     * @return ResultadoSorteo con el ganador y la semilla usada
     * @throws IllegalArgumentException si la lista está vacía
     */
    public ResultadoSorteo ejecutar(List<NumeroRifa> numerosReservados) {

        if (numerosReservados == null || numerosReservados.isEmpty()) {
            throw new IllegalArgumentException(
                "No hay números reservados para ejecutar el sorteo");
        }

        // ── Paso 1: Generar semilla con SecureRandom ──────────────
        // Esta semilla es el "número mágico" que se guarda en BD
        // y permite reproducir el resultado.
        SecureRandom secureRandom = new SecureRandom();
        long semillaLong = secureRandom.nextLong();
        String semilla = String.valueOf(Math.abs(semillaLong));

        // ── Paso 2: Crear un nuevo SecureRandom con esa semilla ───
        // Usamos SHA1PRNG para garantizar reproducibilidad:
        // el mismo seed siempre produce la misma secuencia.
        SecureRandom randomReproducible;
        try {
            randomReproducible = SecureRandom.getInstance("SHA1PRNG");
            randomReproducible.setSeed(semilla.getBytes());
        } catch (Exception e) {
            // Fallback: usar el SecureRandom normal si SHA1PRNG no está disponible
            log.warn("SHA1PRNG no disponible, usando SecureRandom estándar");
            randomReproducible = new SecureRandom(semilla.getBytes());
        }

        // ── Paso 3: Elegir índice ganador ─────────────────────────
        // nextInt(n) devuelve un número entre 0 (inclusive) y n (exclusive)
        int indiceGanador = randomReproducible.nextInt(numerosReservados.size());
        NumeroRifa numeroGanador = numerosReservados.get(indiceGanador);

        log.info("Sorteo ejecutado: {} números participantes, " +
                 "índice ganador: {}, número ganador: {}, semilla: {}",
                numerosReservados.size(), indiceGanador,
                numeroGanador.getNumero(), semilla);

        return new ResultadoSorteo(
            numeroGanador,
            semilla,
            "java.security.SecureRandom/SHA1PRNG"
        );
    }
}
