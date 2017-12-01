package no.nav.melosys.domain.dokument.inntekt;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.inntekt.inntektstype.Loennsinntekt;
import no.nav.melosys.domain.dokument.inntekt.inntektstype.Naeringsinntekt;
import no.nav.melosys.domain.dokument.inntekt.inntektstype.PensjonEllerTrygd;
import no.nav.melosys.domain.dokument.inntekt.inntektstype.YtelseFraOffentlige;
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.Tilleggsinformasjon;
import no.nav.melosys.domain.dokument.jaxb.OffsetDateTimeXmlAdapter;
import no.nav.melosys.domain.dokument.jaxb.YearMonthXmlAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Inntekt", propOrder = {
        "arbeidsforholdREF",
        "beloep",
        "fordel",
        "inntektskilde",
        "inntektsperiodetype",
        "inntektsstatus",
        "levereringstidspunkt",
        "opptjeningsland",
        "opptjeningsperiode",
        "skattemessigBosattLand",
        "utbetaltIPeriode",
        "opplysningspliktigID",
        "inntektsinnsenderID",
        "virksomhetID",
        "tilleggsinformasjon",
        "inntektsmottakerID",
        "inngaarIGrunnlagForTrekk",
        "utloeserArbeidsgiveravgift",
        "informasjonsstatus"
})
@XmlSeeAlso({
        Loennsinntekt.class,
        Naeringsinntekt.class,
        PensjonEllerTrygd.class,
        YtelseFraOffentlige.class
})
public class Inntekt {

    private String arbeidsforholdREF;

    @XmlElement(required = true)
    private BigDecimal beloep;

    @XmlElement(required = true)
    private String fordel; //Fordel http://nav.no/kodeverk/Kodeverk/Fordel

    @XmlElement(required = true)
    private String inntektskilde; //"http://nav.no/kodeverk/Kodeverk/InntektsInformasjonsopphav"

    @XmlElement(required = true)
    private String inntektsperiodetype; //http://nav.no/kodeverk/Kodeverk/Inntektsperiodetyper

    @XmlElement(required = true)
    private String inntektsstatus; //"http://nav.no/kodeverk/Kodeverk/Inntektsstatuser"

    @XmlJavaTypeAdapter(OffsetDateTimeXmlAdapter.class)
    private LocalDateTime levereringstidspunkt;

    private String opptjeningsland;

    private Periode opptjeningsperiode;

    private String skattemessigBosattLand;

    @XmlJavaTypeAdapter(YearMonthXmlAdapter.class)
    @XmlElement(required = true)
    private YearMonth utbetaltIPeriode;

    private String opplysningspliktigID;

    private String inntektsinnsenderID;

    private String virksomhetID;

    private Tilleggsinformasjon tilleggsinformasjon;

    private String inntektsmottakerID;

    private Boolean inngaarIGrunnlagForTrekk;

    private Boolean utloeserArbeidsgiveravgift;

    private String informasjonsstatus; //"http://nav.no/kodeverk/Kodeverk/Informasjonsstatuser"


    public String getArbeidsforholdREF() {
        return arbeidsforholdREF;
    }

    public void setArbeidsforholdREF(String arbeidsforholdREF) {
        this.arbeidsforholdREF = arbeidsforholdREF;
    }

    public BigDecimal getBeloep() {
        return beloep;
    }

    public void setBeloep(BigDecimal beloep) {
        this.beloep = beloep;
    }

    public String getFordel() {
        return fordel;
    }

    public void setFordel(String fordel) {
        this.fordel = fordel;
    }

    public String getInntektskilde() {
        return inntektskilde;
    }

    public void setInntektskilde(String inntektskilde) {
        this.inntektskilde = inntektskilde;
    }

    public String getInntektsperiodetype() {
        return inntektsperiodetype;
    }

    public void setInntektsperiodetype(String inntektsperiodetype) {
        this.inntektsperiodetype = inntektsperiodetype;
    }

    public String getInntektsstatus() {
        return inntektsstatus;
    }

    public void setInntektsstatus(String inntektsstatus) {
        this.inntektsstatus = inntektsstatus;
    }

    public LocalDateTime getLevereringstidspunkt() {
        return levereringstidspunkt;
    }

    public void setLevereringstidspunkt(LocalDateTime levereringstidspunkt) {
        this.levereringstidspunkt = levereringstidspunkt;
    }

    public String getOpptjeningsland() {
        return opptjeningsland;
    }

    public void setOpptjeningsland(String opptjeningsland) {
        this.opptjeningsland = opptjeningsland;
    }

    public Periode getOpptjeningsperiode() {
        return opptjeningsperiode;
    }

    public void setOpptjeningsperiode(Periode opptjeningsperiode) {
        this.opptjeningsperiode = opptjeningsperiode;
    }

    public String getSkattemessigBosattLand() {
        return skattemessigBosattLand;
    }

    public void setSkattemessigBosattLand(String skattemessigBosattLand) {
        this.skattemessigBosattLand = skattemessigBosattLand;
    }

    public YearMonth getUtbetaltIPeriode() {
        return utbetaltIPeriode;
    }

    public void setUtbetaltIPeriode(YearMonth utbetaltIPeriode) {
        this.utbetaltIPeriode = utbetaltIPeriode;
    }

    public String getOpplysningspliktigID() {
        return opplysningspliktigID;
    }

    public void setOpplysningspliktigID(String opplysningspliktigID) {
        this.opplysningspliktigID = opplysningspliktigID;
    }

    public String getInntektsinnsenderID() {
        return inntektsinnsenderID;
    }

    public void setInntektsinnsenderID(String inntektsinnsenderID) {
        this.inntektsinnsenderID = inntektsinnsenderID;
    }

    public String getVirksomhetID() {
        return virksomhetID;
    }

    public void setVirksomhetID(String virksomhetID) {
        this.virksomhetID = virksomhetID;
    }

    public Tilleggsinformasjon getTilleggsinformasjon() {
        return tilleggsinformasjon;
    }

    public void setTilleggsinformasjon(Tilleggsinformasjon tilleggsinformasjon) {
        this.tilleggsinformasjon = tilleggsinformasjon;
    }

    public String getInntektsmottakerID() {
        return inntektsmottakerID;
    }

    public void setInntektsmottakerID(String inntektsmottakerID) {
        this.inntektsmottakerID = inntektsmottakerID;
    }

    public Boolean getInngaarIGrunnlagForTrekk() {
        return inngaarIGrunnlagForTrekk;
    }

    public void setInngaarIGrunnlagForTrekk(Boolean inngaarIGrunnlagForTrekk) {
        this.inngaarIGrunnlagForTrekk = inngaarIGrunnlagForTrekk;
    }

    public Boolean getUtloeserArbeidsgiveravgift() {
        return utloeserArbeidsgiveravgift;
    }

    public void setUtloeserArbeidsgiveravgift(Boolean utloeserArbeidsgiveravgift) {
        this.utloeserArbeidsgiveravgift = utloeserArbeidsgiveravgift;
    }
}
