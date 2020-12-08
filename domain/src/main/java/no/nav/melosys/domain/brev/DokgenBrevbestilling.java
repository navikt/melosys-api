package no.nav.melosys.domain.brev;

import java.time.Instant;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;

public class DokgenBrevbestilling {
    private final Produserbaredokumenter produserbartdokument;
    private final Behandling behandling;
    private final OrganisasjonDokument org;
    private final Kontaktopplysning kontaktopplysning;
    private Instant forsendelseMottatt;
    private String avsenderNavn;
    private String avsenderId;
    private boolean bestillKopi = false;

    public DokgenBrevbestilling(Produserbaredokumenter produserbartdokument, Behandling behandling,
                                OrganisasjonDokument org, Kontaktopplysning kontaktopplysning) {
        this.produserbartdokument = produserbartdokument;
        this.behandling = behandling;
        this.org = org;
        this.kontaktopplysning = kontaktopplysning;
    }

    public void setForsendelseMottatt(Instant forsendelseMottatt) {
        this.forsendelseMottatt = forsendelseMottatt;
    }

    public void setAvsenderNavn(String avsenderNavn) {
        this.avsenderNavn = avsenderNavn;
    }

    public void setAvsenderId(String avsenderId) {
        this.avsenderId = avsenderId;
    }

    public void setBestillKopi(boolean bestillKopi) {
        this.bestillKopi = bestillKopi;
    }

    public Produserbaredokumenter getProduserbartdokument() {
        return produserbartdokument;
    }

    public Behandling getBehandling() {
        return behandling;
    }

    public OrganisasjonDokument getOrg() {
        return org;
    }

    public Kontaktopplysning getKontaktopplysning() {
        return kontaktopplysning;
    }

    public Instant getForsendelseMottatt() {
        return forsendelseMottatt;
    }

    public String getAvsenderNavn() {
        return avsenderNavn;
    }

    public String getAvsenderId() {
        return avsenderId;
    }

    public boolean bestillKopi() {
        return bestillKopi;
    }
}
