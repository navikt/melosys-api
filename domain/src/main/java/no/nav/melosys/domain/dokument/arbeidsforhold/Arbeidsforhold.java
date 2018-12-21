package no.nav.melosys.domain.dokument.arbeidsforhold;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.HarPeriode;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.jaxb.XMLDateTimeToOffsetDateTime;

@XmlAccessorType(XmlAccessType.FIELD)
public class Arbeidsforhold implements HarPeriode {

    public String arbeidsforholdID;

    public long arbeidsforholdIDnav;

    public Periode ansettelsesPeriode;

    public String arbeidsforholdstype; //"http://nav.no/kodeverk/Kodeverk/Arbeidsforholdstyper"

    @XmlElementWrapper(name="arbeidsavtaler")
    @XmlElement(name="avtale")
    public List<Arbeidsavtale> arbeidsavtaler = new ArrayList<>();

    @XmlElementWrapper(name="permisjonOgPermittering")
    @XmlElement(name="permisjonOgPermittering")
    public List<PermisjonOgPermittering> permisjonOgPermittering = new ArrayList<>();

    public List<Utenlandsopphold> utenlandsopphold = new ArrayList<>();

    public Aktoertype arbeidsgivertype;

    public String arbeidsgiverID;

    public String arbeidstakerID;

    public Aktoertype opplysningspliktigtype;

    public String opplysningspliktigID;

    @JsonProperty("Aordning")
    public Boolean arbeidsforholdInnrapportertEtterAOrdningen;

    @XmlJavaTypeAdapter(XMLDateTimeToOffsetDateTime.class)
    public OffsetDateTime opprettelsestidspunkt;

    @XmlJavaTypeAdapter(XMLDateTimeToOffsetDateTime.class)
    public OffsetDateTime sistBekreftet;

    @JsonProperty("timerTimelonnet")
    public List<AntallTimerIPerioden> antallTimerForTimeloennet = new ArrayList<>();

    @Override
    @JsonIgnore
    public ErPeriode getPeriode() {
        return ansettelsesPeriode;
    }

    public String getArbeidsforholdID() {
        return arbeidsforholdID;
    }

    public long getArbeidsforholdIDnav() {
        return arbeidsforholdIDnav;
    }

    public Periode getAnsettelsesPeriode() {
        return ansettelsesPeriode;
    }

    public String getArbeidsforholdstype() {
        return arbeidsforholdstype;
    }

    public List<PermisjonOgPermittering> getPermisjonOgPermittering() {
        return permisjonOgPermittering;
    }

    public List<Utenlandsopphold> getUtenlandsopphold() {
        return utenlandsopphold;
    }

    public List<Arbeidsavtale> getArbeidsavtaler() {
        return arbeidsavtaler;
    }

    public Aktoertype getArbeidsgivertype() {
        return arbeidsgivertype;
    }

    public String getArbeidsgiverID() {
        return arbeidsgiverID;
    }

    public String getArbeidstakerID() {
        return arbeidstakerID;
    }

    public Aktoertype getOpplysningspliktigtype() {
        return opplysningspliktigtype;
    }

    public String getOpplysningspliktigID() {
        return opplysningspliktigID;
    }

    public Boolean getArbeidsforholdInnrapportertEtterAOrdningen() {
        return arbeidsforholdInnrapportertEtterAOrdningen;
    }

    public OffsetDateTime getOpprettelsestidspunkt() {
        return opprettelsestidspunkt;
    }

    public OffsetDateTime getSistBekreftet() {
        return sistBekreftet;
    }

    public List<AntallTimerIPerioden> getAntallTimerForTimeloennet() {
        return antallTimerForTimeloennet;
    }

}
