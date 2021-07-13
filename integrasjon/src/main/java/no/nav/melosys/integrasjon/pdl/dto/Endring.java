package no.nav.melosys.integrasjon.pdl.dto;

import java.time.LocalDateTime;

import static no.nav.melosys.integrasjon.pdl.dto.Endringstype.OPPHOER;
import static no.nav.melosys.integrasjon.pdl.dto.Endringstype.OPPRETT;

public record Endring(Endringstype type, LocalDateTime registrert, String kilde) {
    public boolean erOpphør() {
        return type == OPPHOER;
    }

    public boolean erOpprettelse() {
        return type == OPPRETT;
    }
}
