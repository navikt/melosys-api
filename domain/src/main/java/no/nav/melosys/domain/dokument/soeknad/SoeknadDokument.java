package no.nav.melosys.domain.dokument.soeknad;

import java.time.LocalDate;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonRootName;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.Familiemedlem;
import no.nav.melosys.domain.dokument.person.Sivilstand;

@JsonRootName("soknadDokument")
@XmlRootElement
public class SoeknadDokument extends SaksopplysningDokument {

    // Personopplysninger(FIXME er det en del av søknaden?)
    public String sammensattNavn;
    public String fnr;
    public LocalDate fødselsdato;
    public Bostedsadresse bostedsadresseNorge;
    public Bostedsadresse bostedsadresseUtland;
    public Land statsborgerskap;
    public String utenlandskId;
    // Barn sendes med i søknaden, da det ikke er gitt av TPS hvilke som er medfølgende.
    public List<Familiemedlem> familiemedlemmer;
    public Sivilstand sivilstand;

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

    // Bekreftelser fra arbeidsgiveren
    public ArbeidsgiversBekreftelse arbeidsgiversBekreftelse;

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