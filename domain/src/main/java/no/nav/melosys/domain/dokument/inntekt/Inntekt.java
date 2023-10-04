package no.nav.melosys.domain.dokument.inntekt;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

import com.fasterxml.jackson.annotation.JsonView;
import no.nav.melosys.domain.dokument.DokumentView;
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.Tilleggsinformasjon;
import org.jetbrains.annotations.NotNull;

public class Inntekt {

    @JsonView(DokumentView.Database.class)
    public String arbeidsforholdREF;

    @NotNull
    public BigDecimal beloep;

    @NotNull
    public String fordel; //Fordel http://nav.no/kodeverk/Kodeverk/Fordel

    @NotNull
    public String inntektskilde; //"http://nav.no/kodeverk/Kodeverk/InntektsInformasjonsopphav"

    public String inntektsperiodetype; //http://nav.no/kodeverk/Kodeverk/Inntektsperiodetyper

    @NotNull
    public String inntektsstatus; //"http://nav.no/kodeverk/Kodeverk/Inntektsstatuser"

    public LocalDateTime levereringstidspunkt;

    public String opptjeningsland;

    public Periode opptjeningsperiode = new Periode();

    @JsonView(DokumentView.Database.class)
    public String skattemessigBosattLand;

    public YearMonth utbetaltIPeriode;

    public String opplysningspliktigID;

    @JsonView(DokumentView.Database.class)
    public String inntektsinnsenderID;

    public String virksomhetID;

    @JsonView(DokumentView.Database.class)
    public Tilleggsinformasjon tilleggsinformasjon;

    public String inntektsmottakerID;

    public Boolean inngaarIGrunnlagForTrekk;

    public Boolean utloeserArbeidsgiveravgift;

    public String informasjonsstatus; //"http://nav.no/kodeverk/Kodeverk/Informasjonsstatuser"

    public void setBeskrivelse(String beskrivelse) {
        this.beskrivelse = beskrivelse;
    }

    @NotNull
    private String beskrivelse;

    public String getArbeidsforholdREF() {
        return arbeidsforholdREF;
    }

    public BigDecimal getBeloep() {
        return beloep;
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }

    public String getFordel() {
        return fordel;
    }

    public String getInntektskilde() {
        return inntektskilde;
    }

    public String getInntektsperiodetype() {
        return inntektsperiodetype;
    }

    public String getInntektsstatus() {
        return inntektsstatus;
    }

    public LocalDateTime getLevereringstidspunkt() {
        return levereringstidspunkt;
    }

    public String getOpptjeningsland() {
        return opptjeningsland;
    }

    public Periode getOpptjeningsperiode() {
        return opptjeningsperiode;
    }

    public String getSkattemessigBosattLand() {
        return skattemessigBosattLand;
    }

    public YearMonth getUtbetaltIPeriode() {
        return utbetaltIPeriode;
    }

    public String getOpplysningspliktigID() {
        return opplysningspliktigID;
    }

    public String getInntektsinnsenderID() {
        return inntektsinnsenderID;
    }

    public String getVirksomhetID() {
        return virksomhetID;
    }

    public Tilleggsinformasjon getTilleggsinformasjon() {
        return tilleggsinformasjon;
    }

    public String getInntektsmottakerID() {
        return inntektsmottakerID;
    }

    public Boolean getInngaarIGrunnlagForTrekk() {
        return inngaarIGrunnlagForTrekk;
    }

    public Boolean getUtloeserArbeidsgiveravgift() {
        return utloeserArbeidsgiveravgift;
    }

}
