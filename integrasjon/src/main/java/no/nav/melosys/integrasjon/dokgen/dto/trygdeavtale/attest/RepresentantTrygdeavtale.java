package no.nav.melosys.integrasjon.dokgen.dto.trygdeavtale.attest;

import java.util.List;

import no.nav.melosys.domain.brev.trygdeavtale.Representant;

public record RepresentantTrygdeavtale(String navn, List<String> adresse) {

    public static RepresentantTrygdeavtale av(Representant representant) {
        if (representant == null) return null;

        return new RepresentantTrygdeavtale(representant.navn(), representant.adresse());
    }
}
