package no.nav.melosys.integrasjon.dokgen.dto.innvilgelsestorbritannia;

import no.nav.melosys.domain.brev.InnvilgelseBrevbestilling;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk;
import no.nav.melosys.integrasjon.dokgen.dto.DokgenDto;
import no.nav.melosys.integrasjon.dokgen.dto.felles.Innvilgelse;
import no.nav.melosys.integrasjon.dokgen.dto.felles.Mottaker;

public class InnvilgelseUK extends DokgenDto {

    private final Mottaker mottaker;
    private final Innvilgelse innvilgelse;
    private final Lovvalgbestemmelser_trygdeavtale_uk artikkel;
    private final Soknad soknad;
    private final Familie familie;
    private final boolean virksomhetArbeidsgiverSkalHaKopi;

    @Override
    public Mottaker getMottaker() {
        return mottaker;
    }

    public Innvilgelse getInnvilgelse() {
        return innvilgelse;
    }

    public Lovvalgbestemmelser_trygdeavtale_uk getArtikkel() {
        return artikkel;
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

    public InnvilgelseUK(Builder builder) {
        super(builder.brevbestilling);
        this.mottaker = builder.mottaker;
        this.innvilgelse = Innvilgelse.av(builder.brevbestilling);
        this.artikkel = builder.artikkel;
        this.soknad = builder.soknad;
        this.familie = builder.familie;
        this.virksomhetArbeidsgiverSkalHaKopi = builder.virksomhetArbeidsgiverSkalHaKopi;

    }

    static public class Builder {
        private Mottaker mottaker;
        private Innvilgelse innvilgelse;
        private Lovvalgbestemmelser_trygdeavtale_uk artikkel;
        private Soknad soknad;
        private Familie familie;
        private boolean virksomhetArbeidsgiverSkalHaKopi;
        private final InnvilgelseBrevbestilling brevbestilling;

        public Builder(InnvilgelseBrevbestilling brevbestilling) {
            this.brevbestilling = brevbestilling;
        }

        public Builder mottaker(Mottaker mottaker) {
            this.mottaker = mottaker;
            return this;
        }

        public Builder innvilgelse(Innvilgelse innvilgelse) {
            this.innvilgelse = innvilgelse;
            return this;
        }

        public Builder artikkel(Lovvalgbestemmelser_trygdeavtale_uk artikkel) {
            this.artikkel = artikkel;
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

        public InnvilgelseUK build() {
            return new InnvilgelseUK(this);
        }
    }
}
