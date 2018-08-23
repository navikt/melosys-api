package no.nav.melosys.domain.dokument.felles;

import no.nav.melosys.domain.FellesKodeverk;

public interface KodeverkHjelper {

    String getKode();

    FellesKodeverk hentKodeverkNavn();
}
