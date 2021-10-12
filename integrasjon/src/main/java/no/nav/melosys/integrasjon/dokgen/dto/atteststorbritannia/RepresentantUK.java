package no.nav.melosys.integrasjon.dokgen.dto.atteststorbritannia;

import java.util.List;

import no.nav.melosys.domain.brev.storbritannia.Representant;

public record RepresentantUK(String navn, List<String> adresse) {
    public static RepresentantUK av(Representant representantUK) {
        return new RepresentantUK(representantUK.navn(), representantUK.adresse());
    }
}
