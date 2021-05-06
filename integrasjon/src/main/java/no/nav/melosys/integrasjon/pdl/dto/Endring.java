package no.nav.melosys.integrasjon.pdl.dto;

import java.time.LocalDateTime;

public record Endring(String type, LocalDateTime registrert, String kilde) {
    private static final String ENDRINGSTYPE_OPPHØR = "OPPHOER";

    public boolean erOpphør() {
        return ENDRINGSTYPE_OPPHØR.equals(type);
    }
}
