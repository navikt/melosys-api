package no.nav.melosys.domain.dokument.arbeidsforhold;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import no.nav.melosys.domain.dokument.felles.Periode;

@XmlAccessorType(XmlAccessType.FIELD)
public class Arbeidsforhold {

    private String arbeidsforholdID;

    private long arbeidsforholdIDnav;

    private Periode ansettelsesPeriode;

    private String arbeidsforholdstype;

    @XmlElementWrapper(name="arbeidsforhold")
    @XmlElement(name="arbeidsforhold")
    private List<PermisjonOgPermittering> permisjonOgPermittering = new ArrayList<>();

    @XmlElementWrapper(name="utenlandsopphold")
    @XmlElement(name="opphold")
    private List<Utenlandsopphold> utenlandsopphold = new ArrayList<>();

    @XmlElementWrapper(name="arbeidsavtaler")
    @XmlElement(name="avtale")
    private List<Arbeidsavtale> arbeidsavtaler = new ArrayList<>();

    private String arbeidsgiverID;

    private String arbeidstakerID;

    private String opplysningspliktigID;

    private Boolean arbeidsforholdInnrapportertEtterAOrdningen;

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
}
