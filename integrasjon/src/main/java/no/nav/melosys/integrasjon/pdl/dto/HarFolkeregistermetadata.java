package no.nav.melosys.integrasjon.pdl.dto;

import java.time.LocalDateTime;
import java.util.Comparator;

public interface HarFolkeregistermetadata {
    Folkeregistermetadata folkeregistermetadata();

    default LocalDateTime hentGyldighetstidspunkt() {
        return folkeregistermetadata().gyldighetstidspunkt();
    }
}
