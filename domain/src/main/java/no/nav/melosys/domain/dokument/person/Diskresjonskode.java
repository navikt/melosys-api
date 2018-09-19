package no.nav.melosys.domain.dokument.person;

import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.dokument.felles.AbstraktKodeverkHjelper;

public class Diskresjonskode extends AbstraktKodeverkHjelper {
    @Override
    public FellesKodeverk hentKodeverkNavn() {
        return FellesKodeverk.DISKRESJONSKODER;
    }
}
