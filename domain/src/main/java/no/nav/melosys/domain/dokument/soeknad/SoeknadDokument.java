package no.nav.melosys.domain.dokument.soeknad;

import com.fasterxml.jackson.annotation.JsonRootName;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.felles.Landkode;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;

import javax.xml.bind.annotation.XmlRootElement;
import java.time.LocalDate;

@JsonRootName("soknadDokument")
@XmlRootElement
public class SoeknadDokument extends SaksopplysningDokument {

    // Personopplysninger(FIXME er det en del av søknaden?)
    public String sammensattNavn;
    public String fnr;
    public LocalDate fødselsdato;
    public Bostedsadresse bostedsadresseNorge;
    public Bostedsadresse bostedsadresseUtland;
    public Landkode statsborgerskap;
    public String utenlandskId;
    // FIXME: Barn? (Barn kan utfylles med TPS)

    // Opplysninger om arbeid i utlandet
    public ArbeidUtland arbeidUtland;

    // Opplysninger om foretak i utlandet
    public ForetakUtland foretakUtland;

    // Opplysninger om opphold i utland
    public OppholdUtland oppholdUtland;

    // Opplysninger om arbeid i Norge
    public ArbeidNorge arbeidNorge;

    // Opplysninger om juridiske arbeidsgiver i Norge
    public JuridiskArbeidsgiverNorge juridiskArbeidsgiverNorge;

    // Opplysninger om arbeidsinntekt
    public Arbeidsinntekt arbeidsinntekt;

    // Øvrige
    public String tilleggsopplysninger;

    public SoeknadDokument() {
        arbeidUtland = new ArbeidUtland();
        foretakUtland = new ForetakUtland();
        oppholdUtland = new OppholdUtland();
        arbeidNorge = new ArbeidNorge();
        juridiskArbeidsgiverNorge = new JuridiskArbeidsgiverNorge();
        arbeidsinntekt = new Arbeidsinntekt();
    }

}