package no.nav.melosys.domain.dokument.felles;

import no.nav.melosys.domain.util.FellesKodeverk;

public interface KodeverkHjelper {

    String getKode();

    FellesKodeverk hentKodeverkNavn();
}
