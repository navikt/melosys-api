package no.nav.melosys.service.persondata;

import java.time.LocalDateTime;
import java.util.List;

import no.nav.melosys.integrasjon.pdl.dto.Endring;
import no.nav.melosys.integrasjon.pdl.dto.Metadata;

public class PdlObjectFactory {
    public static Metadata lagMetadata() {
        return new Metadata("PDL", false,
            List.of(new Endring("OPPRETT", LocalDateTime.parse("2021-05-07T10:04:52"), "Dolly")));
    }
}
