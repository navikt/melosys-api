package no.nav.melosys.melosysmock.pdl

import no.nav.melosys.generated.graphql.api.HentPersonQueryResolver
import no.nav.melosys.generated.graphql.api.PersonResolver
import no.nav.melosys.generated.graphql.model.*
import no.nav.melosys.melosysmock.person.Person
import no.nav.melosys.melosysmock.person.PersonRepo
import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope
import java.time.LocalDate
import java.time.LocalDateTime

@Component
@RequestScope
class HentPersonQuery(private val querySession: PDLQuerySession) : HentPersonQueryResolver {

    override fun hentPerson(ident: String): PersonDto? {
        querySession.setIdent(ident)
        return PersonRepo.finnVedIdent(ident)?.let {
            PersonDto(
                doedfoedtBarn = listOf(),
                doedsfall = listOf(),
                falskIdentitet = null,
                foedsel = listOf(lagFoedselDto(it)),
                forelderBarnRelasjon = lagForelderRelasjon(),
                sikkerhetstiltak = listOf(),
                telefonnummer = listOf(TelefonnummerDto("NOR", "123321", 0, metadata())),
                tilrettelagtKommunikasjon = listOf(),
                utflyttingFraNorge = listOf(),
                innflyttingTilNorge = listOf()
            )
        }
    }

    private fun lagFoedselDto(person: Person) = FoedselDto(
        foedselsaar = person.foedselsdato.year,
        foedselsdato = person.foedselsdato.toString(),
        foedeland = "NOR",
        foedested = "OSLO",
        foedekommune = null,
        folkeregistermetadata = null,
        metadata = metadata()
    )

    private fun lagForelderRelasjon() = listOf(
        ForelderBarnRelasjonDto(
            relatertPersonsIdent = "77777777777",
            relatertPersonsRolle = FamilierelasjonsrolleDto.BARN,
            minRolleForPerson = FamilierelasjonsrolleDto.FAR,
            folkeregistermetadata = null,
            metadata = metadata()
        ),
        ForelderBarnRelasjonDto(
            relatertPersonsIdent = "30056928150",
            relatertPersonsRolle = FamilierelasjonsrolleDto.FAR,
            minRolleForPerson = FamilierelasjonsrolleDto.BARN,
            folkeregistermetadata = null,
            metadata = metadata()
        ),
        ForelderBarnRelasjonDto(
            relatertPersonsIdent = "21075114491",
            relatertPersonsRolle = FamilierelasjonsrolleDto.MOR,
            minRolleForPerson = FamilierelasjonsrolleDto.BARN,
            folkeregistermetadata = null,
            metadata = metadata()
        )
    )
}

fun metadata(historisk: Boolean = false) = MetadataDto(
    opplysningsId = "123",
    master = "PDL",
    endringer = listOf(endring()),
    historisk = historisk
)

fun endring() = EndringDto(
    type = EndringstypeDto.OPPRETT,
    registrert = LocalDateTime.now().minusYears(10),
    registrertAv = "Hemmelig",
    systemkilde = "FREG",
    kilde = "Folkeregisteret"
)

fun folkeregisterMetadata(gyldighetstidspunkt: LocalDateTime = LocalDateTime.of(1990, 1, 1, 0, 0)) =
    FolkeregistermetadataDto(
        ajourholdstidspunkt = null,
        gyldighetstidspunkt = gyldighetstidspunkt,
        opphoerstidspunkt = null,
        kilde = "Folkeregisteret",
        aarsak = null,
        sekvens = null
    )

@Component
@RequestScope
class PersonResolverImpl(private val querySession: PDLQuerySession) : PersonResolver {
    override fun adressebeskyttelse(personDto: PersonDto, historikk: Boolean?): List<AdressebeskyttelseDto> {
        return listOf()
    }

