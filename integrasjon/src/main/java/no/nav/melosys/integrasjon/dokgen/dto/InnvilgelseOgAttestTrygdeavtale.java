package no.nav.melosys.integrasjon.dokgen.dto;

import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.integrasjon.dokgen.dto.felles.SaksinfoBruker;
import no.nav.melosys.integrasjon.dokgen.dto.trygdeavtale.attest.AttestTrygdeavtale;
import no.nav.melosys.integrasjon.dokgen.dto.trygdeavtale.innvilgelse.InnvilgelseTrygdeavtale;

public class InnvilgelseOgAttestTrygdeavtale extends DokgenDto {

    private final InnvilgelseTrygdeavtale innvilgelse;
    private final AttestTrygdeavtale attest;
    private final boolean skalHaInfoOmRettigheter;
    private final String nyVurderingBakgrunn;

    public InnvilgelseOgAttestTrygdeavtale(Builder builder, Aktoersroller mottaker) {
        super(builder.brevbestilling, mottaker);
        this.innvilgelse = builder.innvilgelse;
        this.attest = builder.attest;
        this.skalHaInfoOmRettigheter = builder.skalHaInfoOmRettigheter;
        this.nyVurderingBakgrunn = builder.nyVurderingBakgrunn;
    }

    public static class Builder {
        private InnvilgelseTrygdeavtale innvilgelse;
        private AttestTrygdeavtale attest;
        private boolean skalHaInfoOmRettigheter;
        private final DokgenBrevbestilling brevbestilling;
        private String nyVurderingBakgrunn;

        public Builder(DokgenBrevbestilling brevbestilling) {
            this.brevbestilling = brevbestilling;
        }

        public Builder innvilgelse(InnvilgelseTrygdeavtale innvilgelse) {
            this.innvilgelse = innvilgelse;
            return this;
        }

        public Builder attest(AttestTrygdeavtale attest) {
            this.attest = attest;
            return this;
        }

        public Builder skalHaInfoOmRettigheter(boolean skalHaInfoOmRettigheter) {
            this.skalHaInfoOmRettigheter = skalHaInfoOmRettigheter;
            return this;
        }

        public Builder nyVurderingBakgrunn(String nyVurderingBakgrunn) {
            this.nyVurderingBakgrunn = nyVurderingBakgrunn;
            return this;
        }

        public InnvilgelseOgAttestTrygdeavtale build() {
            Aktoersroller mottaker = brevbestilling.getUtenlandskMyndighet() == null
                ? Aktoersroller.BRUKER
                : Aktoersroller.TRYGDEMYNDIGHET;
            return new InnvilgelseOgAttestTrygdeavtale(this, mottaker);
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

    public InnvilgelseTrygdeavtale getInnvilgelse() {
        return innvilgelse;
    }

    public AttestTrygdeavtale getAttest() {
        return attest;
    }

    public String getNyVurderingBakgrunn() {
        return nyVurderingBakgrunn;
    }

    @Override
    public SaksinfoBruker getSaksinfo() {
        return (SaksinfoBruker) super.getSaksinfo();
    }
}
