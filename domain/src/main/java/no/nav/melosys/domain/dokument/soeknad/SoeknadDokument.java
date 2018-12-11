package no.nav.melosys.domain.dokument.soeknad;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.bind.annotation.XmlRootElement;

import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import org.apache.commons.lang3.StringUtils;

@XmlRootElement
public class SoeknadDokument extends SaksopplysningDokument {

    public OpplysningerOmBrukeren personOpplysninger = new OpplysningerOmBrukeren();

    // Opplysninger om arbeid i utlandet
    public List<ArbeidUtland> arbeidUtland = new ArrayList<>();

    // Opplysninger om foretak i utlandet
    public List<ForetakUtland> foretakUtland = new ArrayList<>();

    // Opplysninger om opphold i utland
    public OppholdUtland oppholdUtland = new OppholdUtland();

    // Opplysninger om arbeid i Norge
    public ArbeidNorge arbeidNorge = new ArbeidNorge();

    public SelvstendigArbeid selvstendigArbeid = new SelvstendigArbeid();

    // Opplysninger om juridiske arbeidsgiver i Norge
    public JuridiskArbeidsgiverNorge juridiskArbeidsgiverNorge = new JuridiskArbeidsgiverNorge();

    // Opplysninger om arbeidsinntekt
    public Arbeidsinntekt arbeidsinntekt = new Arbeidsinntekt();

    // Bekreftelser fra arbeidsgiveren
    public ArbeidsgiversBekreftelse arbeidsgiversBekreftelse = new ArbeidsgiversBekreftelse();

    public MaritimtArbeid maritimtArbeid = new MaritimtArbeid();

    public Bosted bosted = new Bosted();

    public Set<String> hentAlleOrganisasjonsnumre() {
        return Stream.of(selvstendigArbeid.hentAlleOrganisasjonsnumre(),
                         juridiskArbeidsgiverNorge.hentAlleOrganisasjonsnumre())
                .flatMap(i -> i)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toSet());
    }

    public Set<String> hentAllePersonnumre() {
        return personOpplysninger.hentAllePersonnummer()
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toSet());
    }
}