    override fun bostedsadresse(personDto: PersonDto, historikk: Boolean?): List<BostedsadresseDto> {
        return querySession.hentIdent()
            ?.let { PersonRepo.finnVedIdent(it)?.bostedsadresse }
            ?.let {
                listOf(
                    BostedsadresseDto(
                        angittFlyttedato = null,
                        gyldigFraOgMed = LocalDateTime.now().minusYears(10),
                        gyldigTilOgMed = null,
                        coAdressenavn = "Co Yo",
                        vegadresse = VegadresseDto(
                            matrikkelId = "123",
                            husnummer = it.husnummer,
                            husbokstav = it.husbokstav,
                            bruksenhetsnummer = null,
                            adressenavn = it.gatenavn,
                            kommunenummer = null,
                            bydelsnummer = null,
                            tilleggsnavn = null,
                            postnummer = it.postnummer,
                            koordinater = null
                        ),
                        matrikkeladresse = null,
                        utenlandskAdresse = null,
                        ukjentBosted = null,
                        folkeregistermetadata = null,
                        metadata = metadata()
                    )
                )
            } ?: emptyList()
    }

    override fun deltBosted(personDto: PersonDto, historikk: Boolean?): List<DeltBostedDto> {
        return emptyList()
    }

    override fun folkeregisteridentifikator(
        personDto: PersonDto,
        historikk: Boolean?
    ): List<FolkeregisteridentifikatorDto> {
        return querySession.hentIdent()
            ?.let { PersonRepo.finnVedIdent(it) }
            ?.let {
                listOf(
                    FolkeregisteridentifikatorDto(
                        identifikasjonsnummer = it.ident,
                        status = "I_BRUK",
                        type = if (Character.getNumericValue(it.ident.toCharArray()[0]) < 4) "FNR" else "DNR",
                        folkeregistermetadata = folkeregisterMetadata(),
                        metadata = metadata()
                    )
                )
            } ?: emptyList()
    }

    override fun folkeregisterpersonstatus(
        personDto: PersonDto,
        historikk: Boolean?
    ): List<FolkeregisterpersonstatusDto> {
        return listOf(
            FolkeregisterpersonstatusDto(
                status = "bosatt",
                forenkletStatus = "bosattEtterFolkeregisterloven",
                folkeregistermetadata = folkeregisterMetadata(),
                metadata = metadata()
            ),
            FolkeregisterpersonstatusDto(
                status = "midlertidig",
                forenkletStatus = "dNummer",
                folkeregistermetadata = folkeregisterMetadata(
                    gyldighetstidspunkt = LocalDateTime.of(2020, 10, 22, 0, 0)
                ),
                metadata = metadata(historisk = true)
            ),
            FolkeregisterpersonstatusDto(
                status = "utflyttet",
                forenkletStatus = "ikkeBosatt",
                folkeregistermetadata = folkeregisterMetadata(
                    gyldighetstidspunkt = LocalDateTime.of(2012, 3, 15, 0, 0)
                ),
                metadata = metadata(historisk = true)
            )
        )
    }

    override fun foreldreansvar(personDto: PersonDto, historikk: Boolean?): List<ForeldreansvarDto> {
        return listOf(
            ForeldreansvarDto(
                ansvar = "felles",
                null,
                null,
                null,
                metadata()
            )
        )
    }

    override fun fullmakt(personDto: PersonDto, historikk: Boolean?): List<FullmaktDto> {
        return emptyList()
    }

    override fun identitetsgrunnlag(personDto: PersonDto, historikk: Boolean?): List<IdentitetsgrunnlagDto> {
        return emptyList()
    }

    override fun kjoenn(personDto: PersonDto, historikk: Boolean?): List<KjoennDto> {
        return emptyList()
    }

    override fun kontaktadresse(personDto: PersonDto, historikk: Boolean?): List<KontaktadresseDto> {
        return querySession.hentIdent()
            ?.let { PersonRepo.finnVedIdent(it) }
            ?.let {
                listOf(
                    KontaktadresseDto(
                        gyldigFraOgMed = LocalDateTime.now().minusYears(10),
                        gyldigTilOgMed = null,
                        coAdressenavn = null,
                        vegadresse = VegadresseDto(
                            matrikkelId = "456",
                            husnummer = "1",
                            husbokstav = "A",
                            bruksenhetsnummer = null,
                            adressenavn = "kontaktAdresse",
                            kommunenummer = null,
                            bydelsnummer = null,
                            tilleggsnavn = null,
                            postnummer = "0010",
                            koordinater = null
                        ),
                        postboksadresse = null,
                        postadresseIFrittFormat = null,
                        utenlandskAdresse = null,
                        utenlandskAdresseIFrittFormat = null,
                        type = KontaktadresseTypeDto.Innland,
                        folkeregistermetadata = null,
                        metadata = metadata()
                    )
                )
            } ?: emptyList()
    }

