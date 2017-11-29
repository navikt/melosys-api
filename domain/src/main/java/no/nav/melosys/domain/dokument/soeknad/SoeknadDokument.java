package no.nav.melosys.domain.dokument.soeknad;

import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.felles.Landkode;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.organisasjon.adresse.Gateadresse;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;

import javax.xml.bind.annotation.XmlRootElement;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@XmlRootElement
public class SoeknadDokument extends SaksopplysningDokument {

    // Personopplysninger
    public String sammensattNavn;
    public String fnr;
    public LocalDate fødselsdato;
    public Bostedsadresse bostedsadresseNorge;
    public Bostedsadresse bostedsadresseUtland;
    public Landkode statsborgerskap;
    public String utenlandskId;
    // FIXME: Barn? (Barn kan utfylles med TPS)

    // Opplysninger om arbeid i utlandet
    public List<Landkode> arbeidsland;
    public Periode arbeidsperiode;
    public Gateadresse arbeidsstedUtland; // TODO kan Gateadresse brukes? SED må sjekkes
    public BigDecimal arbeidsandelNorge;
    public BigDecimal arbeidsandelUtland;
    public Landkode bostedsland;
    public Boolean erstatterTidligereUtsendt;

    // Opplysninger om foretak i utlandet
    public String foretakUtlandNavn;
    public String foretakUtlandOrgnr;
    public String foretakUtlandAdresse; // TODO kan Gateadresse brukes?

    // Opplysninger om opphold i utland
    public Landkode oppholdsland;
    public Periode oppholdsPeriode;
    public Boolean studentIEOS;
    public String studentFinansiering;
    public String studentSemester;
    public Landkode studieLand;

    // Opplysninger om arbeid i Norge
    public Boolean arbeidsforholdOpprettholdIHelePerioden;
    public Boolean brukerErSelvstendigNæringsdrivende;
    public Boolean selvstendigFortsetterEtterArbeidIUtlandet;
    public Boolean brukerArbeiderIVikarbyrå; //TODO fjernes?
    public String vikarOrgnr;
    public String flyendePersonellHjemmebase;
    public Boolean ansattPåSokkelEllerSkip;
    public String navnSkipEllerSokkel;
    public Landkode sokkelLand;
    public String skipFartsomrade;
    public Landkode skipFlaggLand;

    // Opplysninger om juridiske arbeidsgivere i Norge
    // Opplysningene er for å kunne vurdere vesentlig virksomhet i Norge.
    // De er bare relevant når det gjelder utsendt arbeidstaker
    // og pre-utfyllingen fra informasjon innsendt tidligere (fra samme arbeidsgiver) er eldre enn 12 måneder.
    public Integer arbeidsgiverNorgeAntallAnsatte;
    public Integer arbeidsgiverNorgeAntallAdminAnsatte;
    public Integer arbeidsgiverNorgeAntallAdminAnsatteEØS;
    public BigDecimal arbeidsgiverNorgeAndelOmsetningINorge;
    public BigDecimal arbeidsgiverNorgeAndelKontrakterINorge;
    public Boolean arbeidsgiverNorgeErBemanningsbyra;
    public Boolean arbeidsgiverNorgeHattDriftSiste12Mnd;
    public Integer arbeidsgiverNorgeAntallUtsendte;

    // Opplysninger om arbeidsinntekt
    public Integer inntektNorskIPerioden;
    public Integer inntektUtenlandskIPerioden;
    public Integer inntektNaeringIPerioden;
    public List<String> inntektNaturalYtelser;
    public Boolean inntektErInnrapporteringspliktig;
    public Boolean inntektTrygdeavgiftBlirTrukket;

    // Øvrige
    public String tilleggsopplysninger;

}