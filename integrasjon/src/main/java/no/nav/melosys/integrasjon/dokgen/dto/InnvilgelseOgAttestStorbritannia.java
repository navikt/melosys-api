package no.nav.melosys.integrasjon.dokgen.dto;

import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.integrasjon.dokgen.dto.storbritannia.attest.AttestStorbritannia;
import no.nav.melosys.integrasjon.dokgen.dto.storbritannia.innvilgelse.InnvilgelseStorbritannia;

public class InnvilgelseOgAttestStorbritannia extends DokgenDto {

    private final InnvilgelseStorbritannia innvilgelse;
    private final AttestStorbritannia attest;
    private final boolean skalHaInfoOmRettigheter;

    public InnvilgelseOgAttestStorbritannia(Builder builder, Aktoersroller mottaker) {
        super(builder.brevbestilling, mottaker);
        this.innvilgelse = builder.innvilgelse;
        this.attest = builder.attest;
        this.skalHaInfoOmRettigheter = builder.skalHaInfoOmRettigheter;
    }

    public static class Builder {
        private InnvilgelseStorbritannia innvilgelse;
        private AttestStorbritannia attest;
        private boolean skalHaInfoOmRettigheter;
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

        public Builder skalHaInfoOmRettigheter(boolean skalHaInfoOmRettigheter) {
            this.skalHaInfoOmRettigheter = skalHaInfoOmRettigheter;
            return this;
        }

        public InnvilgelseOgAttestStorbritannia build() {
            Aktoersroller mottaker = brevbestilling.getUtenlandskMyndighet() == null
                ? Aktoersroller.BRUKER
                : Aktoersroller.TRYGDEMYNDIGHET;
            return new InnvilgelseOgAttestStorbritannia(this, mottaker);
        }
    }

    public boolean isSkalHaInnvilgelse() {
        return innvilgelse != null;
    }

    public boolean isSkalHaAttest() {
        return attest != null;
    }

    public boolean isSkalHaInfoOmRettigheter() {
        return skalHaInfoOmRettigheter;
    }

    public InnvilgelseStorbritannia getInnvilgelse() {
        return innvilgelse;
    }

    public AttestStorbritannia getAttest() {
        return attest;
    }
}
