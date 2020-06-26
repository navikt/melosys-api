package no.nav.melosys.service.dokument.brev.mapper.arbeidssted;

import no.nav.melosys.domain.dokument.soeknad.LuftfartBase;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesgrupper;

public final class FlyvendeArbeidssted extends AbstractArbeidssted implements IkkeFysiskArbeidssted {

    private final String hjemmebaseNavn;

    public FlyvendeArbeidssted(LuftfartBase luftfartBase) {
        super(luftfartBase.hjemmebaseNavn, "", luftfartBase.hjemmebaseLand);
        this.hjemmebaseNavn = luftfartBase.hjemmebaseNavn;
    }

    @Override
    public String getOmråde() {
        return landkode;
    }

    @Override
    public Yrkesgrupper getYrkesgruppe() {
        return Yrkesgrupper.FLYENDE_PERSONELL;
    }

    @Override
    public String getEnhetNavn() {
        return hjemmebaseNavn;
    }

    @Override
    public String lagAdresselinje() {
        return Landkoder.valueOf(landkode).getBeskrivelse();
    }
}
