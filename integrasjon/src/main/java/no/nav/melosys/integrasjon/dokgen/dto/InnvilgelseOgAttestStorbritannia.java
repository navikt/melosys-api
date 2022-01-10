package no.nav.melosys.integrasjon.dokgen.dto;

import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.integrasjon.dokgen.dto.storbritannia.attest.AttestStorbritannia;
import no.nav.melosys.integrasjon.dokgen.dto.storbritannia.innvilgelse.InnvilgelseStorbritannia;

public class InnvilgelseOgAttestStorbritannia extends DokgenDto {

    private final InnvilgelseStorbritannia innvilgelse;
    private final AttestStorbritannia attest;

    public InnvilgelseOgAttestStorbritannia(Builder builder) {
        super(builder.brevbestilling);
        innvilgelse = builder.innvilgelse;
        attest = builder.attest;
    }

    public InnvilgelseOgAttestStorbritannia(Builder builder, Aktoersroller mottaker) {
        super(builder.brevbestilling, mottaker);
        innvilgelse = builder.innvilgelse;
        attest = builder.attest;
    }

    public static class Builder {
        private InnvilgelseStorbritannia innvilgelse;
        private AttestStorbritannia attest;
        private final DokgenBrevbestilling brevbestilling;

        public Builder(DokgenBrevbestilling brevbestilling) {
            this.brevbestilling = brevbestilling;
        }

        public Builder innvilgelse(InnvilgelseStorbritannia innvilgelse) {
            this.innvilgelse = innvilgelse;
            return this;
        }

        public Builder attest(AttestStorbritannia attest) {
            this.attest = attest;
            return this;
        }

        public InnvilgelseOgAttestStorbritannia build() {
            if (brevbestilling.getUtenlandskMyndighet() == null) {
                return new InnvilgelseOgAttestStorbritannia(this);
            } else {
                return new InnvilgelseOgAttestStorbritannia(this, Aktoersroller.MYNDIGHET);
            }
        }
    }

    public boolean isSkalHaInnvilgelse() {
        return innvilgelse != null;
    }

    public boolean isSkalHaAttest() {
        return attest != null;
    }

    public InnvilgelseStorbritannia getInnvilgelse() {
        return innvilgelse;
    }

    public AttestStorbritannia getAttest() {
        return attest;
    }
}
