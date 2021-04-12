package no.nav.melosys.domain.brev;

import java.time.Instant;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;

public class DokgenBrevbestilling extends Brevbestilling {
    private OrganisasjonDokument org;
    private Kontaktopplysning kontaktopplysning;
    private String kontaktpersonNavn;
    private Instant forsendelseMottatt;
    private String avsenderId;
    private long behandlingId;
    private boolean bestillKopi;
    private Instant vedtaksdato;
    private PersonDokument persondokument;

    public DokgenBrevbestilling() {
        super();
        //Tom constructor på grunn av deserialsering i prosessinstans
    }

    protected DokgenBrevbestilling(Builder<?> builder) {
        super(builder.produserbartdokument, builder.behandling, builder.avsenderNavn);
        this.org = builder.org;
        this.kontaktopplysning = builder.kontaktopplysning;
        this.kontaktpersonNavn = builder.kontaktpersonNavn;
        this.forsendelseMottatt = builder.forsendelseMottatt;
        this.avsenderId = builder.avsenderId;
        this.behandlingId = builder.behandlingId;
        this.bestillKopi = builder.bestillKopi;
        this.vedtaksdato = builder.vedtaksdato;
        this.persondokument = builder.persondokument;
    }

    public OrganisasjonDokument getOrg() {
        return org;
    }

    public Kontaktopplysning getKontaktopplysning() {
        return kontaktopplysning;
    }

    public String getKontaktpersonNavn() {
        return kontaktpersonNavn;
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

    public boolean bestillKopi() {
        return bestillKopi;
    }

    public Instant getVedtaksdato() {
        return vedtaksdato;
    }

    public PersonDokument getPersondokument() {
        return persondokument;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static class Builder<T extends Builder<T>> {
        private Produserbaredokumenter produserbartdokument;
        private Behandling behandling;
        private OrganisasjonDokument org;
        private Kontaktopplysning kontaktopplysning;
        private String kontaktpersonNavn;
        private Instant forsendelseMottatt;
        private String avsenderNavn;
        private String avsenderId;
        private long behandlingId;
        private boolean bestillKopi;
        private Instant vedtaksdato;
        private PersonDokument persondokument;

        public Builder() {
        }

        public Builder(DokgenBrevbestilling brevbestilling) {
            this.produserbartdokument = brevbestilling.produserbartdokument;
            this.behandling = brevbestilling.behandling;
            this.org = brevbestilling.org;
            this.kontaktopplysning = brevbestilling.kontaktopplysning;
            this.kontaktpersonNavn = brevbestilling.kontaktpersonNavn;
            this.forsendelseMottatt = brevbestilling.forsendelseMottatt;
            this.avsenderNavn = brevbestilling.avsenderNavn;
            this.avsenderId = brevbestilling.avsenderId;
            this.behandlingId = brevbestilling.behandlingId;
            this.bestillKopi = brevbestilling.bestillKopi;
            this.vedtaksdato = brevbestilling.vedtaksdato;
            this.persondokument = brevbestilling.persondokument;
        }

        public T medProduserbartdokument(Produserbaredokumenter produserbartdokument) {
            this.produserbartdokument = produserbartdokument;
            return (T) this;
        }

        public T medBehandling(Behandling behandling) {
            this.behandling = behandling;
            return (T) this;
        }

        public T medOrg(OrganisasjonDokument org) {
            this.org = org;
            return (T) this;
        }

        public T medKontaktopplysning(Kontaktopplysning kontaktopplysning) {
            this.kontaktopplysning = kontaktopplysning;
            return (T) this;
        }

        public T medKontaktpersonNavn(String kontaktpersonNavn) {
            this.kontaktpersonNavn = kontaktpersonNavn;
            return (T) this;
        }

        public T medForsendelseMottatt(Instant forsendelseMottatt) {
            this.forsendelseMottatt = forsendelseMottatt;
            return (T) this;
        }

        public T medAvsenderNavn(String avsenderNavn) {
            this.avsenderNavn = avsenderNavn;
            return (T) this;
        }

        public T medAvsenderId(String avsenderId) {
            this.avsenderId = avsenderId;
            return (T) this;
        }

        public T medBehandlingId(long behandlingId) {
            this.behandlingId = behandlingId;
            return (T) this;
        }

        public T medBestillKopi(boolean bestillKopi) {
            this.bestillKopi = bestillKopi;
            return (T) this;
        }

        public T medVedtaksdato(Instant vedtaksdato) {
            this.vedtaksdato = vedtaksdato;
            return (T) this;
        }

        public T medPersonDokument(PersonDokument personDokument) {
            this.persondokument = personDokument;
            return (T) this;
        }

        public DokgenBrevbestilling build() {
            return new DokgenBrevbestilling(this);
        }
    }
}
