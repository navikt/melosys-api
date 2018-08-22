package no.nav.melosys.tjenester.gui.dto;

import java.time.LocalDate;

public class OrganisasjonDto {
    private String orgnr;
    private String navn;
    private LocalDate oppstartdato;
    private String organisasjonsform;
    private AdresseDto forretningsadresse;
    private AdresseDto postadresse;

    public String getOrgnr() {
        return orgnr;
    }

    public void setOrgnr(String orgnr) {
        this.orgnr = orgnr;
    }

    public String getNavn() {
        return navn;
    }

    public void setNavn(String navn) {
        this.navn = navn;
    }

    public LocalDate getOppstartdato() {
        return oppstartdato;
    }

    public void setOppstartdato(LocalDate oppstartdato) {
        this.oppstartdato = oppstartdato;
    }

    public String getOrganisasjonsform() {
        return organisasjonsform;
    }

    public void setOrganisasjonsform(String organisasjonsform) {
        this.organisasjonsform = organisasjonsform;
    }

    public AdresseDto getForretningsadresse() {
        return forretningsadresse;
    }

    public void setForretningsadresse(AdresseDto forretningsadresse) {
        this.forretningsadresse = forretningsadresse;
    }

    public AdresseDto getPostadresse() {
        return postadresse;
    }

    public void setPostadresse(AdresseDto postadresse) {
        this.postadresse = postadresse;
    }
}
