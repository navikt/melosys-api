package no.nav.melosys.domain.brev.storbritannia;

import java.time.Instant;

import no.nav.melosys.domain.brev.DokgenBrevbestilling;

public class AttestStorbritanniaBrevbestilling extends DokgenBrevbestilling {

    private Arbeidstaker arbeidstaker;
    private MedfolgendeFamiliemedlemmer medfolgendeFamiliemedlemmer;
    private Representant arbeidsgiverNorge;
    private Utsendelse utsendelse;
    private Representant representantUK;
    private Instant vedtaksdato;

    public AttestStorbritanniaBrevbestilling() {
        super();
        //Tom constructor på grunn av deserialsering i prosessinstans
    }

    private AttestStorbritanniaBrevbestilling(AttestStorbritanniaBrevbestilling.Builder builder) {
        super(builder);
        this.arbeidstaker = builder.arbeidstaker;
    }

    public Arbeidstaker getArbeidstaker() {
        return arbeidstaker;
    }

    public MedfolgendeFamiliemedlemmer getMedfolgendeFamiliemedlemmer() {
        return medfolgendeFamiliemedlemmer;
    }

    public Representant getArbeidsgiverNorge() {
        return arbeidsgiverNorge;
    }

    public Utsendelse getUtsendelse() {
        return utsendelse;
    }

    public Representant getRepresentantUK() {
        return representantUK;
    }

    @Override
    public Instant getVedtaksdato() {
        return vedtaksdato;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static final class Builder extends DokgenBrevbestilling.Builder<Builder> {
        private Arbeidstaker arbeidstaker;
        private MedfolgendeFamiliemedlemmer medfolgendeFamiliemedlemmer;
        private Representant arbeidsgiverNorge;
        private Utsendelse utsendelse;
        private Representant representantUK;
        private Instant vedtaksdato;

        public Builder() {
        }

        public Builder(AttestStorbritanniaBrevbestilling attestStorbritanniaBrevbestilling) {
            super(attestStorbritanniaBrevbestilling);
            this.arbeidstaker = attestStorbritanniaBrevbestilling.arbeidstaker;
            this.medfolgendeFamiliemedlemmer = attestStorbritanniaBrevbestilling.medfolgendeFamiliemedlemmer;
            this.arbeidsgiverNorge = attestStorbritanniaBrevbestilling.arbeidsgiverNorge;
            this.utsendelse = attestStorbritanniaBrevbestilling.utsendelse;
            this.representantUK = attestStorbritanniaBrevbestilling.representantUK;
            this.vedtaksdato = attestStorbritanniaBrevbestilling.vedtaksdato;
        }

        public Builder medArbeidstaker(Arbeidstaker arbeidstaker) {
            this.arbeidstaker = arbeidstaker;
            return this;
        }

        public Builder medMedfolgendeFamiliemedlemmer(MedfolgendeFamiliemedlemmer medfolgendeFamiliemedlemmer) {
            this.medfolgendeFamiliemedlemmer = medfolgendeFamiliemedlemmer;
            return this;
        }

        public Builder medArbeidsgiverNorge(Representant arbeidsgiverNorge) {
            this.arbeidsgiverNorge = arbeidsgiverNorge;
            return this;
        }

        public Builder medUtsendelse(Utsendelse utsendelse) {
            this.utsendelse = utsendelse;
            return this;
        }

        public Builder medRepresentantUK(Representant representantUK) {
            this.representantUK = representantUK;
            return this;
        }

        public Builder medVedtaksdato(Instant vedtaksdato) {
            this.vedtaksdato = vedtaksdato;
            return this;
        }

        public AttestStorbritanniaBrevbestilling build() {
            return new AttestStorbritanniaBrevbestilling(this);
        }
    }
}
