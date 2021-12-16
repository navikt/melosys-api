package no.nav.melosys.integrasjon.dokgen.dto;

import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.integrasjon.dokgen.dto.storbritannia.attest.AttestStorbritannia;
import no.nav.melosys.integrasjon.dokgen.dto.storbritannia.innvilgelse.InnvilgelseStorbritannia;

public class InnvilgelseOgAttestStorbritannia extends DokgenDto {

    private final boolean skalHaInnvilgelse;
    private final boolean skalHaAttest;
    private final InnvilgelseStorbritannia innvilgelse;
    private final AttestStorbritannia attest;

    public InnvilgelseOgAttestStorbritannia(Builder builder) {
        super(builder.brevbestilling);
        skalHaInnvilgelse = builder.innvilgelse != null;
        skalHaAttest = builder.attest != null;
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
            return new InnvilgelseOgAttestStorbritannia(this);
        }
    }

    public boolean isSkalHaInnvilgelse() {
        return skalHaInnvilgelse;
    }

    public boolean isSkalHaAttest() {
        return skalHaAttest;
    }

    public InnvilgelseStorbritannia getInnvilgelse() {
        return innvilgelse;
    }

    public AttestStorbritannia getAttest() {
        return attest;
    }
}
