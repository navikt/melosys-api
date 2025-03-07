package no.nav.melosys.melosysmock.pdl

import no.nav.melosys.generated.graphql.api.HentPersonQueryResolver
import no.nav.melosys.generated.graphql.api.PersonResolver
import no.nav.melosys.generated.graphql.model.*
import no.nav.melosys.melosysmock.person.Adresse
import no.nav.melosys.melosysmock.person.Person
import no.nav.melosys.melosysmock.person.PersonRepo
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class HentPersonQuery(private val querySession: PDLQuerySession) : HentPersonQueryResolver {

    override fun hentPerson(ident: String): PersonDto? {
        querySession.setIdent(ident)
        return PersonRepo.finnVedIdent(ident)?.let {
            PersonDto(
                doedfoedtBarn = listOf(),
                doedsfall = listOf(),
                falskIdentitet = null,
                foedselsdato = listOf(lagFoedselDto(it)),
                forelderBarnRelasjon = lagForelderRelasjon(),
                sikkerhetstiltak = listOf(),
                telefonnummer = listOf(TelefonnummerDto("NOR", "123321", 0, metadata())),
                tilrettelagtKommunikasjon = listOf(),
                utflyttingFraNorge = listOf(),
                innflyttingTilNorge = listOf()
            )
        }
    }

    fun lagFoedselDto(person: Person) = FoedselsdatoDto(
        foedselsaar = person.foedselsdato.year,
        foedselsdato = person.foedselsdato.toString(),
        foedekommune = null,
        folkeregistermetadata = null,
        metadata = metadata()
    )

    fun lagForelderRelasjon() = listOf(
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
    master = "Freg",
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
                        coAdressenavn = "Co Yo 1",
                        matrikkeladresse = null,
                        vegadresse = if (it.landkode == "NOR") lagVegadresse(it) else null,
                        utenlandskAdresse = if (it.landkode != "NOR") lagUtenlandskAdresse(it) else null,
                        ukjentBosted = null,
                        folkeregistermetadata = null,
                        metadata = metadata()
                    )
                )
            } ?: emptyList()

    }

    private fun lagUtenlandskAdresse(it: Adresse) =
        UtenlandskAdresseDto.builder()
            .setLandkode(it.landkode)
            .setAdressenavnNummer(it.gatenavn)
            .setPostkode(it.postnummer)
            .setBygningEtasjeLeilighet(it.husnummer)
            .build()

    private fun lagVegadresse(it: Adresse) =
        VegadresseDto.builder()
            .setHusnummer(it.husnummer)
            .setHusbokstav(it.husbokstav)
            .setAdressenavn(it.gatenavn)
            .setPostnummer(it.postnummer)
            .build()

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
            ?.let { PersonRepo.finnVedIdent(it)?.kontaktadresse }
            ?.let {
                listOf(
                    KontaktadresseDto(
                        gyldigFraOgMed = LocalDateTime.now().minusYears(10),
                        gyldigTilOgMed = null,
                        coAdressenavn = "CO 2",
                        vegadresse = if (it.landkode == "NOR") lagVegadresse(it) else null,
                        utenlandskAdresse = if (it.landkode != "NOR") lagUtenlandskAdresse(it) else null,
                        postboksadresse = null,
                        postadresseIFrittFormat = null,
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
            ?.let { PersonRepo.finnVedIdent(it)?.oppholdsadresse }
            ?.let {
                listOf(
                    OppholdsadresseDto(
                        gyldigFraOgMed = LocalDateTime.now().minusYears(10),
                        gyldigTilOgMed = null,
                        coAdressenavn = "CO 3",
                        vegadresse = if (it.landkode == "NOR") lagVegadresse(it) else null,
                        utenlandskAdresse = if (it.landkode != "NOR") lagUtenlandskAdresse(it) else null,
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
                    gyldigFraOgMed = LocalDate.of(2019, 8, 3).toString(),
                    relatertVedSivilstand = "21075114491",
                    bekreftelsesdato = null,
                    folkeregistermetadata = null,
                    metadata = MetadataDto(
                        endringer = listOf(
                            EndringDto(
                                type = EndringstypeDto.OPPRETT,
                                registrert = toLocalDateTime("2020-01-25T17:55:42"),
                                registrertAv = "Folkeregisteret",
                                systemkilde = "FREG",
                                kilde = "FREG",
                            ),
                            EndringDto(
                                type = EndringstypeDto.KORRIGER,
                                registrert = toLocalDateTime("2020-12-05T08:32:21"),
                                registrertAv = "Folkeregisteret",
                                systemkilde = "FREG",
                                kilde = "FREG",
                            )
                        ),
                        master = "Freg",
                        opplysningsId = "4d25bb7f-202c-48da-ace1-764a95cd8e80",
                        historisk = false
                    )
                ),

                SivilstandDto(
                    type = SivilstandstypeDto.GIFT,
                    gyldigFraOgMed = LocalDate.of(2011, 2, 2).toString(),
                    relatertVedSivilstand = "12028536819",
                    bekreftelsesdato = null,
                    folkeregistermetadata = null,
                    metadata = MetadataDto(
                        endringer = listOf(
                            EndringDto(
                                type = EndringstypeDto.OPPRETT,
                                registrert = toLocalDateTime("2020-01-25T17:55:42"),
                                registrertAv = "Folkeregisteret",
                                systemkilde = "FREG",
                                kilde = "KILDE_DSF",
                            ),
                            EndringDto(
                                type = EndringstypeDto.KORRIGER,
                                registrert = toLocalDateTime("2022-04-27T14:57:56"),
                                registrertAv = "Folkeregisteret",
                                systemkilde = "FREG",
                                kilde = "KILDE_DSF",
                            )
                        ),
                        master = "Freg",
                        opplysningsId = "4d25bb7f-202c-48da-ace1-764a95cd8e3d",
                        historisk = false
                    )
                ),

                SivilstandDto(
                    type = SivilstandstypeDto.SEPARERT,
                    gyldigFraOgMed = LocalDate.of(2013, 1, 3).toString(),
                    relatertVedSivilstand = "12028536819",
                    bekreftelsesdato = null,
                    folkeregistermetadata = null,
                    metadata = MetadataDto(
                        endringer = listOf(
                            EndringDto(
                                type = EndringstypeDto.OPPRETT,
                                registrert = toLocalDateTime("2020-01-25T17:55:42"),
                                registrertAv = "Folkeregisteret",
                                systemkilde = "FREG",
                                kilde = "KILDE_DSF",
                            ),
                            EndringDto(
                                type = EndringstypeDto.KORRIGER,
                                registrert = toLocalDateTime("2022-04-28T13:28:43"),
                                registrertAv = "Folkeregisteret",
                                systemkilde = "pdl-web",
                                kilde = "Tyske trygdemyndigheter, TYSKLAND",
                            )
                        ),
                        master = "PDL",
                        opplysningsId = "4d25bb7f-202c-48da-ace1-764a95cd8e3f",
                        historisk = true
                    )
                ),

                SivilstandDto(
                    type = SivilstandstypeDto.SKILT,
                    gyldigFraOgMed = LocalDate.of(2014, 4, 9).toString(),
                    relatertVedSivilstand = null, // SKILT har ikke verdi her
                    bekreftelsesdato = null,
                    folkeregistermetadata = null,
                    metadata = MetadataDto(
                        endringer = listOf(
                            EndringDto(
                                type = EndringstypeDto.OPPRETT,
                                registrert = toLocalDateTime("2020-01-25T17:55:42"),
                                registrertAv = "Folkeregisteret",
                                systemkilde = "FREG",
                                kilde = "KILDE_DSF",
                            ),
                            EndringDto(
                                type = EndringstypeDto.KORRIGER,
                                registrert = toLocalDateTime("2022-05-04T12:20:45"),
                                registrertAv = "Folkeregisteret",
                                systemkilde = "pdl-web",
                                kilde = "Tyske trygdemyndigheter, TYSKLAND",
                            )
                        ),
                        master = "PDL",
                        opplysningsId = "4d25bb7f-202c-48da-ace1-764a95cd8e3e",
                        historisk = true
                    )
                ),
            )
            "2222222222222"
            -> return listOf(
                SivilstandDto(
                    type = SivilstandstypeDto.GIFT,
                    gyldigFraOgMed = LocalDate.of(2019, 8, 3).toString(),
                    relatertVedSivilstand = "30056928150",
                    bekreftelsesdato = null,
                    folkeregistermetadata = null,
                    metadata = MetadataDto(
                        endringer = listOf(
                            EndringDto(
                                type = EndringstypeDto.OPPRETT,
                                registrert = toLocalDateTime("2020-01-25T17:55:42"),
                                registrertAv = "Folkeregisteret",
                                systemkilde = "FREG",
                                kilde = "FREG",
                            ),
                            EndringDto(
                                type = EndringstypeDto.KORRIGER,
                                registrert = toLocalDateTime("2020-12-05T08:32:21"),
                                registrertAv = "Folkeregisteret",
                                systemkilde = "FREG",
                                kilde = "FREG",
                            )
                        ),
                        master = "Freg",
                        opplysningsId = "4d25bb7f-202c-48da-ace1-764a95cd8e80",
                        historisk = false
                    )
                ),
            )
            "4444444444444" -> return listOf(

                SivilstandDto(
                    type = SivilstandstypeDto.GIFT,
                    gyldigFraOgMed = LocalDate.of(2011, 2, 2).toString(),
                    relatertVedSivilstand = "30056928150",
                    bekreftelsesdato = null,
                    folkeregistermetadata = null,
                    metadata = MetadataDto(
                        endringer = listOf(
                            EndringDto(
                                type = EndringstypeDto.OPPRETT,
                                registrert = toLocalDateTime("2020-01-25T17:55:42"),
                                registrertAv = "Folkeregisteret",
                                systemkilde = "FREG",
                                kilde = "KILDE_DSF",
                            ),
                            EndringDto(
                                type = EndringstypeDto.KORRIGER,
                                registrert = toLocalDateTime("2022-04-27T14:57:56"),
                                registrertAv = "Folkeregisteret",
                                systemkilde = "FREG",
                                kilde = "KILDE_DSF",
                            )
                        ),
                        master = "Freg",
                        opplysningsId = "4d25bb7f-202c-48da-ace1-764a95cd8e3d",
                        historisk = false
                    )
                ),

                SivilstandDto(
                    type = SivilstandstypeDto.SEPARERT,
                    gyldigFraOgMed = LocalDate.of(2013, 1, 3).toString(),
                    relatertVedSivilstand = "30056928150",
                    bekreftelsesdato = null,
                    folkeregistermetadata = null,
                    metadata = MetadataDto(
                        endringer = listOf(
                            EndringDto(
                                type = EndringstypeDto.OPPRETT,
                                registrert = toLocalDateTime("2020-01-25T17:55:42"),
                                registrertAv = "Folkeregisteret",
                                systemkilde = "FREG",
                                kilde = "KILDE_DSF",
                            ),
                            EndringDto(
                                type = EndringstypeDto.KORRIGER,
                                registrert = toLocalDateTime("2022-04-28T13:28:43"),
                                registrertAv = "Folkeregisteret",
                                systemkilde = "pdl-web",
                                kilde = "Tyske trygdemyndigheter, TYSKLAND",
                            )
                        ),
                        master = "PDL",
                        opplysningsId = "4d25bb7f-202c-48da-ace1-764a95cd8e3f",
                        historisk = true
                    )
                ),
                SivilstandDto(
                    type = SivilstandstypeDto.SKILT,
                    gyldigFraOgMed = LocalDate.of(2014, 4, 9).toString(),
                    relatertVedSivilstand = null, // SKILT har ikke verdi her
                    bekreftelsesdato = null,
                    folkeregistermetadata = null,
                    metadata = MetadataDto(
                        endringer = listOf(
                            EndringDto(
                                type = EndringstypeDto.OPPRETT,
                                registrert = toLocalDateTime("2020-01-25T17:55:42"),
                                registrertAv = "Folkeregisteret",
                                systemkilde = "FREG",
                                kilde = "KILDE_DSF",
                            ),
                            EndringDto(
                                type = EndringstypeDto.KORRIGER,
                                registrert = toLocalDateTime("2022-05-04T12:20:45"),
                                registrertAv = "Folkeregisteret",
                                systemkilde = "pdl-web",
                                kilde = "Tyske trygdemyndigheter, TYSKLAND",
                            )
                        ),
                        master = "PDL",
                        opplysningsId = "4d25bb7f-202c-48da-ace1-764a95cd8e3e",
                        historisk = true
                    )
                )
            )
            else -> return listOf()
        }
    }

    private fun toLocalDateTime(localDateTime: String) =
        LocalDateTime.parse(localDateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME)

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
