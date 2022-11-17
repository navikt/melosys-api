package no.nav.melosys.integrasjon.dokgen.dto.trygdeavtale.innvilgelse;

import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.integrasjon.dokgen.dto.felles.Innvilgelse;

public class InnvilgelseTrygdeavtale {

    private final Innvilgelse innvilgelse;
    private final LovvalgBestemmelse artikkel;
    private final LovvalgBestemmelse tilleggsbestemmelse;
    private final Soknad soknad;
    private final Familie familie;
    private final boolean virksomhetArbeidsgiverSkalHaKopi;

    public Innvilgelse getInnvilgelse() {
        return innvilgelse;
    }

    public LovvalgBestemmelse getArtikkel() {
        return artikkel;
    }

    public LovvalgBestemmelse getTilleggsbestemmelse() {
        return tilleggsbestemmelse;
    }

    public Soknad getSoknad() {
        return soknad;
    }

    public Familie getFamilie() {
        return familie;
    }

    public boolean isVirksomhetArbeidsgiverSkalHaKopi() {
        return virksomhetArbeidsgiverSkalHaKopi;
    }

    public InnvilgelseTrygdeavtale(Builder builder) {
        this.innvilgelse = builder.innvilgelse;
        this.artikkel = builder.artikkel;
        this.tilleggsbestemmelse = builder.tilleggsbestemmelse;
        this.soknad = builder.soknad;
        this.familie = builder.familie;
        this.virksomhetArbeidsgiverSkalHaKopi = builder.virksomhetArbeidsgiverSkalHaKopi;
    }

    public static class Builder {
        private Innvilgelse innvilgelse;
        private LovvalgBestemmelse artikkel;
        private LovvalgBestemmelse tilleggsbestemmelse;
        private Soknad soknad;
        private Familie familie;
        private boolean virksomhetArbeidsgiverSkalHaKopi;

        public Builder innvilgelse(Innvilgelse innvilgelse) {
            this.innvilgelse = innvilgelse;
            return this;
        }

        public Builder artikkel(LovvalgBestemmelse artikkel) {
            this.artikkel = artikkel;
            return this;
        }

        public Builder tilleggsbestemmelse(LovvalgBestemmelse tilleggsbestemmelse) {
            this.tilleggsbestemmelse = tilleggsbestemmelse;
            return this;
        }

        public Builder soknad(Soknad soknad) {
            this.soknad = soknad;
            return this;
        }

        public Builder familie(Familie familie) {
            this.familie = familie;
            return this;
        }

        public Builder virksomhetArbeidsgiverSkalHaKopi(boolean virksomhetArbeidsgiverSkalHaKopi) {
            this.virksomhetArbeidsgiverSkalHaKopi = virksomhetArbeidsgiverSkalHaKopi;
            return this;
        }

        public InnvilgelseTrygdeavtale build() {
            return new InnvilgelseTrygdeavtale(this);
        }
    }
}
