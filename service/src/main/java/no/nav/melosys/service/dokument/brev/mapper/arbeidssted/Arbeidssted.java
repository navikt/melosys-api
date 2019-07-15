package no.nav.melosys.service.dokument.brev.mapper.arbeidssted;

import no.nav.melosys.domain.kodeverk.Yrkesgrupper;

public interface Arbeidssted {

    default boolean erFysisk() {
        return false;
    }

    String getNavn();

    String getLandkode();

    String getIdnummer();

    default Yrkesgrupper getYrkesgruppe() {
        return Yrkesgrupper.ORDINAER;
    }
}
