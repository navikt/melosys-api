package no.nav.melosys.integrasjon.gsak.behandleoppgave.oppgave;

import javax.xml.bind.Unmarshaller;

import no.nav.melosys.integrasjon.gsak.behandleoppgave.oppgave.kodeverk.Fagomrade;
import no.nav.melosys.integrasjon.gsak.behandleoppgave.oppgave.kodeverk.OppgaveType;
import no.nav.melosys.integrasjon.gsak.behandleoppgave.oppgave.kodeverk.Underkategori;

public class OpprettOppgave {

    private String beskrivelse;
    private int opprettetAvEnhetId;
    private String ansvarligEnhetId;
    private Fagomrade fagområde;
    private OppgaveType oppgaveType;
    private Underkategori underkategori;

    private OpprettOppgave(String beskrivelse, int opprettetAvEnhetId, String ansvarligEnhetId, Fagomrade fagområde, OppgaveType oppgaveType, Underkategori underkategori) {
        this.beskrivelse = beskrivelse;
        this.opprettetAvEnhetId = opprettetAvEnhetId;
        this.ansvarligEnhetId = ansvarligEnhetId;
        this.fagområde = fagområde;
        this.oppgaveType = oppgaveType;
        this.underkategori = underkategori;
    }

    public int getOpprettetAvEnhetId() {
        return opprettetAvEnhetId;
    }

    public String getAnsvarligEnhetId() {
        return ansvarligEnhetId;
    }

    public Fagomrade getFagområde() {
        return fagområde;
    }

    public OppgaveType getOppgaveType() {
        return oppgaveType;
    }

    public Underkategori getUnderkategori() {
        return underkategori;
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String beskrivelse;
        private int opprettetAvEnhetId;
        private String ansvarligEnhetId;
        private Fagomrade fagområde;
        private OppgaveType oppgaveType;
        private Underkategori underkategori;

        public Builder medBeskrivelse(String beskrivelse) {
            this.beskrivelse = beskrivelse;
            return this;
        }

        public Builder opprettetAvEnhet(int opprettetAvEnhetId) {
            this.opprettetAvEnhetId = opprettetAvEnhetId;
            return this;
        }

        public Builder medAnsvarligEnhet(String ansvarligEnhetId) {
            this.ansvarligEnhetId = ansvarligEnhetId;
            return this;
        }

        public Builder medFagområde(Fagomrade fagområde) {
            this.fagområde = fagområde;
            return this;
        }

        public Builder medOppgaveType(OppgaveType oppgaveType) {
            this.oppgaveType = oppgaveType;
            return this;
        }

        public Builder medUnderkategori(Underkategori underkategori) {
            this.underkategori = underkategori;
            return this;
        }

        public OpprettOppgave build() {
            return new OpprettOppgave(beskrivelse, opprettetAvEnhetId, ansvarligEnhetId, fagområde, oppgaveType, underkategori);
        }
    }
}
