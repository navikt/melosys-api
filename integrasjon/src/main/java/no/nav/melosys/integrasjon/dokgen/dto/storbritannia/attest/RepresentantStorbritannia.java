package no.nav.melosys.integrasjon.dokgen.dto.storbritannia.attest;

import java.util.List;

import no.nav.melosys.domain.brev.storbritannia.Representant;

public record RepresentantStorbritannia(String navn, List<String> adresse) {

    public static RepresentantStorbritannia av(Representant representant) {
        if (representant == null) return null;

        return new RepresentantStorbritannia(representant.navn(), representant.adresse());
    }
}
