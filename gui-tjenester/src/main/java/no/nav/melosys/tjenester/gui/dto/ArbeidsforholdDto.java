package no.nav.melosys.tjenester.gui.dto;

import static no.nav.melosys.tjenester.gui.dto.util.DtoUtils.tilLocalDate;

import java.util.ArrayList;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Aktoer;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.AnsettelsesPeriode;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Arbeidsavtale;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Arbeidsforhold;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Arbeidsforholdstyper;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Organisasjon;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.PermisjonOgPermittering;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Utenlandsopphold;

public class ArbeidsforholdDto {

    private String arbeidsforholdID;

    private long arbeidsforholdIDnav;

    private PeriodeDto ansettelsesPeriode;

    private String arbeidsforholdstype;

    private List<PermisjonOgPermitteringDto> permisjonOgPermittering = new ArrayList<>();

    private List<UtenlandsoppholdDto> utenlandsopphold = new ArrayList<>();

    private List<ArbeidsavtaleDto> arbeidsavtale = new ArrayList<>();

    private OrganisajonDto arbeidsgiver;

    private String arbeidstaker;

    private OrganisajonDto opplysningspliktig;

    private Boolean arbeidsforholdInnrapportertEtterAOrdningen;

    public ArbeidsforholdDto() {
    }

    public static ArbeidsforholdDto toDto(Arbeidsforhold a) {
        ArbeidsforholdDto arbeidsforhold = new ArbeidsforholdDto();

        arbeidsforhold.setArbeidsforholdID(a.getArbeidsforholdID());
        arbeidsforhold.setArbeidsforholdIDnav(a.getArbeidsforholdIDnav());

        arbeidsforhold.setArbeidsforholdInnrapportertEtterAOrdningen(a.isArbeidsforholdInnrapportertEtterAOrdningen());

        arbeidsforhold.setArbeidstaker(a.getArbeidstaker().getIdent().getIdent());

        Aktoer arbeidsgiverXml = a.getArbeidsgiver();
        if (arbeidsgiverXml instanceof Organisasjon) {
            // Organisasjonsnummer - til den virksomheten hvor arbeidsforholdet er knyttet
            String orgnummer = ((Organisasjon) arbeidsgiverXml).getOrgnummer();
            OrganisajonDto arbeidsgiver = new OrganisajonDto(orgnummer);
            arbeidsforhold.setArbeidsgiver(arbeidsgiver);
        }

        // Startdato og slutdato arbeidsforhold
        AnsettelsesPeriode periode = a.getAnsettelsesPeriode();
        XMLGregorianCalendar fraOgMed = periode.getPeriode().getFom();
        XMLGregorianCalendar tilOgMed = periode.getPeriode().getTom();
        PeriodeDto periodeDto = new PeriodeDto(tilLocalDate(fraOgMed), tilLocalDate(tilOgMed));
        arbeidsforhold.setAnsettelsesPeriode(periodeDto);

        // Type arbeidsforhold - "Ordinært", "Maritimt", «Forenklet oppgjørsordning»...
        // Ikke påkrevd i tjenesten.
        Arbeidsforholdstyper type = a.getArbeidsforholdstype();
        if (type != null) {
            arbeidsforhold.setArbeidsforholdstype(type.getValue()); //TODO Kodeverk
        }

        // Opplysningspliktig - juridisk enhet (vesentlig virksomhet i Norge?)
        Aktoer opplysningspliktig = a.getOpplysningspliktig();
        if (opplysningspliktig instanceof Organisasjon) {
            String orgnummer = ((Organisasjon) opplysningspliktig).getOrgnummer();

            OrganisajonDto organisasjon = new OrganisajonDto(orgnummer);
            arbeidsforhold.setOpplysningspliktig(organisasjon);

        }

        // Dato sist bekreftet
        // TODO arbeidsforhold.set(xmlTilLocalDate(a.getSistBekreftet()));

        // PermisjonOgPermitteringDto. Til å gjøre kontroll etter vedtak er innvilget. Ikke direkte vilkårvurdering.
        // TODO Hentes med et separat kall?
        List<PermisjonOgPermittering> permisjoner = a.getPermisjonOgPermittering();
        for (PermisjonOgPermittering p : permisjoner) {
            PermisjonOgPermitteringDto permisjon = new PermisjonOgPermitteringDto();
            permisjon.setPermisjonsId(p.getPermisjonsId());

            PeriodeDto permisjonsPeriode = new PeriodeDto();
            permisjonsPeriode.setFom(tilLocalDate(p.getPermisjonsPeriode().getFom()));
            permisjonsPeriode.setTom(tilLocalDate(p.getPermisjonsPeriode().getTom()));
            permisjon.setPermisjonsPeriode(permisjonsPeriode);

            permisjon.setPermisjonsprosent(p.getPermisjonsprosent());
            permisjon.setPermisjonOgPermittering(p.getPermisjonOgPermittering().getValue());
        }

        // TODO Utenlandsopphold
        List<Utenlandsopphold> oppholdListe = a.getUtenlandsopphold();
        for (Utenlandsopphold o : oppholdListe) {
            UtenlandsoppholdDto utenlandsopphold = new UtenlandsoppholdDto();
            utenlandsopphold.setLand(o.getLand().getValue());

            // TODO rapporteringsperiode eller periode?
            // Rapporteringsperiode: Tidsperioden som ble dekket i rapporten
            PeriodeDto oppholdPeriode = new PeriodeDto(tilLocalDate(o.getPeriode().getFom()), tilLocalDate(o.getPeriode().getTom()));
            utenlandsopphold.setPeriode(oppholdPeriode);
        }

        // Arbeidsavtaler
        List<Arbeidsavtale> avtaler = a.getArbeidsavtale();
        List<ArbeidsavtaleDto> arbeidsavtaleListe = new ArrayList<>();
        avtaler.forEach(x -> arbeidsavtaleListe.add(ArbeidsavtaleDto.tilDto(x)));
        arbeidsforhold.setArbeidsavtale(arbeidsavtaleListe);

        return arbeidsforhold;
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

    public PeriodeDto getAnsettelsesPeriode() {
        return ansettelsesPeriode;
    }

    public void setAnsettelsesPeriode(PeriodeDto ansettelsesPeriode) {
        this.ansettelsesPeriode = ansettelsesPeriode;
    }

    public String getArbeidsforholdstype() {
        return arbeidsforholdstype;
    }

    public void setArbeidsforholdstype(String arbeidsforholdstype) {
        this.arbeidsforholdstype = arbeidsforholdstype;
    }

    public List<PermisjonOgPermitteringDto> getPermisjonOgPermittering() {
        return permisjonOgPermittering;
    }

    public void setPermisjonOgPermittering(List<PermisjonOgPermitteringDto> permisjonOgPermittering) {
        this.permisjonOgPermittering = permisjonOgPermittering;
    }

    public List<UtenlandsoppholdDto> getUtenlandsopphold() {
        return utenlandsopphold;
    }

    public void setUtenlandsopphold(List<UtenlandsoppholdDto> utenlandsopphold) {
        this.utenlandsopphold = utenlandsopphold;
    }

    public List<ArbeidsavtaleDto> getArbeidsavtale() {
        return arbeidsavtale;
    }

    public void setArbeidsavtale(List<ArbeidsavtaleDto> arbeidsavtale) {
        this.arbeidsavtale = arbeidsavtale;
    }

    public OrganisajonDto getArbeidsgiver() {
        return arbeidsgiver;
    }

    public void setArbeidsgiver(OrganisajonDto arbeidsgiver) {
        this.arbeidsgiver = arbeidsgiver;
    }

    public String getArbeidstaker() {
        return arbeidstaker;
    }

    public void setArbeidstaker(String arbeidstaker) {
        this.arbeidstaker = arbeidstaker;
    }

    public OrganisajonDto getOpplysningspliktig() {
        return opplysningspliktig;
    }

    public void setOpplysningspliktig(OrganisajonDto opplysningspliktig) {
        this.opplysningspliktig = opplysningspliktig;
    }

    public Boolean getArbeidsforholdInnrapportertEtterAOrdningen() {
        return arbeidsforholdInnrapportertEtterAOrdningen;
    }

    public void setArbeidsforholdInnrapportertEtterAOrdningen(Boolean arbeidsforholdInnrapportertEtterAOrdningen) {
        this.arbeidsforholdInnrapportertEtterAOrdningen = arbeidsforholdInnrapportertEtterAOrdningen;
    }

}


