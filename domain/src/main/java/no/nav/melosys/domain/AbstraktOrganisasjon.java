package no.nav.melosys.domain;

import java.time.LocalDate;

import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;

// N.B. Subklasser av denne klassen serialiseres i OrganisasjonSerializer
public abstract class AbstraktOrganisasjon {
    protected String orgnummer;
    protected LocalDate oppstartsdato;
    protected String enhetstype; //"http://nav.no/kodeverk/Kodeverk/EnhetstyperJuridiskEnhet"

    public abstract String getNavn();
    public abstract StrukturertAdresse getForretningsadresse();
    public abstract StrukturertAdresse getPostadresse();

    public String getOrgnummer() {
        return orgnummer;
    }

    public LocalDate getOppstartsdato() {
        return oppstartsdato;
    }

    public String getEnhetstype() {
        return enhetstype;
    }
}
