package no.nav.melosys.integrasjon.dokgen.dto.atteststorbritannia;

import java.util.List;

import no.nav.melosys.domain.brev.storbritannia.Representant;

public record ArbeidsgiverNorge(String virksomhetsnavn, List<String> fullstendigAdresse) {

    public static ArbeidsgiverNorge av(Representant arbeidsgiverNorge) {
        if (arbeidsgiverNorge == null) return null;

        return new ArbeidsgiverNorge(arbeidsgiverNorge.navn(), arbeidsgiverNorge.adresse());
    }
}
