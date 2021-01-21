package no.nav.melosys.domain.brev;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;

public class DokgenBrevbestilling extends Brevbestilling {
    private final OrganisasjonDokument org;
    private final Kontaktopplysning kontaktopplysning;
    private final Instant forsendelseMottatt;
    private final String avsenderId;
    private final long behandlingId;
    private final Aktoer mottaker; //NOTE Flytt opp til Brevbestilling
    private final boolean bestillKopi;
    private final Map<DokgenMetaKey, Object> variableFelter;
    private final Behandlingsresultat behandlingsresultat;

    private DokgenBrevbestilling(Builder builder) {
        super(builder.produserbartdokument, builder.behandling, builder.avsenderNavn);
        this.org = builder.org;
        this.kontaktopplysning = builder.kontaktopplysning;
        this.forsendelseMottatt = builder.forsendelseMottatt;
        this.avsenderId = builder.avsenderId;
        this.behandlingId = builder.behandlingId;
        this.mottaker = builder.mottaker;
        this.bestillKopi = builder.bestillKopi;
        this.behandlingsresultat = builder.behandlingsresultat;
        this.variableFelter = builder.variableFelter;
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

    public long getBehandlingId() {
        return behandlingId;
    }

    public Aktoer getMottaker() {
        return mottaker;
    }

    public boolean bestillKopi() {
        return bestillKopi;
    }

    public Behandlingsresultat getBehandlingsresultat() {
        return behandlingsresultat;
    }

    public <T> T getVariabeltFelt(DokgenMetaKey key, Class<T> type) {
        return (variableFelter != null && variableFelter.get(key) != null) ? type.cast(variableFelter.get(key)) : null;
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
        private long behandlingId;
        private Aktoer mottaker;
        private boolean bestillKopi;
        private Behandlingsresultat behandlingsresultat;
        private Map<DokgenMetaKey, Object> variableFelter = null;

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
            this.behandlingId = brevbestilling.behandlingId;
            this.mottaker = brevbestilling.mottaker;
            this.bestillKopi = brevbestilling.bestillKopi;
            this.behandlingsresultat = brevbestilling.behandlingsresultat;
            this.variableFelter = brevbestilling.variableFelter;
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

        public Builder medBehandlingId(long behandlingId) {
            this.behandlingId = behandlingId;
            return this;
        }

        public Builder medMottaker(Aktoer mottaker) {
            this.mottaker = mottaker;
            return this;
        }

        public Builder medBestillKopi(boolean bestillKopi) {
            this.bestillKopi = bestillKopi;
            return this;
        }

        public Builder medBehandlingsResultat(Behandlingsresultat behandlingsresultat) {
            this.behandlingsresultat = behandlingsresultat;
            return this;
        }

        public Builder medVariableFelter(Map<DokgenMetaKey, Object> variableFelter) {
            this.variableFelter = variableFelter;
            return this;
        }

        public Builder medVariabeltFelt(DokgenMetaKey key, Object object) {
            if (this.variableFelter == null) {
                this.variableFelter = new HashMap<>();
            }
            this.variableFelter.put(key, object);
            return this;
        }

        public DokgenBrevbestilling build() {
            return new DokgenBrevbestilling(this);
        }
    }
}
