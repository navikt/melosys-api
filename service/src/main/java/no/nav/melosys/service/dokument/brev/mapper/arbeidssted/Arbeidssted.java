package no.nav.melosys.service.dokument.brev.mapper.arbeidssted;

import no.nav.melosys.domain.kodeverk.yrker.Yrkesgrupper;

public interface Arbeidssted {

    default boolean erFysisk() {
        return false;
    }

    String getForetakNavn();

    String getLandkode();

    String getIdnummer();

    default Yrkesgrupper getYrkesgruppe() {
        return Yrkesgrupper.ORDINAER;
    }

    String lagAdresselinje();
}