    override fun kontaktinformasjonForDoedsbo(
        personDto: PersonDto,
        historikk: Boolean?
    ): List<KontaktinformasjonForDoedsboDto> {
        return emptyList()
    }

    override fun navn(personDto: PersonDto, historikk: Boolean?): List<NavnDto> {
        return querySession.hentIdent()
            ?.let { PersonRepo.finnVedIdent(it) }
            ?.let {
                listOf(
                    NavnDto(
                        fornavn = it.fornavn,
                        mellomnavn = null,
                        etternavn = it.etternavn,
                        forkortetNavn = null,
                        originaltNavn = null,
                        gyldigFraOgMed = it.foedselsdato.atStartOfDay().toString(),
                        folkeregistermetadata = null,
                        metadata = metadata()
                    )
                )
            } ?: emptyList()
    }

    override fun opphold(personDto: PersonDto, historikk: Boolean?): List<OppholdDto> {
        return emptyList()
    }

    override fun oppholdsadresse(personDto: PersonDto, historikk: Boolean?): List<OppholdsadresseDto> {
        return querySession.hentIdent()
            ?.let { PersonRepo.finnVedIdent(it) }
            ?.let {
                listOf(
                    OppholdsadresseDto(
                        gyldigFraOgMed = LocalDateTime.now().minusYears(10),
                        gyldigTilOgMed = null,
                        coAdressenavn = null,
                        vegadresse = VegadresseDto(
                            matrikkelId = null,
                            husnummer = "3",
                            husbokstav = "C",
                            bruksenhetsnummer = null,
                            adressenavn = "opphold her",
                            kommunenummer = null,
                            bydelsnummer = null,
                            tilleggsnavn = null,
                            postnummer = "0030",
                            koordinater = null
                        ),
                        utenlandskAdresse = null,
                        folkeregistermetadata = null,
                        matrikkeladresse = null,
                        oppholdAnnetSted = null,
                        metadata = metadata()
                    )
                )
            } ?: emptyList()
    }

    override fun sivilstand(personDto: PersonDto, historikk: Boolean?): List<SivilstandDto> {
        return querySession.hentIdent()
            ?.let { PersonRepo.finnVedIdent(it)?.aktørId }
            ?.let { lagSivilstand(it) }
            ?: emptyList()
    }

    private fun lagSivilstand(aktørID: String): List<SivilstandDto> {
        when (aktørID) {
            "1111111111111" -> return listOf(
                SivilstandDto(
                    type = SivilstandstypeDto.GIFT,
                    relatertVedSivilstand = "21075114491",
                    gyldigFraOgMed = LocalDate.of(1999, 2, 3).toString(),
                    bekreftelsesdato = null,
                    folkeregistermetadata = null,
                    metadata = metadata()
                )
            )
            "2222222222222" -> return listOf(
                SivilstandDto(
                    type = SivilstandstypeDto.GIFT,
                    relatertVedSivilstand = "30056928150",
                    gyldigFraOgMed = LocalDate.of(1999, 2, 3).toString(),
                    bekreftelsesdato = null,
                    folkeregistermetadata = null,
                    metadata = metadata()
                )
            )
            else -> return listOf()
        }
    }

    override fun statsborgerskap(personDto: PersonDto, historikk: Boolean?): List<StatsborgerskapDto> {
        return querySession.hentIdent()
            ?.let { PersonRepo.finnVedIdent(it)?.statsborgerskap }
            ?.map {
                StatsborgerskapDto(
                    land = it,
                    bekreftelsesdato = null,
                    gyldigFraOgMed = LocalDate.of(1990, 1, 1).toString(),
                    gyldigTilOgMed = null,
                    folkeregistermetadata = null,
                    metadata = metadata()
                )
            } ?: emptyList()
    }

    override fun utenlandskIdentifikasjonsnummer(
        personDto: PersonDto,
        historikk: Boolean?
    ): List<UtenlandskIdentifikasjonsnummerDto> {
        return emptyList()
    }

    override fun vergemaalEllerFremtidsfullmakt(
        personDto: PersonDto,
        historikk: Boolean?
    ): List<VergemaalEllerFremtidsfullmaktDto> {
        return emptyList()
    }

}
