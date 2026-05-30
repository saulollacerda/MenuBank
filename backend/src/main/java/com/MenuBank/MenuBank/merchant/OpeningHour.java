package com.MenuBank.MenuBank.merchant;

import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Janela de funcionamento do restaurante para um dia da semana.
 * Persistido como item de uma lista JSON em {@link Merchant#getOpeningHours()}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpeningHour {

    /** Dia da semana (MONDAY..SUNDAY). */
    private DayOfWeek dayOfWeek;

    /** Hora de abertura (24h). Pode ser {@code null} quando {@link #closed} é true. */
    private LocalTime openTime;

    /** Hora de fechamento (24h). Pode ser {@code null} quando {@link #closed} é true. */
    private LocalTime closeTime;

    /** Se true, o restaurante está fechado neste dia (ignora {@link #openTime}/{@link #closeTime}). */
    private boolean closed;
}
