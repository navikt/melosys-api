package no.nav.melosys.integrasjon.dokgen.dto.atteststorbritannia;

import java.time.LocalDate;
import java.time.ZoneId;

import com.fasterxml.jackson.annotation.JsonFormat;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.integrasjon.dokgen.dto.DokgenDto;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public class AttestStorbritannia extends DokgenDto {

    private final Arbeidstaker arbeidstaker;
    private final MedfolgendeFamiliemedlemmer medfolgendeFamiliemedlemmer;
    private final ArbeidsgiverNorge arbeidsgiverNorge;
    private final Utsendelse utsendelse;
    private final RepresentantUK representantUK;

    @JsonFormat(shape = STRING)
    private final LocalDate vedtaksdato;

    public Arbeidstaker getArbeidstaker() {
        return arbeidstaker;
    }

    public MedfolgendeFamiliemedlemmer getMedfolgendeFamiliemedlemmer() {
        return medfolgendeFamiliemedlemmer;
    }

    public ArbeidsgiverNorge getArbeidsgiverNorge() {
        return arbeidsgiverNorge;
    }

    public Utsendelse getUtsendelse() {
        return utsendelse;
    }

    public RepresentantUK getRepresentantUK() {
        return representantUK;
    }

    public LocalDate getVedtaksdato() {
        return vedtaksdato;
    }

    public AttestStorbritannia(Builder builder) {
        super(builder.brevbestilling);
        this.vedtaksdato = builder.brevbestilling.getVedtaksdato() != null ? LocalDate.ofInstant(builder.brevbestilling.getVedtaksdato(), ZoneId.systemDefault()): null;
        this.utsendelse = builder.utsendelse;
        this.arbeidstaker = builder.arbeidstaker;
        this.arbeidsgiverNorge = builder.arbeidsgiverNorge;
        this.medfolgendeFamiliemedlemmer = builder.medfolgendeFamiliemedlemmer;
        this.representantUK = builder.representantUK;
    }

    static public class Builder {
        private Arbeidstaker arbeidstaker;
        private MedfolgendeFamiliemedlemmer medfolgendeFamiliemedlemmer;
        private ArbeidsgiverNorge arbeidsgiverNorge;
        private Utsendelse utsendelse;
        private RepresentantUK representantUK;
        private final DokgenBrevbestilling brevbestilling;

        public Builder(DokgenBrevbestilling brevbestilling) {
            this.brevbestilling = brevbestilling;
        }

        public Builder arbeidstaker(Arbeidstaker arbeidstaker) {
            this.arbeidstaker = arbeidstaker;
            return this;
        }

        public Builder medfolgendeFamiliemedlemmer(MedfolgendeFamiliemedlemmer medfolgendeFamiliemedlemmer) {
            this.medfolgendeFamiliemedlemmer = medfolgendeFamiliemedlemmer;
            return this;
        }

        public Builder arbeidsgiverNorge(ArbeidsgiverNorge arbeidsgiverNorge) {
            this.arbeidsgiverNorge = arbeidsgiverNorge;
            return this;
        }

        public Builder utsendelse(Utsendelse utsendelse) {
            this.utsendelse = utsendelse;
            return this;
        }

        public Builder representantUK(RepresentantUK representantUK) {
            this.representantUK = representantUK;
            return this;
        }

        public AttestStorbritannia build() {
            return new AttestStorbritannia(this);
        }
    }
}
