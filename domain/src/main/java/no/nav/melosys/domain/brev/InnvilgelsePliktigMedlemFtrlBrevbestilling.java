package no.nav.melosys.domain.brev;

import java.util.List;

import no.nav.melosys.domain.dokument.felles.Periode;

public class InnvilgelsePliktigMedlemFtrlBrevbestilling extends DokgenBrevbestilling {

    private boolean flereLandUkjentHvilke;
    private List<String> land;
    private String bestemmelse;
    private String innvilgelseNyVurderingBakgrunn;
    private String innledningFritekst;
    private String begrunnelseFritekst;
    private String ikkeYrkesaktivOppholdType;
    private String ikkeYrkesaktivRelasjonType;
    private Periode medlemskapsperiode;
    private String trygdeavgiftFritekst;
    private boolean harLavSatsPgaAlder;
    private String arbeidssituasjontype;

    public InnvilgelsePliktigMedlemFtrlBrevbestilling() {
        super();
        //Tom constructor på grunn av deserialsering i prosessinstans
    }

    private InnvilgelsePliktigMedlemFtrlBrevbestilling(Builder builder) {
        super(builder);
        this.flereLandUkjentHvilke = builder.flereLandUkjentHvilke;
        this.bestemmelse = builder.bestemmelse;
        this.innvilgelseNyVurderingBakgrunn = builder.nyVurderingBakgrunn;
        this.ikkeYrkesaktivOppholdType = builder.ikkeYrkesaktivOppholdType;
        this.innledningFritekst = builder.innledningFritekst;
        this.begrunnelseFritekst = builder.begrunnelseFritekst;
        this.ikkeYrkesaktivRelasjonType = builder.ikkeYrkesaktivRelasjonType;
        this.medlemskapsperiode = builder.medlemskapsperiode;
        this.land = builder.land;
        this.trygdeavgiftFritekst = builder.trygdeavgiftFritekst;
        this.harLavSatsPgaAlder = builder.harLavSatsPgaAlder;
        this.arbeidssituasjontype = builder.arbeidssituasjontype;
    }

    public void setArbeidssituasjontype(String arbeidssituasjontype) {
        this.arbeidssituasjontype = arbeidssituasjontype;
    }

    public boolean getHarLavSatsPgaAlder() {
        return harLavSatsPgaAlder;
    }

    public void setHarLavSatsPgaAlder(boolean harLavSatsPgaAlder) {
        this.harLavSatsPgaAlder = harLavSatsPgaAlder;
    }

    public String getArbeidssituasjontype() {
        return arbeidssituasjontype;
    }

    public boolean harLavSatsPgaAlder() {
        return harLavSatsPgaAlder;
    }

    public void harLavSatsPgaAlder(boolean harLavSatsPgaAlder) {
        this.harLavSatsPgaAlder = harLavSatsPgaAlder;
    }

    public String getTrygdeavgiftFritekst() {
        return trygdeavgiftFritekst;
    }

    public void setTrygdeavgiftFritekst(String trygdeavgiftFritekst) {
        this.trygdeavgiftFritekst = trygdeavgiftFritekst;
    }

    public boolean getFlereLandUkjentHvilke() {
        return flereLandUkjentHvilke;
    }

    public String getBestemmelse() {
        return bestemmelse;
    }

    public String getInnvilgelseNyVurderingBakgrunn() {
        return innvilgelseNyVurderingBakgrunn;
    }

    public String getInnledningFritekst() {
        return innledningFritekst;
    }

    public String getBegrunnelseFritekst() {
        return begrunnelseFritekst;
    }

    public String getIkkeYrkesaktivOppholdType() {
        return ikkeYrkesaktivOppholdType;
    }

    public String getIkkeYrkesaktivRelasjonType() {
        return ikkeYrkesaktivRelasjonType;
    }

    public Periode getMedlemskapsperiode() {
        return medlemskapsperiode;
    }

    public List<String> getLand() {
        return land;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static final class Builder extends DokgenBrevbestilling.Builder<Builder> {
        private boolean flereLandUkjentHvilke;
        private List<String> land;
        private String bestemmelse;
        private String nyVurderingBakgrunn;
        private String innledningFritekst;
        private String begrunnelseFritekst;
        private String ikkeYrkesaktivOppholdType;
        private String ikkeYrkesaktivRelasjonType;
        private Periode medlemskapsperiode;
        private boolean harLavSatsPgaAlder;
        private String trygdeavgiftFritekst;
        private String arbeidssituasjontype;

        public Builder() {
        }

        public Builder(InnvilgelsePliktigMedlemFtrlBrevbestilling brevbestilling) {
            super(brevbestilling);
            this.flereLandUkjentHvilke = brevbestilling.flereLandUkjentHvilke;
            this.bestemmelse = brevbestilling.bestemmelse;
            this.nyVurderingBakgrunn = brevbestilling.innvilgelseNyVurderingBakgrunn;
            this.innledningFritekst = brevbestilling.innledningFritekst;
            this.begrunnelseFritekst = brevbestilling.begrunnelseFritekst;
            this.ikkeYrkesaktivOppholdType = brevbestilling.ikkeYrkesaktivOppholdType;
            this.ikkeYrkesaktivRelasjonType = brevbestilling.ikkeYrkesaktivRelasjonType;
            this.medlemskapsperiode = brevbestilling.medlemskapsperiode;
            this.land = brevbestilling.land;
            this.harLavSatsPgaAlder = brevbestilling.harLavSatsPgaAlder();
            this.trygdeavgiftFritekst = brevbestilling.trygdeavgiftFritekst;
            this.arbeidssituasjontype = brevbestilling.arbeidssituasjontype;
        }

        public Builder medNyVurderingBakgrunn(String nyVurderingBakgrunn) {
            this.nyVurderingBakgrunn = nyVurderingBakgrunn;
            return this;
        }

        public Builder medInnledningFritekst(String innledningFritekst) {
            this.innledningFritekst = innledningFritekst;
            return this;
        }

        public Builder medBegrunnelseFritekst(String begrunnelseFritekst) {
            this.begrunnelseFritekst = begrunnelseFritekst;
            return this;
        }

        @Override
        public InnvilgelsePliktigMedlemFtrlBrevbestilling build() {
            return new InnvilgelsePliktigMedlemFtrlBrevbestilling(this);
        }
    }
}
