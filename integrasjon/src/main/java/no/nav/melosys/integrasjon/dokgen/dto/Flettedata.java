package no.nav.melosys.integrasjon.dokgen.dto;

import java.time.LocalDateTime;
import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.exception.TekniskException;
import org.apache.commons.lang3.NotImplementedException;

public abstract class Flettedata {
    private final String fodselsnr;
    private final String saksnummer;
    private final LocalDateTime dagensDato;
    private final String navnBruker;
    private final String navnMottaker;
    private final List<String> adresselinjer;
    private final String postnr;
    private final String poststed;

    protected static final int SAKSBEHANDLINGSTID_DAGER = 12 * 7;

    public Flettedata(String fodselsnr, String saksnummer, LocalDateTime dagensDato,
                      String navnBruker, String navnMottaker, List<String> adresselinjer,
                      String postnr, String poststed) {
        this.fodselsnr = fodselsnr;
        this.saksnummer = saksnummer;
        this.dagensDato = dagensDato;
        this.navnBruker = navnBruker;
        this.navnMottaker = navnMottaker;
        this.adresselinjer = adresselinjer;
        this.postnr = postnr;
        this.poststed = poststed;
    }

    // NOTE Kun tilstede for å fungere som en mal for klasser som arver fra denne
    public static Flettedata map(Behandling behandling) throws TekniskException {
        throw new NotImplementedException("Skal implementeres i subklasser");
    }

    public String getFodselsnr() {
        return fodselsnr;
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
