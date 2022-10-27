package no.nav.melosys.integrasjon.dokgen.dto.trygdeavtale.attest;

import java.time.LocalDate;
import java.time.ZoneId;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public class AttestStorbritannia {

    private final Arbeidstaker arbeidstaker;
    private final MedfolgendeFamiliemedlemmer medfolgendeFamiliemedlemmer;
    private final ArbeidsgiverNorge arbeidsgiverNorge;
    private final Utsendelse utsendelse;
    private final RepresentantStorbritannia representant;

    @JsonSerialize(using = LocalDateSerializer.class)
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

    public RepresentantStorbritannia getRepresentant() {
        return representant;
    }

    public LocalDate getVedtaksdato() {
        return vedtaksdato;
    }

    public AttestStorbritannia(Builder builder) {
        this.vedtaksdato = builder.brevbestilling.getVedtaksdato() != null
            ? LocalDate.ofInstant(builder.brevbestilling.getVedtaksdato(), ZoneId.systemDefault()) : null;
        this.utsendelse = builder.utsendelse;
        this.arbeidstaker = builder.arbeidstaker;
        this.arbeidsgiverNorge = builder.arbeidsgiverNorge;
        this.medfolgendeFamiliemedlemmer = builder.medfolgendeFamiliemedlemmer;
        this.representant = builder.representant;
    }

    static public class Builder {
        private Arbeidstaker arbeidstaker;
        private MedfolgendeFamiliemedlemmer medfolgendeFamiliemedlemmer;
        private ArbeidsgiverNorge arbeidsgiverNorge;
        private Utsendelse utsendelse;
        private RepresentantStorbritannia representant;
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

        public Builder representant(RepresentantStorbritannia representant) {
            this.representant = representant;
            return this;
        }

        public AttestStorbritannia build() {
            return new AttestStorbritannia(this);
        }
    }
}
