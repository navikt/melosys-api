package no.nav.melosys.service.sak;

import no.nav.melosys.domain.kodeverk.Behandlingstyper;

public class OpprettSakRequest {
    private String aktørID;
    private String arbeidsgiver;
    private String representant;
    private String representantKontaktperson;
    private Behandlingstyper behandlingstype;
    private String initierendeJournalpostId;
    private String initierendeDokumentId;

    private OpprettSakRequest(String aktørID, String arbeidsgiver, String representant, String representantKontaktperson, Behandlingstyper behandlingstype, String initierendeJournalpostId, String initierendeDokumentId) {
        this.aktørID = aktørID;
        this.arbeidsgiver = arbeidsgiver;
        this.representant = representant;
        this.representantKontaktperson = representantKontaktperson;
        this.behandlingstype = behandlingstype;
        this.initierendeJournalpostId = initierendeJournalpostId;
        this.initierendeDokumentId = initierendeDokumentId;
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

    public Behandlingstyper getBehandlingstype() {
        return behandlingstype;
    }

    public String getInitierendeJournalpostId() {
        return initierendeJournalpostId;
    }

    public String getInitierendeDokumentId() {
        return initierendeDokumentId;
    }

    public static class Builder {
        private String aktørID;
        private String arbeidsgiver;
        private String representant;
        private String representantKontaktperson;
        private Behandlingstyper behandlingstype;
        private String initierendeJournalpostId;
        private String initierendeDokumentId;

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

        public OpprettSakRequest build() {
            return new OpprettSakRequest(aktørID, arbeidsgiver, representant, representantKontaktperson, behandlingstype, initierendeJournalpostId, initierendeDokumentId);
        }
    }
}
