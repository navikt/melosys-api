package no.nav.melosys.integrasjon.pdl.dto;

import java.time.LocalDateTime;

import static no.nav.melosys.integrasjon.pdl.dto.Endringstype.OPPHOER;

public record Endring(Endringstype type, LocalDateTime registrert, String kilde) {
    public boolean erIkkeOpphør() {
        return type != OPPHOER;
    }

    @Override
    public String toString() {
        return "Endring{" +
            "type=" + type +
            ", registrert=" + registrert +
            ", kilde='" + kilde + '\'' +
            '}';
    }
}
