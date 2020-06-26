package no.nav.melosys.domain.behandlingsgrunnlag;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public SelvstendigArbeid selvstendigArbeid = new SelvstendigArbeid();

    // Opplysninger om juridiske arbeidsgiver i Norge
    public JuridiskArbeidsgiverNorge juridiskArbeidsgiverNorge = new JuridiskArbeidsgiverNorge();

    public List<MaritimtArbeid> maritimtArbeid = new ArrayList<>();

    public List<LuftfartBase> luftfartBaser = new ArrayList<>();

    public Bosted bosted = new Bosted();

    public Set<String> hentAlleOrganisasjonsnumre() {
        return Stream.of(selvstendigArbeid.hentAlleOrganisasjonsnumre(),
            juridiskArbeidsgiverNorge.hentManueltRegistrerteArbeidsgiverOrgnumre())
            .flatMap(i -> i)
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.toSet());
    }

    public Set<String> hentAllePersonnumre() {
        return personOpplysninger.hentAllePersonnummer()
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.toSet());
    }

    public List<String> hentUtenlandskeArbeidsstederLandkode() {
        return arbeidUtland.stream()
            .map(a -> a.adresse != null ? a.adresse.landkode : null)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
    }
}
