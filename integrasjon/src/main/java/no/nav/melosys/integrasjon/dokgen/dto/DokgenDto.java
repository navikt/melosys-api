package no.nav.melosys.integrasjon.dokgen.dto;

import java.time.LocalDateTime;
import java.util.List;

public abstract class DokgenDto {
    private final String fnr;
    private final String saksnummer;
    private final LocalDateTime dagensDato;
    private final String navnBruker;
    private final String navnMottaker;
    private final List<String> adresselinjer;
    private final String postnr;
    private final String poststed;

    protected static final int SAKSBEHANDLINGSTID_DAGER = 12 * 7;

    protected DokgenDto(String fnr, String saksnummer, LocalDateTime dagensDato,
                        String navnBruker, String navnMottaker, List<String> adresselinjer,
                        String postnr, String poststed) {
        this.fnr = fnr;
        this.saksnummer = saksnummer;
        this.dagensDato = dagensDato;
        this.navnBruker = navnBruker;
        this.navnMottaker = navnMottaker;
        this.adresselinjer = adresselinjer;
        this.postnr = postnr;
        this.poststed = poststed;
    }

    public String getFnr() {
        return fnr;
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public LocalDateTime getDagensDato() {
        return dagensDato;
    }

    public String getNavnBruker() {
        return navnBruker;
    }

    public String getNavnMottaker() {
        return navnMottaker;
    }

    public List<String> getAdresselinjer() {
        return adresselinjer;
    }

    public String getPostnr() {
        return postnr;
    }

    public String getPoststed() {
        return poststed;
    }
}
