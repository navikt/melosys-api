package no.nav.melosys.domain.brev.storbritannia;

import no.nav.melosys.domain.brev.DokgenBrevbestilling;

public class AttestStorbritanniaBrevbestilling extends DokgenBrevbestilling {

    private Arbeidstaker arbeidstaker;

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

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static final class Builder extends DokgenBrevbestilling.Builder<Builder> {
        private Arbeidstaker arbeidstaker;

        public Builder() {
        }

        public Builder(AttestStorbritanniaBrevbestilling attestStorbritanniaBrevbestilling) {
            super(attestStorbritanniaBrevbestilling);
            this.arbeidstaker = attestStorbritanniaBrevbestilling.arbeidstaker;
        }

        public Builder medArbeidstaker(Arbeidstaker arbeidstaker) {
            this.arbeidstaker = arbeidstaker;
            return this;
        }

        public AttestStorbritanniaBrevbestilling build() {
            return new AttestStorbritanniaBrevbestilling(this);
        }
    }
}
