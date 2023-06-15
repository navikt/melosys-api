package no.nav.melosys.domain.brev;

import java.time.LocalDate;

import no.nav.melosys.domain.kodeverk.begrunnelser.Ikkeyrkesaktivsituasjontype;

public class IkkeYrkesaktivBrevbestilling extends DokgenBrevbestilling {
    private String begrunnelseFritekst;
    private String innledningFritekst;
    private boolean brukerSkalHaKopi;
    private String nyVurderingBakgrunn;
    private String nyVurderingFritekst;
    private String oppholdsLand;
    private LocalDate periodeFom;
    private LocalDate periodeTom;
    private String bestemmelse;
    private Ikkeyrkesaktivsituasjontype ikkeYrkesaktivSituasjontype;
    private String artikkel;

    public IkkeYrkesaktivBrevbestilling() {
        super();
        //Tom constructor på grunn av deserialsering i prosessinstans
    }

    private IkkeYrkesaktivBrevbestilling(IkkeYrkesaktivBrevbestilling.Builder builder) {
        super(builder);
        this.begrunnelseFritekst = builder.begrunnelseFritekst;
        this.innledningFritekst = builder.innledningFritekst;
        this.brukerSkalHaKopi = builder.brukerSkalHaKopi;
        this.nyVurderingBakgrunn = builder.nyVurderingBakgrunn;
        this.oppholdsLand = builder.oppholdsLand;
        this.periodeFom = builder.periodeFom;
        this.periodeTom = builder.periodeTom;
        this.bestemmelse = builder.bestemmelse;
        this.ikkeYrkesaktivSituasjontype = builder.ikkeYrkesaktivSituasjontype;
        this.nyVurderingFritekst = builder.nyVurderingFritekst;
        this.artikkel = builder.artikkel;
    }

    public String getBegrunnelseFritekst() {
        return begrunnelseFritekst;
    }

    public String getInnledningFritekst() {
        return innledningFritekst;
    }

    public String getNyVurderingBakgrunn() {
        return nyVurderingBakgrunn;
    }

    public boolean isBrukerSkalHaKopi() {
        return brukerSkalHaKopi;
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    public String getOppholdsLand() {
        return oppholdsLand;
    }

    public LocalDate getPeriodeFom() {
        return periodeFom;
    }

    public LocalDate getPeriodeTom() {
        return periodeTom;
    }

    public String getBestemmelse() {
        return bestemmelse;
    }

    public Ikkeyrkesaktivsituasjontype getIkkeYrkesaktivSituasjontype() {
        return ikkeYrkesaktivSituasjontype;
    }

    public String getNyVurderingFritekst() {
        return nyVurderingFritekst;
    }

    public String getArtikkel() {
        return artikkel;
    }

    public static final class Builder extends DokgenBrevbestilling.Builder<Builder> {
        private String begrunnelseFritekst;
        private String innledningFritekst;
        private String oppholdsLand;
        private boolean brukerSkalHaKopi;
        private String nyVurderingBakgrunn;
        private String nyVurderingFritekst;
        private LocalDate periodeFom;
        private LocalDate periodeTom;
        private String bestemmelse;
        private Ikkeyrkesaktivsituasjontype ikkeYrkesaktivSituasjontype;
        private String artikkel;

        public Builder() {
        }

        public Builder(IkkeYrkesaktivBrevbestilling ikkeYrkesaktivBrevbestilling) {
            super(ikkeYrkesaktivBrevbestilling);
            this.begrunnelseFritekst = ikkeYrkesaktivBrevbestilling.begrunnelseFritekst;
            this.innledningFritekst = ikkeYrkesaktivBrevbestilling.innledningFritekst;
            this.brukerSkalHaKopi = ikkeYrkesaktivBrevbestilling.brukerSkalHaKopi;
            this.oppholdsLand = ikkeYrkesaktivBrevbestilling.oppholdsLand;
            this.periodeFom = ikkeYrkesaktivBrevbestilling.periodeFom;
            this.periodeTom = ikkeYrkesaktivBrevbestilling.periodeTom;
            this.bestemmelse = ikkeYrkesaktivBrevbestilling.bestemmelse;
            this.ikkeYrkesaktivSituasjontype = ikkeYrkesaktivBrevbestilling.ikkeYrkesaktivSituasjontype;
            this.nyVurderingFritekst = ikkeYrkesaktivBrevbestilling.nyVurderingFritekst;
            this.nyVurderingBakgrunn = ikkeYrkesaktivBrevbestilling.nyVurderingBakgrunn;
            this.artikkel = ikkeYrkesaktivBrevbestilling.artikkel;
        }

        public Builder medBegrunnelseFritekst(String begrunnelseFritekst) {
            this.begrunnelseFritekst = begrunnelseFritekst;
            return this;
        }

        public Builder medNyVurderingBakgrunn(String nyVurderingBakgrunn) {
            this.nyVurderingBakgrunn = nyVurderingBakgrunn;
            return this;
        }

        public Builder medNyVurderingFritekst(String nyVurderingFritekst) {
            this.nyVurderingFritekst = nyVurderingFritekst;
            return this;
        }

        public Builder medPeriodeFom(LocalDate periodeFom) {
            this.periodeFom = periodeFom;
            return this;
        }

        public Builder medPeriodeTom(LocalDate periodeTom) {
            this.periodeTom = periodeTom;
            return this;
        }

        public Builder medBestemmelse(String bestemmelse) {
            this.bestemmelse = bestemmelse;
            return this;
        }

        public Builder medArtikkel(String artikkel) {
            this.artikkel = artikkel;
            return this;
        }

        public Builder medInnledningFritekst(String innledningFritekst) {
            this.innledningFritekst = innledningFritekst;
            return this;
        }

        public Builder medOppholdsLand(String oppholdsLand) {
            this.oppholdsLand = oppholdsLand;
            return this;
        }

        public Builder medBrukerSkalHaKopi(boolean brukerSkalHaKopi) {
            this.brukerSkalHaKopi = brukerSkalHaKopi;
            return this;
        }

        public Builder medIkkeyrkesaktivSituasjontype(Ikkeyrkesaktivsituasjontype ikkeYrkesaktivSituasjontype) {
            this.ikkeYrkesaktivSituasjontype = ikkeYrkesaktivSituasjontype;
            return this;
        }

        @Override
        public IkkeYrkesaktivBrevbestilling build() {
            return new IkkeYrkesaktivBrevbestilling(this);
        }
    }
}
