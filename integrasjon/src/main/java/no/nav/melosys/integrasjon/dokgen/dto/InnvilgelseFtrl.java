package no.nav.melosys.integrasjon.dokgen.dto;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import no.nav.melosys.domain.brev.InnvilgelseBrevbestilling;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.integrasjon.dokgen.dto.felles.Innvilgelse;
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.*;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public class InnvilgelseFtrl extends DokgenDto {

    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonFormat(shape = STRING)
    private final LocalDate datoMottatt;

    private final Innvilgelse innvilgelse;
    private final List<Periode> perioder;
    private final boolean erFullstendigInnvilget;
    private final String ftrl_2_8_begrunnelse;
    private final boolean vurderingMedlemskapEktefelle;
    private final boolean vurderingLovvalgBarn;
    private final List<FamiliemedlemInfo> omfattetFamilie;
    private final List<IkkeOmfattetBarn> ikkeOmfattetBarn;
    private final IkkeOmfattetEktefelle ikkeOmfattetEktefelle;
    private final String arbeidsgiverNavn;
    private final String arbeidsland;
    private final boolean trygdeavtaleMedArbeidsland;
    private final VurderingTrygdeavgift vurderingTrygdeavgift;
    private final String loennsforhold;
    private final String arbeidsgiverFullmektigNavn;
    private final boolean brukerHarFullmektig;
    private final String avgiftssatsAar;
    private final boolean loennNorgeSkattepliktig;
    private final boolean loennUtlandSkattepliktig;

    public LocalDate getDatoMottatt() {
        return datoMottatt;
    }

    public List<Periode> getPerioder() {
        return perioder;
    }

    public boolean isErFullstendigInnvilget() {
        return erFullstendigInnvilget;
    }

    public String getFtrl_2_8_begrunnelse() {
        return ftrl_2_8_begrunnelse;
    }

    public boolean isVurderingMedlemskapEktefelle() {
        return vurderingMedlemskapEktefelle;
    }

    public boolean isVurderingLovvalgBarn() {
        return vurderingLovvalgBarn;
    }

    public List<FamiliemedlemInfo> getOmfattetFamilie() {
        return omfattetFamilie;
    }

    public List<IkkeOmfattetBarn> getIkkeOmfattetBarn() {
        return ikkeOmfattetBarn;
    }

    public IkkeOmfattetEktefelle getIkkeOmfattetEktefelle() {
        return ikkeOmfattetEktefelle;
    }

    public Innvilgelse getInnvilgelse() {
        return innvilgelse;
    }

    public String getArbeidsgiverNavn() {
        return arbeidsgiverNavn;
    }

    public String getArbeidsland() {
        return arbeidsland;
    }

    public boolean isTrygdeavtaleMedArbeidsland() {
        return trygdeavtaleMedArbeidsland;
    }

    public VurderingTrygdeavgift getVurderingTrygdeavgift() {
        return vurderingTrygdeavgift;
    }

    public String getLoennsforhold() {
        return loennsforhold;
    }

    public String getArbeidsgiverFullmektigNavn() {
        return arbeidsgiverFullmektigNavn;
    }

    public boolean isBrukerHarFullmektig() {
        return brukerHarFullmektig;
    }

    public String getAvgiftssatsAar() {
        return avgiftssatsAar;
    }

    public boolean isLoennNorgeSkattepliktig() {
        return loennNorgeSkattepliktig;
    }

    public boolean isLoennUtlandSkattepliktig() {
        return loennUtlandSkattepliktig;
    }

    public InnvilgelseFtrl(Builder builder) {
        super(builder.brevbestilling);
        this.innvilgelse = Innvilgelse.av(builder.brevbestilling);
        this.datoMottatt = builder.brevbestilling.getForsendelseMottatt() != null
            ? LocalDate.ofInstant(builder.brevbestilling.getForsendelseMottatt(), ZoneId.systemDefault()): null;
        this.perioder = builder.perioder;
        this.erFullstendigInnvilget = builder.erFullstendigInnvilget;
        this.ftrl_2_8_begrunnelse = builder.ftrl_2_8_begrunnelse;
        this.vurderingMedlemskapEktefelle = builder.vurderingMedlemskapEktefelle;
        this.vurderingLovvalgBarn = builder.vurderingLovvalgBarn;
        this.omfattetFamilie = builder.omfattetFamilie;
        this.ikkeOmfattetBarn = builder.ikkeOmfattetBarn;
        this.ikkeOmfattetEktefelle = builder.ikkeOmfattetEktefelle;
        this.arbeidsgiverNavn = builder.arbeidsgiverNavn;
        this.arbeidsland = builder.arbeidsland;
        this.trygdeavtaleMedArbeidsland = builder.trygdeavtaleMedArbeidsland;
        this.vurderingTrygdeavgift = builder.vurderingTrygdeavgift;
        this.loennsforhold = builder.loennsforhold;
        this.arbeidsgiverFullmektigNavn = builder.arbeidsgiverFullmektigNavn;
        this.brukerHarFullmektig = builder.brevbestilling.getBehandling().getFagsak().finnRepresentant(Representerer.BRUKER).isPresent();
        this.avgiftssatsAar = builder.avgiftssatsAar;
        this.loennNorgeSkattepliktig = builder.loennNorgeSkattepliktig;
        this.loennUtlandSkattepliktig = builder.loennUtlandSkattepliktig;
    }

    static public class Builder {
        private List<Periode> perioder;
        private boolean erFullstendigInnvilget;
        private String ftrl_2_8_begrunnelse;
        private boolean vurderingMedlemskapEktefelle;
        private boolean vurderingLovvalgBarn;
        private List<FamiliemedlemInfo> omfattetFamilie;
        private List<IkkeOmfattetBarn> ikkeOmfattetBarn;
        private IkkeOmfattetEktefelle ikkeOmfattetEktefelle;
        private String arbeidsgiverNavn;
        private String arbeidsland;
        private boolean trygdeavtaleMedArbeidsland;
        private VurderingTrygdeavgift vurderingTrygdeavgift;
        private String loennsforhold;
        private String arbeidsgiverFullmektigNavn;
        private String avgiftssatsAar;
        private boolean loennNorgeSkattepliktig;
        private boolean loennUtlandSkattepliktig;
        private final InnvilgelseBrevbestilling brevbestilling;

        public Builder(InnvilgelseBrevbestilling brevbestilling) {
            this.brevbestilling = brevbestilling;
        }

        public Builder perioder(List<Periode> perioder) {
            this.perioder = perioder;
            return this;
        }

        public Builder erFullstendigInnvilget(boolean erFullstendigInnvilget) {
            this.erFullstendigInnvilget = erFullstendigInnvilget;
            return this;
        }

        public Builder ftrl_2_8_begrunnelse(String ftrl_2_8_begrunnelse) {
            this.ftrl_2_8_begrunnelse = ftrl_2_8_begrunnelse;
            return this;
        }

        public Builder vurderingMedlemskapEktefelle(boolean vurderingMedlemskapEktefelle) {
            this.vurderingMedlemskapEktefelle = vurderingMedlemskapEktefelle;
            return this;
        }

        public Builder vurderingLovvalgBarn(boolean vurderingLovvalgBarn) {
            this.vurderingLovvalgBarn = vurderingLovvalgBarn;
            return this;
        }

        public Builder omfattetFamilie(List<FamiliemedlemInfo> omfattetFamilie) {
            this.omfattetFamilie = omfattetFamilie;
            return this;
        }

        public Builder ikkeOmfattetBarn(List<IkkeOmfattetBarn> ikkeOmfattetBarn) {
            this.ikkeOmfattetBarn = ikkeOmfattetBarn;
            return this;
        }

        public Builder ikkeOmfattetEktefelle(IkkeOmfattetEktefelle ikkeOmfattetEktefelle) {
            this.ikkeOmfattetEktefelle = ikkeOmfattetEktefelle;
            return this;
        }

        public Builder arbeidsgiverNavn(String arbeidsgiverNavn) {
            this.arbeidsgiverNavn = arbeidsgiverNavn;
            return this;
        }

        public Builder arbeidsland(String arbeidsland) {
            this.arbeidsland = arbeidsland;
            return this;
        }

        public Builder trygdeavtaleMedArbeidsland(boolean trygdeavtaleMedArbeidsland) {
            this.trygdeavtaleMedArbeidsland = trygdeavtaleMedArbeidsland;
            return this;
        }

        public Builder vurderingTrygdeavgift(VurderingTrygdeavgift vurderingTrygdeavgift) {
            this.vurderingTrygdeavgift = vurderingTrygdeavgift;
            return this;
        }

        public Builder loennsforhold(String loennsforhold) {
            this.loennsforhold = loennsforhold;
            return this;
        }

        public Builder arbeidsgiverFullmektigNavn(String arbeidsgiverFullmektigNavn) {
            this.arbeidsgiverFullmektigNavn = arbeidsgiverFullmektigNavn;
            return this;
        }

        public Builder avgiftssatsAar(String avgiftssatsAar) {
            this.avgiftssatsAar = avgiftssatsAar;
            return this;
        }

        public Builder loennNorgeSkattepliktig(boolean loennNorgeSkattepliktig) {
            this.loennNorgeSkattepliktig = loennNorgeSkattepliktig;
            return this;
        }

        public Builder loennUtlandSkattepliktig(boolean loennUtlandSkattepliktig) {
            this.loennUtlandSkattepliktig = loennUtlandSkattepliktig;
            return this;
        }

        public InnvilgelseFtrl build() {
            return new InnvilgelseFtrl(this);
        }
    }
}
