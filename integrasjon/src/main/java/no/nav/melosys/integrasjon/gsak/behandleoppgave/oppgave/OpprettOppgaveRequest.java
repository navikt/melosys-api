package no.nav.melosys.integrasjon.gsak.behandleoppgave.oppgave;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.melosys.integrasjon.gsak.behandleoppgave.oppgave.kodeverk.*;

public class OpprettOppgaveRequest {

    private OpprettOppgave oppgave;
    private OpprettOppgaveFristOgPrioritet fristOgPrioritet;
    private OpprettOppgaveBruker bruker;
    private OpprettOppgaveDokumentOgSak dokumentOgSak;
    private boolean lest;

    private OpprettOppgaveRequest(OpprettOppgave oppgave, OpprettOppgaveFristOgPrioritet fristOgPrioritet,
                                  boolean lest, OpprettOppgaveBruker bruker, OpprettOppgaveDokumentOgSak dokumentOgSak) {
        this.oppgave = oppgave;
        this.fristOgPrioritet = fristOgPrioritet;
        this.lest = lest;
        this.bruker = bruker;
        this.dokumentOgSak = dokumentOgSak;
    }

    public static Builder builder() {
        return new Builder();
    }

    public LocalDate getNormertBehandlingsTidInnen() {
        return dokumentOgSak.getNormertBehandlingsTidInnen();
    }

    public AktorType getAktørType() {
        return bruker.getAktørType();
    }

    public int getOpprettetAvEnhetId() {
        return oppgave.getOpprettetAvEnhetId();
    }

    public String getAnsvarligEnhetId() {
        return oppgave.getAnsvarligEnhetId();
    }

    public String getFnr() {
        return bruker.getFnr();
    }

    public Fagomrade getFagområde() {
        return oppgave.getFagområde();
    }

    public LocalDate getAktivFra() {
        return fristOgPrioritet.getAktivFra();
    }

    public Optional<LocalDate> getAktivTil() {
        return Optional.ofNullable(fristOgPrioritet.getAktivTil());
    }

    public OppgaveType getOppgaveType() {
        return oppgave.getOppgaveType();
    }

    public Underkategori getUnderkategoriKode() {
        return oppgave.getUnderkategori();
    }

    public PrioritetType getPrioritetType() {
        return fristOgPrioritet.getPrioritetType();
    }

    public String getBeskrivelse() {
        return oppgave.getBeskrivelse();
    }

    public boolean isLest() {
        return lest;
    }

    public String getSaksnummer() {
        return dokumentOgSak.getSaksnummer();
    }

    public String getDokumentId() {
        return dokumentOgSak.getDokumentId();
    }

    public LocalDate getMottattDato() {
        return dokumentOgSak.getMottattDato();
    }

    public static class Builder {

        private boolean lest;
        private OpprettOppgaveFristOgPrioritet.Builder fristOgPrioritetBuilder = OpprettOppgaveFristOgPrioritet.builder();
        private OpprettOppgave.Builder oppgaveBuilder = OpprettOppgave.builder();
        private OpprettOppgaveBruker.Builder brukerBuilder = OpprettOppgaveBruker.builder();
        private OpprettOppgaveDokumentOgSak.Builder dokumentOgSakBuilder = OpprettOppgaveDokumentOgSak.builder();

        public Builder medOpprettetAvEnhetId(int opprettetAvEnhetId) {
            this.oppgaveBuilder.opprettetAvEnhet(opprettetAvEnhetId);
            return this;
        }

        public Builder medAnsvarligEnhetId(String ansvarligEnhetId) {
            this.oppgaveBuilder.medAnsvarligEnhet(ansvarligEnhetId);
            return this;
        }

        public Builder medFagområde(Fagomrade fagområde) {
            this.oppgaveBuilder.medFagområde(fagområde);
            return this;
        }

        public Builder medFnr(String fnr) {
            this.brukerBuilder.medFødselsnummer(fnr);
            return this;
        }

        public Builder medAktivFra(LocalDate aktivFra) {
            this.fristOgPrioritetBuilder.aktivFra(aktivFra);
            return this;
        }

        public Builder medAktivTil(LocalDate aktivTil) {
            this.fristOgPrioritetBuilder.aktivTil(aktivTil);
            return this;
        }

        public Builder medOppgaveType(OppgaveType oppgaveType) {
            this.oppgaveBuilder.medOppgaveType(oppgaveType);
            return this;
        }

        public Builder medUnderkategori(Underkategori underkategori) {
            this.oppgaveBuilder.medUnderkategori(underkategori);
            return this;
        }

        public Builder medSaksnummer(String saksnummmer) {
            this.dokumentOgSakBuilder.medSaksnummer(saksnummmer);
            return this;
        }

        public Builder medPrioritetType(PrioritetType prioritetType) {
            this.fristOgPrioritetBuilder.medPrioritetType(prioritetType);
            return this;
        }

        public Builder medBeskrivelse(String beskrivelse) {
            this.oppgaveBuilder.medBeskrivelse(beskrivelse);
            return this;
        }

        public Builder medLest(boolean lest) {
            this.lest = lest;
            return this;
        }

        public Builder medDokumentId(String dokumentId) {
            this.dokumentOgSakBuilder.medDokumentId(dokumentId);
            return this;
        }

        public Builder medMottattDato(LocalDate mottattDato) {
            this.dokumentOgSakBuilder.medMottattDato(mottattDato);
            return this;
        }

        public Builder medNormertBehandlingsTidInnen(LocalDate normertBehandlingsTidInnen) {
            this.dokumentOgSakBuilder.medNormertBehandlingsTidInnen(normertBehandlingsTidInnen);
            return this;
        }

        public Builder medAktørType(AktorType aktørType) {
            this.brukerBuilder.medAktørType(aktørType);
            return this;
        }

        public OpprettOppgaveRequest build() {
            return new OpprettOppgaveRequest(oppgaveBuilder.build(), fristOgPrioritetBuilder.build(), lest, brukerBuilder.build(), dokumentOgSakBuilder.build());
        }
    }
}
