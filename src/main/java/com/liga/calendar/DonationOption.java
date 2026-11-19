package com.liga.calendar;

/**
 * One donation choice on the "Donaciones" section: a label shown on its
 * button (e.g. {@code "U$S 10"} or {@code "Otro monto"}) and the paypal.me
 * URL it opens. See {@link DonacionesController}.
 */
public record DonationOption(String label, String url) {
}
