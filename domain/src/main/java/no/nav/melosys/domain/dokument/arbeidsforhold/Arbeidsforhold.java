package no.nav.melosys.domain.dokument.arbeidsforhold;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.HarPeriode;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.jaxb.XMLDateTimeToOffsetDateTime;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class Arbeidsforhold implements HarPeriode {

    private String arbeidsforholdID;

    private long arbeidsforholdIDnav;

    private Periode ansettelsesPeriode;

    private String arbeidsforholdstype; //"http://nav.no/kodeverk/Kodeverk/Arbeidsforholdstyper"

    @XmlElementWrapper(name="arbeidsavtaler")
    @XmlElement(name="avtale")
    private List<Arbeidsavtale> arbeidsavtaler = new ArrayList<>();

    @XmlElementWrapper(name="permisjonOgPermittering")
    @XmlElement(name="permisjonOgPermittering")
    private List<PermisjonOgPermittering> permisjonOgPermittering = new ArrayList<>();

    private List<Utenlandsopphold> utenlandsopphold = new ArrayList<>();

    private Aktoertype arbeidsgivertype;

    private String arbeidsgiverID;

    private String arbeidstakerID;

    private Aktoertype opplysningspliktigtype;

    private String opplysningspliktigID;

    @JsonProperty("Aordning")
    private Boolean arbeidsforholdInnrapportertEtterAOrdningen;

    @XmlJavaTypeAdapter(XMLDateTimeToOffsetDateTime.class)
    private OffsetDateTime opprettelsestidspunkt;

    @XmlJavaTypeAdapter(XMLDateTimeToOffsetDateTime.class)
    private OffsetDateTime sistBekreftet;

    @JsonProperty("timerTimelonnet")
    private List<AntallTimerIPerioden> antallTimerForTimeloennet;

    // FIXME: Sjekk om dette påvirker JAX.
    // Hvis det gjør det, gjør nødvendige endringer slik at det ikke gjør det. Gjør samme endringer også i andre relevante klasser som implementerer HarPeriode
    @Override
    @JsonIgnore
    public ErPeriode getPeriode() {
        return ansettelsesPeriode;
    }

    public String getArbeidsforholdID() {
        return arbeidsforholdID;
    }

    public void setArbeidsforholdID(String arbeidsforholdID) {
        this.arbeidsforholdID = arbeidsforholdID;
    }

    public long getArbeidsforholdIDnav() {
        return arbeidsforholdIDnav;
    }

    public void setArbeidsforholdIDnav(long arbeidsforholdIDnav) {
        this.arbeidsforholdIDnav = arbeidsforholdIDnav;
    }

    public Periode getAnsettelsesPeriode() {
        return ansettelsesPeriode;
    }

    public void setAnsettelsesPeriode(Periode ansettelsesPeriode) {
        this.ansettelsesPeriode = ansettelsesPeriode;
    }

    public String getArbeidsforholdstype() {
        return arbeidsforholdstype;
    }

    public void setArbeidsforholdstype(String arbeidsforholdstype) {
        this.arbeidsforholdstype = arbeidsforholdstype;
    }

    public List<PermisjonOgPermittering> getPermisjonOgPermittering() {
        return permisjonOgPermittering;
    }

    public void setPermisjonOgPermittering(List<PermisjonOgPermittering> permisjonOgPermittering) {
        this.permisjonOgPermittering = permisjonOgPermittering;
    }

    public List<Utenlandsopphold> getUtenlandsopphold() {
        return utenlandsopphold;
    }

    public void setUtenlandsopphold(List<Utenlandsopphold> utenlandsopphold) {
        this.utenlandsopphold = utenlandsopphold;
    }

    public List<Arbeidsavtale> getArbeidsavtaler() {
        return arbeidsavtaler;
    }

    public void setArbeidsavtaler(List<Arbeidsavtale> arbeidsavtaler) {
        this.arbeidsavtaler = arbeidsavtaler;
    }

    public Aktoertype getArbeidsgivertype() {
        return arbeidsgivertype;
    }

    public void setArbeidsgivertype(Aktoertype arbeidsgivertype) {
        this.arbeidsgivertype = arbeidsgivertype;
    }

    public String getArbeidsgiverID() {
        return arbeidsgiverID;
    }

    public void setArbeidsgiverID(String arbeidsgiverID) {
        this.arbeidsgiverID = arbeidsgiverID;
    }

    public String getArbeidstakerID() {
        return arbeidstakerID;
    }

    public void setArbeidstakerID(String arbeidstakerID) {
        this.arbeidstakerID = arbeidstakerID;
    }

    public Aktoertype getOpplysningspliktigtype() {
        return opplysningspliktigtype;
    }

    public void setOpplysningspliktigtype(Aktoertype opplysningspliktigtype) {
        this.opplysningspliktigtype = opplysningspliktigtype;
    }

    public String getOpplysningspliktigID() {
        return opplysningspliktigID;
    }

    public void setOpplysningspliktigID(String opplysningspliktigID) {
        this.opplysningspliktigID = opplysningspliktigID;
    }

    public Boolean getArbeidsforholdInnrapportertEtterAOrdningen() {
        return arbeidsforholdInnrapportertEtterAOrdningen;
    }

    public void setArbeidsforholdInnrapportertEtterAOrdningen(Boolean arbeidsforholdInnrapportertEtterAOrdningen) {
        this.arbeidsforholdInnrapportertEtterAOrdningen = arbeidsforholdInnrapportertEtterAOrdningen;
    }

    public OffsetDateTime getOpprettelsestidspunkt() {
        return opprettelsestidspunkt;
    }

    public void setOpprettelsestidspunkt(OffsetDateTime opprettelsestidspunkt) {
        this.opprettelsestidspunkt = opprettelsestidspunkt;
    }

    public OffsetDateTime getSistBekreftet() {
        return sistBekreftet;
    }

    public void setSistBekreftet(OffsetDateTime sistBekreftet) {
        this.sistBekreftet = sistBekreftet;
    }

    public List<AntallTimerIPerioden> getAntallTimerForTimeloennet() {
        return antallTimerForTimeloennet;
    }

    public void setAntallTimerForTimeloennet(List<AntallTimerIPerioden> antallTimerForTimeloennet) {
        this.antallTimerForTimeloennet = antallTimerForTimeloennet;
    }
}
