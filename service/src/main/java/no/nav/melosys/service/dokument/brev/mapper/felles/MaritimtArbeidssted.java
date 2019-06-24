package no.nav.melosys.service.dokument.brev.mapper.felles;

import no.nav.melosys.service.avklartefakta.AvklartMaritimtArbeid;
import no.nav.melosys.domain.kodeverk.Maritimtyper;
import no.nav.melosys.domain.kodeverk.Yrkesgrupper;

public class MaritimtArbeidssted extends Arbeidssted {
    public final AvklartMaritimtArbeid avklartMaritimtArbeid;

    public MaritimtArbeidssted(AvklartMaritimtArbeid avklartMaritimtArbeid) {
        super(avklartMaritimtArbeid.getNavn(), null, avklartMaritimtArbeid.getLand());
        this.avklartMaritimtArbeid = avklartMaritimtArbeid;
    }

    @Override
    public boolean erFysisk() {
        return false;
    }

    @Override
    public Yrkesgrupper getYrkesgruppe() {
        return Yrkesgrupper.SOKKEL_ELLER_SKIP;
    }

    @Override
    public String getOmråde() {
        if (avklartMaritimtArbeid.getMaritimtype() == Maritimtyper.SOKKEL) {
            return "offshore, " + landkode;
        } else {
            return landkode;
        }
    }
}
