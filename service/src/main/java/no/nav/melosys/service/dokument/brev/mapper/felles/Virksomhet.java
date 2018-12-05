package no.nav.melosys.service.dokument.brev.mapper.felles;

import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.soeknad.ForetakUtland;

public class Virksomhet {
    public Virksomhet(ForetakUtland foretak) {
        this.navn = foretak.navn;
        this.orgnr = foretak.orgnr;
        this.adresse = foretak.adresse;
    }

    public Virksomhet(String navn, String orgnr, StrukturertAdresse adresse) {
        this.navn = navn;
        this.orgnr = orgnr;
        this.adresse = adresse;
    }

    public final String navn;
    public final String orgnr;
    public final StrukturertAdresse adresse;
}