package no.nav.melosys.domain.brev;

import java.time.Instant;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;

public class DokgenBrevbestilling extends Brevbestilling {
    private final OrganisasjonDokument org;
    private final Kontaktopplysning kontaktopplysning;
    private final Instant forsendelseMottatt;
    private final String avsenderId;
    private final boolean bestillKopi;

    private DokgenBrevbestilling(Produserbaredokumenter produserbartdokument, Behandling behandling,
                                 OrganisasjonDokument org, Kontaktopplysning kontaktopplysning,
                                 Instant forsendelseMottatt, String avsenderNavn, String avsenderId,
                                 boolean bestillKopi) {
        super(produserbartdokument, behandling, avsenderNavn);
        this.org = org;
        this.kontaktopplysning = kontaktopplysning;
        this.forsendelseMottatt = forsendelseMottatt;
        this.avsenderId = avsenderId;
        this.bestillKopi = bestillKopi;
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

    public String getAvsenderId() {
        return avsenderId;
    }

    public boolean bestillKopi() {
        return bestillKopi;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static final class Builder {
        private Produserbaredokumenter produserbartdokument;
        private Behandling behandling;
        private OrganisasjonDokument org;
        private Kontaktopplysning kontaktopplysning;
        private Instant forsendelseMottatt;
        private String avsenderNavn;
        private String avsenderId;
        private boolean bestillKopi;

        public Builder() {

        }

        public Builder(DokgenBrevbestilling brevbestilling) {
            this.produserbartdokument = brevbestilling.produserbartdokument;
            this.behandling = brevbestilling.behandling;
            this.org = brevbestilling.org;
            this.kontaktopplysning = brevbestilling.kontaktopplysning;
            this.forsendelseMottatt = brevbestilling.forsendelseMottatt;
            this.avsenderNavn = brevbestilling.avsenderNavn;
            this.avsenderId = brevbestilling.avsenderId;
            this.bestillKopi = brevbestilling.bestillKopi;
        }

        public Builder medProduserbartdokument(Produserbaredokumenter produserbartdokument) {
            this.produserbartdokument = produserbartdokument;
            return this;
        }

        public Builder medBehandling(Behandling behandling) {
            this.behandling = behandling;
            return this;
        }

        public Builder medOrg(OrganisasjonDokument org) {
            this.org = org;
            return this;
        }

        public Builder medKontaktopplysning(Kontaktopplysning kontaktopplysning) {
            this.kontaktopplysning = kontaktopplysning;
            return this;
        }

        public Builder medForsendelseMottatt(Instant forsendelseMottatt) {
            this.forsendelseMottatt = forsendelseMottatt;
            return this;
        }

        public Builder medAvsenderNavn(String avsenderNavn) {
            this.avsenderNavn = avsenderNavn;
            return this;
        }

        public Builder medAvsenderId(String avsenderId) {
            this.avsenderId = avsenderId;
            return this;
        }

        public Builder medBestillKopi(boolean bestillKopi) {
            this.bestillKopi = bestillKopi;
            return this;
        }

        public DokgenBrevbestilling build() {
            return new DokgenBrevbestilling(produserbartdokument, behandling, org, kontaktopplysning,
                forsendelseMottatt, avsenderNavn, avsenderId, bestillKopi);
        }
    }
}
