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

    public String arbeidsforholdREF;

    @XmlElement(required = true)
    public BigDecimal beloep;

    @XmlElement(required = true)
    public String fordel; //Fordel http://nav.no/kodeverk/Kodeverk/Fordel

    @XmlElement(required = true)
    public String inntektskilde; //"http://nav.no/kodeverk/Kodeverk/InntektsInformasjonsopphav"

    @XmlElement(required = true)
    public String inntektsperiodetype; //http://nav.no/kodeverk/Kodeverk/Inntektsperiodetyper

    @XmlElement(required = true)
    public String inntektsstatus; //"http://nav.no/kodeverk/Kodeverk/Inntektsstatuser"

    @XmlJavaTypeAdapter(OffsetDateTimeXmlAdapter.class)
    public LocalDateTime levereringstidspunkt;

    public String opptjeningsland;

    public Periode opptjeningsperiode;

    public String skattemessigBosattLand;

    @XmlJavaTypeAdapter(YearMonthXmlAdapter.class)
    @XmlElement(required = true)
    public YearMonth utbetaltIPeriode;

    public String opplysningspliktigID;

    public String inntektsinnsenderID;

    public String virksomhetID;

    public Tilleggsinformasjon tilleggsinformasjon;

    public String inntektsmottakerID;

    public Boolean inngaarIGrunnlagForTrekk;

    public Boolean utloeserArbeidsgiveravgift;

    public String informasjonsstatus; //"http://nav.no/kodeverk/Kodeverk/Informasjonsstatuser"


    public String getArbeidsforholdREF() {
        return arbeidsforholdREF;
    }

    public BigDecimal getBeloep() {
        return beloep;
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
