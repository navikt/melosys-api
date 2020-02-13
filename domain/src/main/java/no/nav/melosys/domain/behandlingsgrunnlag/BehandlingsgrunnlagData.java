package no.nav.melosys.domain.behandlingsgrunnlag;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import no.nav.melosys.domain.dokument.soeknad.*;
import org.apache.commons.lang3.StringUtils;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BehandlingsgrunnlagData {

    public Soeknadsland soeknadsland = new Soeknadsland();

    public Periode periode = new Periode();

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

    public List<MaritimtArbeid> maritimtArbeid = new ArrayList<>();

    public Bosted bosted = new Bosted();

    public Set<String> hentAlleOrganisasjonsnumre() {
        return selvstendigArbeid.hentAlleOrganisasjonsnumre()
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.toSet());
    }

    public Set<String> hentAllePersonnumre() {
        return personOpplysninger.hentAllePersonnummer()
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.toSet());
    }
}
