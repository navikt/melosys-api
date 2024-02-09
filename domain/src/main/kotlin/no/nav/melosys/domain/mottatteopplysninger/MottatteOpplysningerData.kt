package no.nav.melosys.domain.mottatteopplysninger

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.melosys.domain.mottatteopplysninger.data.*
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.ArbeidPaaLand
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.LuftfartBase
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.MaritimtArbeid
import org.apache.commons.lang3.StringUtils
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream


@JsonIgnoreProperties(ignoreUnknown = true)
open class MottatteOpplysningerData {
    var soeknadsland = Soeknadsland()
    var periode = Periode()
    var personOpplysninger = OpplysningerOmBrukeren()
    var arbeidPaaLand = ArbeidPaaLand()

    // Opplysninger om foretak i utlandet
    var foretakUtland: List<ForetakUtland> = ArrayList()

    // Opplysninger om opphold i utland
    var oppholdUtland = OppholdUtland()
    var selvstendigArbeid = SelvstendigArbeid()

    // Opplysninger om juridiske arbeidsgiver i Norge
    var juridiskArbeidsgiverNorge = JuridiskArbeidsgiverNorge()
    var maritimtArbeid: List<MaritimtArbeid> = ArrayList()
    var luftfartBaser: List<LuftfartBase> = ArrayList()
    var bosted = Bosted()
    fun hentAlleOrganisasjonsnumre(): Set<String?> {
        return Stream
            .of(
                selvstendigArbeid.hentAlleOrganisasjonsnumre(),
                juridiskArbeidsgiverNorge.hentManueltRegistrerteArbeidsgiverOrgnumre()
            )
            .flatMap { i: Stream<String?>? -> i }
            .filter { cs: String? -> StringUtils.isNotEmpty(cs) }
            .collect(Collectors.toSet())
    }

    fun hentUtenlandskeArbeidsstederLandkode(): List<String?> {
        return arbeidPaaLand.fysiskeArbeidssteder.stream()
            .map { a: FysiskArbeidssted -> if (a.adresse != null) a.adresse.landkode else null }
            .filter { obj: String? -> Objects.nonNull(obj) }
            .distinct()
            .collect(Collectors.toList())
    }

    fun hentUtenlandskeArbeidsgivereUuid(): List<String> {
        return foretakUtland.stream()
            .map { f: ForetakUtland -> f.uuid }
            .filter { obj: String? -> Objects.nonNull(obj) }
            .distinct()
            .collect(Collectors.toList())
    }

    fun hentUtenlandskeArbeidsgivereLandkode(): List<String?> {
        return foretakUtland.stream()
            .map { f: ForetakUtland -> if (f.adresse != null) f.adresse.landkode else null }
            .filter { obj: String? -> Objects.nonNull(obj) }
            .distinct()
            .collect(Collectors.toList())
    }

    fun hentUtenlandskeArbeidsgivereUuidOgNavn(): Map<String, String> {
        return foretakUtland.stream()
            .filter { f: ForetakUtland ->
                Objects.nonNull(
                    f.uuid
                ) && Objects.nonNull(f.navn)
            }
            .collect(
                Collectors.toMap(
                    { f: ForetakUtland -> f.uuid },
                    { f: ForetakUtland -> f.navn })
            )
    }

    fun hentFnrMedfølgendeBarn(): Set<String> {
        return personOpplysninger.medfolgendeFamilie.stream()
            .filter { obj: MedfolgendeFamilie -> obj.erBarn() }
            .map { obj: MedfolgendeFamilie -> obj.fnr }
            .collect(Collectors.toSet())
    }

    fun hentUuidOgRolleMedfølgendeFamilie(): Map<String, MedfolgendeFamilie.Relasjonsrolle> {
        return personOpplysninger.medfolgendeFamilie.stream()
            .collect(
                Collectors.toMap(
                    { obj: MedfolgendeFamilie -> obj.uuid },
                    { obj: MedfolgendeFamilie -> obj.relasjonsrolle })
            )
    }

    fun hentMedfølgendeBarn(): Map<String, MedfolgendeFamilie?> {
        return personOpplysninger.medfolgendeFamilie.stream()
            .filter { obj: MedfolgendeFamilie -> obj.erBarn() }
            .collect(
                Collectors.toMap(
                    { obj: MedfolgendeFamilie -> obj.uuid },
                    { mf: MedfolgendeFamilie? -> mf })
            )
    }

    fun hentMedfølgendeEktefelle(): Map<String, MedfolgendeFamilie?> {
        return personOpplysninger.medfolgendeFamilie.stream()
            .filter { obj: MedfolgendeFamilie -> obj.erEktefelleSamboer() }
            .collect(
                Collectors.toMap(
                    { obj: MedfolgendeFamilie -> obj.uuid },
                    { mf: MedfolgendeFamilie? -> mf })
            )
    }
}

