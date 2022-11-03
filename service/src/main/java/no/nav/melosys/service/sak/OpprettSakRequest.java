package no.nav.melosys.service.sak;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.Fullmektig;
import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import org.apache.commons.collections4.CollectionUtils;

public class OpprettSakRequest {
    private final String aktørID;
    private final String virksomhetOrgnr;
    private final String utenlandskPersonId;
    private final String arbeidsgiver;
    private final Fullmektig fullmektig;
    private final List<Kontaktopplysning> kontaktopplysninger;
    private final Behandlingstyper behandlingstype;
    private final Behandlingstema behandlingstema;
    private final Behandlingsaarsaktyper behandlingsårsaktype;
    private final LocalDate mottaksdato;
    private final String initierendeJournalpostId;
    private final String initierendeDokumentId;
    private final Sakstyper sakstype;
    private final Sakstemaer sakstema;

    private OpprettSakRequest(OpprettSakRequest.Builder builder) {
        this.aktørID = builder.aktørID;
        this.virksomhetOrgnr = builder.virksomhetOrgnr;
        this.utenlandskPersonId = builder.utenlandskPersonId;
        this.arbeidsgiver = builder.arbeidsgiver;
        this.fullmektig = builder.fullmektig;
        this.kontaktopplysninger = builder.kontaktopplysninger;
        this.behandlingstype = builder.behandlingstype;
        this.behandlingstema = builder.behandlingstema;
        this.behandlingsårsaktype = builder.behandlingsårsaktype;
        this.mottaksdato = builder.mottaksdato;
        this.initierendeJournalpostId = builder.initierendeJournalpostId;
        this.initierendeDokumentId = builder.initierendeDokumentId;
        this.sakstype = builder.sakstype;
        this.sakstema = builder.sakstema;
    }

    public String getAktørID() {
        return aktørID;
    }

    public String getVirksomhetOrgnr() {
        return virksomhetOrgnr;
    }

    public String getUtenlandskPersonId() {
        return utenlandskPersonId;
    }

    public String getArbeidsgiver() {
        return arbeidsgiver;
    }

    public Fullmektig getFullmektig() {
        return fullmektig;
    }

    public List<Kontaktopplysning> getKontaktopplysninger() {
        return kontaktopplysninger;
    }

    public Behandlingstyper getBehandlingstype() {
        return behandlingstype;
    }

    public Behandlingstema getBehandlingstema() {
        return behandlingstema;
    }

    public Behandlingsaarsaktyper getBehandlingsårsaktype() {
        return behandlingsårsaktype;
    }

    public LocalDate getMottaksdato() {
        return mottaksdato;
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

    public Sakstemaer getSakstema() {
        return sakstema;
    }

    public static class Builder {
        private String aktørID;
        private String virksomhetOrgnr;
        private String utenlandskPersonId;
        private String arbeidsgiver;
        private Fullmektig fullmektig;
        private List<Kontaktopplysning> kontaktopplysninger = new ArrayList<>();
        private Behandlingstyper behandlingstype;
        private Behandlingstema behandlingstema;
        private Behandlingsaarsaktyper behandlingsårsaktype;
        private LocalDate mottaksdato;
        private String initierendeJournalpostId;
        private String initierendeDokumentId;
        private Sakstyper sakstype;
        private Sakstemaer sakstema;

        public Builder medAktørID(String aktørID) {
            this.aktørID = aktørID;
            return this;
        }

        public Builder medVirksomhetOrgnr(String virksomhetOrgnr) {
            this.virksomhetOrgnr = virksomhetOrgnr;
            return this;
        }

        public Builder medUtenlandskPersonId(String utenlandskPersonId) {
            this.utenlandskPersonId = utenlandskPersonId;
            return this;
        }

        public Builder medArbeidsgiver(String arbeidsgiver) {
            this.arbeidsgiver = arbeidsgiver;
            return this;
        }

        public Builder medFullmektig(Fullmektig fullmektig) {
            this.fullmektig = fullmektig;
            return this;
        }

        public Builder medKontaktopplysninger(List<Kontaktopplysning> kontaktopplysninger) {
            if (CollectionUtils.isNotEmpty(kontaktopplysninger)) {
                this.kontaktopplysninger = kontaktopplysninger;
            }
            return this;
        }

        public Builder medBehandlingstype(Behandlingstyper behandlingstype) {
            this.behandlingstype = behandlingstype;
            return this;
        }

        public Builder medBehandlingstema(Behandlingstema behandlingstema) {
            this.behandlingstema = behandlingstema;
            return this;
        }

        public Builder medBehandlingsårsaktype(Behandlingsaarsaktyper behandlingsårsaktype) {
            this.behandlingsårsaktype = behandlingsårsaktype;
            return this;
        }

        public Builder medMottaksdato(LocalDate mottaksdato) {
            this.mottaksdato = mottaksdato;
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

        public Builder medSakstema(Sakstemaer sakstema) {
            this.sakstema = sakstema;
            return this;
        }

        public OpprettSakRequest build() {
            return new OpprettSakRequest(this);
        }
    }
}
