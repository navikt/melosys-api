package no.nav.melosys.service.dokument.brev.mapper.arbeidssted;

import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.MaritimtArbeid;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Maritimtyper;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesgrupper;
import no.nav.melosys.service.avklartefakta.AvklartMaritimtArbeid;

public final class MaritimtArbeidssted extends AbstractArbeidssted implements IkkeFysiskArbeidssted {
    private final AvklartMaritimtArbeid avklartMaritimtArbeid;
    private final MaritimtArbeid maritimtArbeid;
    private final String enhetNavn;
    private final String flaggLandKode;

    public MaritimtArbeidssted(MaritimtArbeid maritimtArbeid, AvklartMaritimtArbeid avklartMaritimtArbeid) {
        super(null, null, avklartMaritimtArbeid.getLand());
        this.avklartMaritimtArbeid = avklartMaritimtArbeid;
        this.maritimtArbeid = maritimtArbeid;
        this.enhetNavn = maritimtArbeid.getEnhetNavn();
        this.flaggLandKode = maritimtArbeid.getFlaggLandkode();
    }

    public MaritimtArbeidssted(MaritimtArbeid maritimtArbeid) {
        super(null, null, maritimtArbeid.landkode());
        this.avklartMaritimtArbeid = null;
        this.maritimtArbeid = maritimtArbeid;
        this.enhetNavn = maritimtArbeid.getEnhetNavn();
        this.flaggLandKode = maritimtArbeid.getFlaggLandkode();
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
        // Med avklarte fakta vet saksbehandler om det er sokkel eller skip.
        // Uten avklarte fakta utledes sokkel fra søknadens innretningsopplysninger.
        if (avklartMaritimtArbeid != null) {
            return avklartMaritimtArbeid.getMaritimtype() == Maritimtyper.SOKKEL;
        }

        return maritimtArbeid.getInnretningLandkode() != null
            || maritimtArbeid.getInnretningstype() != null;
    }
}
