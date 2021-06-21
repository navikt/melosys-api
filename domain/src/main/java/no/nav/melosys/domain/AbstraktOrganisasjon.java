package no.nav.melosys.domain;

import java.time.LocalDate;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.jaxb.LocalDateXmlAdapter;

// N.B. Subklasser av denne klassen serialiseres i OrganisasjonSerializer
@XmlAccessorType(XmlAccessType.FIELD)
public abstract class AbstraktOrganisasjon {
    protected String orgnummer;
    @XmlJavaTypeAdapter(LocalDateXmlAdapter.class)
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
