package no.nav.melosys.service.dokument.brev.mapper.arbeidssted;

import no.nav.melosys.domain.behandlingsgrunnlag.soeknad.MaritimtArbeid;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Maritimtyper;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesgrupper;
import no.nav.melosys.service.avklartefakta.AvklartMaritimtArbeid;

public final class MaritimtArbeidssted extends AbstractArbeidssted implements IkkeFysiskArbeidssted {
    private final AvklartMaritimtArbeid avklartMaritimtArbeid;
    private final String enhetNavn;
    private final String flaggLandKode;

    public MaritimtArbeidssted(MaritimtArbeid maritimtArbeid, AvklartMaritimtArbeid avklartMaritimtArbeid) {
        super(maritimtArbeid.foretakNavn, maritimtArbeid.foretakOrgnr, avklartMaritimtArbeid.getLand());
        this.avklartMaritimtArbeid = avklartMaritimtArbeid;
        this.enhetNavn = maritimtArbeid.enhetNavn;
        this.flaggLandKode = maritimtArbeid.flaggLandkode;
    }

    @Override
    public Yrkesgrupper getYrkesgruppe() {
        return Yrkesgrupper.SOKKEL_ELLER_SKIP;
    }

    @Override
    public String getEnhetNavn() {
        return enhetNavn;
    }

    @Override
    public String lagAdresselinje() {
        return Landkoder.valueOf(landkode).getBeskrivelse();
    }

    public String getFlaggLandKode() {
        return flaggLandKode;
    }

    public boolean erSokkel() {
        return avklartMaritimtArbeid.getMaritimtype() == Maritimtyper.SOKKEL;
    }
}
