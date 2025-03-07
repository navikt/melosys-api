package no.nav.melosys.tjenester.gui.graphql

import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import no.nav.melosys.domain.person.Folkeregisterpersonstatus
import no.nav.melosys.domain.person.PersonMedHistorikk
import no.nav.melosys.domain.person.Sivilstand
import no.nav.melosys.domain.person.Statsborgerskap
import no.nav.melosys.domain.person.adresse.Bostedsadresse
import no.nav.melosys.domain.person.adresse.Kontaktadresse
import no.nav.melosys.domain.person.adresse.Oppholdsadresse
import no.nav.melosys.service.kodeverk.KodeverkService
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.tjenester.gui.graphql.dto.FolkeregisterpersonstatusDto
import no.nav.melosys.tjenester.gui.graphql.dto.PersonopplysningerDto
import no.nav.melosys.tjenester.gui.graphql.dto.SivilstandDto
import no.nav.melosys.tjenester.gui.graphql.dto.StatsborgerskapDto
import no.nav.melosys.tjenester.gui.graphql.mapping.*
import no.nav.melosys.tjenester.gui.graphql.mapping.BostedsadresseTilDtoKonverter.tilDto
import no.nav.melosys.tjenester.gui.graphql.mapping.KontaktadresseTilDtoKonverter.tilDto
import no.nav.melosys.tjenester.gui.graphql.mapping.OppholdsadresseTilDtoKonverter.tilDto
import org.apache.commons.lang3.StringUtils
import org.springframework.stereotype.Component
import java.util.*


@Component
class PersonopplysningerDataFetcher(private val kodeverkService: KodeverkService, private val persondataFasade: PersondataFasade) :
    DataFetcher<PersonopplysningerDto> {
    @Throws(Exception::class)
    override fun get(fetchingEnvironment: DataFetchingEnvironment): PersonopplysningerDto {
        val personMedHistorikk: PersonMedHistorikk
        val ident = fetchingEnvironment.getArgument<String>("ident")
        personMedHistorikk = if (!StringUtils.isEmpty(ident)) {
            persondataFasade.hentPersonMedHistorikk(ident)
        } else {
            val behandlingID = fetchingEnvironment.executionStepInfo.parent.getArgument<Long>("behandlingID")
            persondataFasade.hentPersonMedHistorikk(behandlingID)
        }
        Objects.requireNonNull(personMedHistorikk)
        val bostedsadresseDtoList = personMedHistorikk.bostedsadresser.stream()
            .map { bostedsadresse: Bostedsadresse? ->
                tilDto(
                    bostedsadresse!!,
                    kodeverkService
                )
            }.toList()
        val folkeregisterpersonstatusDtoList = personMedHistorikk.folkeregisterpersonstatuser.stream()
            .map { folkeregisterpersonstatus: Folkeregisterpersonstatus? ->
                FolkeregisterpersonstatusTilDtoKonverter.tilDto(
                    folkeregisterpersonstatus
                )
            }
            .filter { obj: FolkeregisterpersonstatusDto? ->
                Objects.nonNull(
                    obj
                )
            }.toList()
        val kontaktadresseDtoList = personMedHistorikk.kontaktadresser.stream()
            .map { kontaktadresse: Kontaktadresse? ->
                tilDto(
                    kontaktadresse!!,
                    kodeverkService
                )
            }.toList()
        val oppholdsadresseDtoList = personMedHistorikk.oppholdsadresser.stream()
            .map { oppholdsadresse: Oppholdsadresse? ->
                tilDto(
                    oppholdsadresse!!,
                    kodeverkService
                )
            }.toList()
        val sivilstandDtoList = personMedHistorikk.sivilstand.stream()
            .map { sivilstand: Sivilstand? ->
                SivilstandTilDtoKonverter.tilDto(
                    sivilstand
                )
            }
            .sorted(
                Comparator.comparing(
                    SivilstandDto::gyldigFraOgMed,
                    Comparator.nullsFirst(Comparator.reverseOrder())
                )
            )
            .toList()
        val statsborgerskapDtoList = personMedHistorikk.statsborgerskap.stream()
            .map { s: Statsborgerskap? ->
                StatsborgerskapTilDtoKonverter.tilDto(
                    s,
                    kodeverkService
                )
            }
            .sorted(
                Comparator.comparing(
                    StatsborgerskapDto::gyldigFraOgMed,
                    Comparator.nullsFirst(Comparator.reverseOrder())
                )
            )
            .toList()
        return PersonopplysningerDto(
            bostedsadresseDtoList,
            FoedselsdatoTilDtoKonverter.tilDto(personMedHistorikk.fødselsdato),
            FolkeregisteridentifikatorTilDtoKonverter.tilDto(personMedHistorikk.folkeregisteridentifikator),
            folkeregisterpersonstatusDtoList,
            personMedHistorikk.kjønn,
            kontaktadresseDtoList,
            NavnTilDtoKonverter.tilDto(personMedHistorikk.navn),
            oppholdsadresseDtoList,
            sivilstandDtoList,
            statsborgerskapDtoList
        )
    }
}

