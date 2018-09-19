package no.nav.melosys.domain.dokument.person;

import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.dokument.felles.AbstraktKodeverkHjelper;

public class KjoennsType extends AbstraktKodeverkHjelper {
    @Override
    public FellesKodeverk hentKodeverkNavn() {
        return FellesKodeverk.KJØNNSTYPER;
    }
}
