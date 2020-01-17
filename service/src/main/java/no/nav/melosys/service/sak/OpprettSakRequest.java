package no.nav.melosys.service.sak;

import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;

public class OpprettSakRequest {
    private String aktørID;
    private String arbeidsgiver;
    private String representant;
    private String representantKontaktperson;
    private Representerer representantRepresenterer;
    private Behandlingstyper behandlingstype;
    private String initierendeJournalpostId;
    private String initierendeDokumentId;
    private Sakstyper sakstype;

    private OpprettSakRequest(OpprettSakRequest.Builder builder) {
        this.aktørID = builder.aktørID;
        this.arbeidsgiver = builder.arbeidsgiver;
        this.representant = builder.representant;
        this.representantKontaktperson = builder.representantKontaktperson;
        this.representantRepresenterer = builder.representantRepresenterer;
        this.behandlingstype = builder.behandlingstype;
        this.initierendeJournalpostId = builder.initierendeJournalpostId;
        this.initierendeDokumentId = builder.initierendeDokumentId;
        this.sakstype = builder.sakstype;
    }

    public String getAktørID() {
        return aktørID;
    }

    public String getArbeidsgiver() {
        return arbeidsgiver;
    }

    public String getRepresentant() {
        return representant;
    }

    public String getRepresentantKontaktperson() {
        return representantKontaktperson;
    }

    public Representerer getRepresentantRepresenterer() {
        return representantRepresenterer;
    }

    public Behandlingstyper getBehandlingstype() {
        return behandlingstype;
    }

    public String getInitierendeJournalpostId() {
        return initierendeJournalpostId;
    }

    public String getInitierendeDokumentId() {
        return initierendeDokumentId;
    }

    public Sakstyper getSakstype() {
        return sakstype;
    }

    public static class Builder {
        private String aktørID;
        private String arbeidsgiver;
        private String representant;
        private String representantKontaktperson;
        private Representerer representantRepresenterer;
        private Behandlingstyper behandlingstype;
        private String initierendeJournalpostId;
        private String initierendeDokumentId;
        private Sakstyper sakstype;

        public Builder medAktørID(String aktørID) {
            this.aktørID = aktørID;
            return this;
        }

        public Builder medArbeidsgiver(String arbeidsgiver) {
            this.arbeidsgiver = arbeidsgiver;
            return this;
        }

        public Builder medRepresentant(String representant) {
            this.representant = representant;
            return this;
        }

        public Builder medRepresentantKontaktperson(String representantKontaktperson) {
            this.representantKontaktperson = representantKontaktperson;
            return this;
        }

        public Builder medRepresentantRepresenterer(Representerer representantRepresenterer) {
            this.representantRepresenterer = representantRepresenterer;
            return this;
        }

        public Builder medBehandlingstype(Behandlingstyper behandlingstype) {
            this.behandlingstype = behandlingstype;
            return this;
        }

        public Builder medInitierendeJournalpostId(String initierendeJournalpostId) {
            this.initierendeJournalpostId = initierendeJournalpostId;
            return this;
        }

        public Builder medInitierendeDokumentId(String initierendeDokumentId) {
            this.initierendeDokumentId = initierendeDokumentId;
            return this;
        }

        public Builder medSakstype(Sakstyper sakstype) {
            this.sakstype = sakstype;
            return this;
        }

        public OpprettSakRequest build() {
            return new OpprettSakRequest(this);
        }
    }
}
