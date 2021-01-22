package no.nav.melosys.domain.behandlingsgrunnlag;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import no.nav.melosys.domain.behandlingsgrunnlag.data.*;
import org.apache.commons.lang3.StringUtils;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BehandlingsgrunnlagData {

    public Soeknadsland soeknadsland = new Soeknadsland();

    public Periode periode = new Periode();

    public OpplysningerOmBrukeren personOpplysninger = new OpplysningerOmBrukeren();

    public ArbeidPaaLand arbeidPaaLand = new ArbeidPaaLand();

    public List<FysiskArbeidssted> fysiskeArbeidsstederUtland = new ArrayList<>();

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

    public List<String> hentUtenlandskeArbeidsstederLandkode() {
        return fysiskeArbeidsstederUtland.stream()
            .map(a -> a.adresse != null ? a.adresse.landkode : null)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
    }

    public List<String> hentUtenlandskeArbeidsgivereUuid() {
        return foretakUtland.stream()
            .map(f -> f.uuid)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
    }

    public List<String> hentUtenlandskeArbeidsgivereLandkode() {
        return foretakUtland.stream()
            .map(f -> f.adresse != null ? f.adresse.landkode : null)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
    }

    public Set<String> hentFnrMedfølgendeBarn() {
        return personOpplysninger.medfolgendeFamilie.stream()
            .filter(MedfolgendeFamilie::erBarn)
            .map(MedfolgendeFamilie::getFnr)
            .collect(Collectors.toSet());
    }

    public Map<String, MedfolgendeFamilie.Relasjonsrolle> hentUuidOgRolleMedfølgendeFamilie() {
        return personOpplysninger.medfolgendeFamilie.stream()
            .collect(Collectors.toMap(MedfolgendeFamilie::getUuid, MedfolgendeFamilie::getRelasjonsrolle));
    }

    public Map<String, MedfolgendeFamilie> hentMedfølgendeBarn() {
        return personOpplysninger.medfolgendeFamilie.stream()
            .filter(MedfolgendeFamilie::erBarn)
            .collect(Collectors.toMap(MedfolgendeFamilie::getUuid, mf -> mf));
    }
}
