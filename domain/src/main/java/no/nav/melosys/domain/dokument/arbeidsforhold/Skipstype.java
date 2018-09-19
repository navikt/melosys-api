package no.nav.melosys.domain.dokument.arbeidsforhold;

import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.dokument.felles.AbstraktKodeverkHjelper;

public class Skipstype extends AbstraktKodeverkHjelper {
    @Override
    public FellesKodeverk hentKodeverkNavn() {
        return FellesKodeverk.SKIPSTYPER;
    }
}
