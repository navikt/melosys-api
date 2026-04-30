package no.nav.melosys.saksflyt.steg.soknad

import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.kodeverk.Flyvningstyper
import no.nav.melosys.domain.kodeverk.Innretningstyper
import no.nav.melosys.domain.kodeverk.begrunnelser.Fartsomrader
import no.nav.melosys.domain.mottatteopplysninger.Soeknad
import no.nav.melosys.domain.mottatteopplysninger.data.ForetakUtland
import no.nav.melosys.domain.mottatteopplysninger.data.JuridiskArbeidsgiverNorge
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.ArbeidPaaLand
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.LuftfartBase
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.MaritimtArbeid
import no.nav.melosys.skjema.types.felles.Ansettelsesform
import no.nav.melosys.skjema.types.felles.LandKode
import no.nav.melosys.skjema.types.felles.PeriodeDto
import no.nav.melosys.skjema.types.felles.UtenlandskVirksomhetBase
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerSkjemaM2MDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.*
import java.util.*

/**
 * Mapper digital søknadsdata til [Soeknad] for pre-utfylling av sidemeny i Melosys.
 *
 * Dekker seksjonene: Periode og land, Arbeidsgiver/virksomhet, Arbeidssted(er).
 * Fullmektig håndteres ikke her — se MELOSYS-8031 (aktør på Fagsak, ikke felt på Soeknad).
 *
 * Regel ved begge deler: arbeidstakers del har presedens over arbeidsgivers del.
 * Full skjemadata lagres som original_data (JSON) i skjema_sak_mapping.
 */
object DigitalSøknadMapper {

    fun tilSoeknad(dto: UtsendtArbeidstakerSkjemaM2MDto): Soeknad = Soeknad().also { søknad ->
        val arbeidstakersDel = hentArbeidstakersData(dto)
        val arbeidsgiversDel = hentArbeidsgiversData(dto)

        // Periode og land: AT vinner, AG fallback
        val periodeOgLand = arbeidstakersDel?.utsendingsperiodeOgLand ?: arbeidsgiversDel?.utsendingsperiodeOgLand
        søknad.periode = mapPeriode(periodeOgLand?.utsendelsePeriode)
        søknad.soeknadsland = mapSoeknadsland(periodeOgLand?.utsendelseLand)

        // Arbeidssted kommer kun fra arbeidsgivers del
        mapArbeidssteder(søknad, arbeidsgiversDel?.arbeidsstedIUtlandet)

        // Norsk arbeidsgiver (hovedarbeidsgivers orgnr: AT vinner, AG fallback)
        søknad.juridiskArbeidsgiverNorge = mapJuridiskArbeidsgiverNorge(dto, arbeidsgiversDel?.arbeidsgiverensVirksomhetINorge)

        // Utenlandske virksomheter ("Arbeidsgiver i utlandet") pre-utfylles fra både arbeidsgivers
        // lønnsliste og arbeidstakers virksomhetsliste. Identiske oppføringer dedupliseres.
        søknad.foretakUtland = mapUtenlandskeVirksomheter(
            arbeidsgiversDel?.arbeidstakerensLonn,
            arbeidstakersDel?.arbeidssituasjon
        )
    }

    private fun hentArbeidstakersData(dto: UtsendtArbeidstakerSkjemaM2MDto): ArbeidstakersDataLook? {
        return when (val data = dto.skjema.data) {
            is UtsendtArbeidstakerArbeidstakersSkjemaDataDto -> ArbeidstakersDataLook(
                utsendingsperiodeOgLand = data.utsendingsperiodeOgLand,
                arbeidssituasjon = data.arbeidssituasjon
            )

            is UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto -> ArbeidstakersDataLook(
                utsendingsperiodeOgLand = data.utsendingsperiodeOgLand,
                arbeidssituasjon = data.arbeidstakersData.arbeidssituasjon
            )

            else -> (dto.kobletSkjema?.data as? UtsendtArbeidstakerArbeidstakersSkjemaDataDto)?.let {
                ArbeidstakersDataLook(
                    utsendingsperiodeOgLand = it.utsendingsperiodeOgLand,
                    arbeidssituasjon = it.arbeidssituasjon
                )
            }
        }
    }

    private fun hentArbeidsgiversData(dto: UtsendtArbeidstakerSkjemaM2MDto): ArbeidsgiversDataLook? {
        return when (val data = dto.skjema.data) {
            is UtsendtArbeidstakerArbeidsgiversSkjemaDataDto -> ArbeidsgiversDataLook(
                utsendingsperiodeOgLand = data.utsendingsperiodeOgLand,
                arbeidsgiverensVirksomhetINorge = data.arbeidsgiverensVirksomhetINorge,
                arbeidstakerensLonn = data.arbeidstakerensLonn,
                arbeidsstedIUtlandet = data.arbeidsstedIUtlandet
            )

            is UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto -> ArbeidsgiversDataLook(
                utsendingsperiodeOgLand = data.utsendingsperiodeOgLand,
                arbeidsgiverensVirksomhetINorge = data.arbeidsgiversData.arbeidsgiverensVirksomhetINorge,
                arbeidstakerensLonn = data.arbeidsgiversData.arbeidstakerensLonn,
                arbeidsstedIUtlandet = data.arbeidsgiversData.arbeidsstedIUtlandet
            )

            else -> (dto.kobletSkjema?.data as? UtsendtArbeidstakerArbeidsgiversSkjemaDataDto)?.let {
                ArbeidsgiversDataLook(
                    utsendingsperiodeOgLand = it.utsendingsperiodeOgLand,
                    arbeidsgiverensVirksomhetINorge = it.arbeidsgiverensVirksomhetINorge,
                    arbeidstakerensLonn = it.arbeidstakerensLonn,
                    arbeidsstedIUtlandet = it.arbeidsstedIUtlandet
                )
            }
        }
    }

    private data class ArbeidstakersDataLook(
        val utsendingsperiodeOgLand: UtsendingsperiodeOgLandDto?,
        val arbeidssituasjon: ArbeidssituasjonDto?
    )

    private data class ArbeidsgiversDataLook(
        val utsendingsperiodeOgLand: UtsendingsperiodeOgLandDto?,
        val arbeidsgiverensVirksomhetINorge: ArbeidsgiverensVirksomhetINorgeDto?,
        val arbeidstakerensLonn: ArbeidstakerensLonnDto?,
        val arbeidsstedIUtlandet: ArbeidsstedIUtlandetDto?
    )

    private fun mapJuridiskArbeidsgiverNorge(
        dto: UtsendtArbeidstakerSkjemaM2MDto,
        virksomhetINorge: ArbeidsgiverensVirksomhetINorgeDto?
    ): JuridiskArbeidsgiverNorge = JuridiskArbeidsgiverNorge().apply {
        erOffentligVirksomhet = virksomhetINorge?.erArbeidsgiverenOffentligVirksomhet
        ekstraArbeidsgivere = listOfNotNull(hentHovedarbeidsgiversOrgnr(dto))
    }

    private fun hentHovedarbeidsgiversOrgnr(dto: UtsendtArbeidstakerSkjemaM2MDto): String? {
        val hovedskjemaOrgnr = dto.skjema.orgnr.takeIf { it.isNotBlank() }
        return when (dto.skjema.metadata.skjemadel) {
            Skjemadel.ARBEIDSTAKERS_DEL, Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL -> hovedskjemaOrgnr
            Skjemadel.ARBEIDSGIVERS_DEL -> {
                val kobletAtOrgnr = dto.kobletSkjema?.takeIf {
                    it.metadata.skjemadel == Skjemadel.ARBEIDSTAKERS_DEL
                }?.orgnr?.takeIf { it.isNotBlank() }
                kobletAtOrgnr ?: hovedskjemaOrgnr
            }
        }
    }

    private fun mapUtenlandskeVirksomheter(
        agLonn: ArbeidstakerensLonnDto?,
        atArbeidssituasjon: ArbeidssituasjonDto?
    ): List<ForetakUtland> {
        val agUtenlandske = agLonn?.virksomheterSomUtbetalerLonnOgNaturalytelser
            ?.utenlandskeVirksomheter.orEmpty()
        val atUtenlandske = atArbeidssituasjon?.virksomheterArbeidstakerJobberForIutsendelsesPeriode
            ?.utenlandskeVirksomheter.orEmpty()

        // AT-versjonen tas først (har ansettelsesform-info). AG-versjon legges kun til hvis ikke
        // identisk på alle felles base-felter.
        val atKeys = atUtenlandske.map(::dedupNokkel).toSet()
        val agUnike = agUtenlandske.filterNot { dedupNokkel(it) in atKeys }

        return atUtenlandske.map { mapForetakUtland(it, selvstendig = it.ansettelsesform == Ansettelsesform.SELVSTENDIG_NAERINGSDRIVENDE) } +
            agUnike.map { mapForetakUtland(it) }
    }

    private fun dedupNokkel(v: UtenlandskVirksomhetBase): List<Any?> = listOf(
        v.navn, v.organisasjonsnummer, v.vegnavnOgHusnummer, v.bygning,
        v.postkode, v.byStedsnavn, v.region, v.land, v.tilhorerSammeKonsern
    )

    private fun mapForetakUtland(v: UtenlandskVirksomhetBase, selvstendig: Boolean = false): ForetakUtland =
        ForetakUtland().apply {
            uuid = UUID.randomUUID().toString()
            navn = v.navn
            orgnr = v.organisasjonsnummer
            tilhorerSammeKonsern = v.tilhorerSammeKonsern
            selvstendigNæringsvirksomhet = selvstendig
            adresse = StrukturertAdresse(
                tilleggsnavn = v.bygning,
                gatenavn = v.vegnavnOgHusnummer,
                husnummerEtasjeLeilighet = null,
                postboks = null,
                postnummer = v.postkode,
                poststed = v.byStedsnavn,
                region = v.region,
                landkode = v.land
            )
        }

    private fun mapArbeidssteder(søknad: Soeknad, arbeidssted: ArbeidsstedIUtlandetDto?) {
        if (arbeidssted == null) return
        when (arbeidssted.arbeidsstedType) {
            ArbeidsstedType.PA_LAND -> søknad.arbeidPaaLand = mapArbeidPaaLand(arbeidssted)
            ArbeidsstedType.OFFSHORE -> søknad.maritimtArbeid = mapOffshore(arbeidssted)
            ArbeidsstedType.PA_SKIP -> søknad.maritimtArbeid = mapPaaSkip(arbeidssted)
            ArbeidsstedType.OM_BORD_PA_FLY -> søknad.luftfartBaser = mapLuftfart(arbeidssted)
        }
    }

    private fun mapArbeidPaaLand(arbeidssted: ArbeidsstedIUtlandetDto): ArbeidPaaLand {
        val paLand = arbeidssted.paLand ?: return ArbeidPaaLand()
        return ArbeidPaaLand().apply {
            erHjemmekontor = paLand.erHjemmekontor
            erFastArbeidssted = paLand.fastEllerVekslendeArbeidssted == FastEllerVekslendeArbeidssted.FAST

            val adresse = paLand.fastArbeidssted?.let {
                StrukturertAdresse(
                    gatenavn = it.vegadresse,
                    husnummerEtasjeLeilighet = it.nummer,
                    postnummer = it.postkode,
                    poststed = it.bySted,
                    region = null,
                    landkode = null
                )
            } ?: StrukturertAdresse()

            fysiskeArbeidssteder = listOf(
                FysiskArbeidssted(virksomhetNavn = paLand.navnPaVirksomhet, adresse = adresse)
            )
        }
    }

    private fun mapOffshore(arbeidssted: ArbeidsstedIUtlandetDto): List<MaritimtArbeid> {
        val offshore = arbeidssted.offshore ?: return emptyList()
        return listOf(
            MaritimtArbeid().apply {
                enhetNavn = offshore.navnPaInnretning
                innretningstype = mapTypeInnretning(offshore.typeInnretning)
                innretningLandkode = offshore.sokkelLand.name
            }
        )
    }

    private fun mapTypeInnretning(type: TypeInnretning): Innretningstyper = when (type) {
        TypeInnretning.BORESKIP_ELLER_ANNEN_FLYTTBAR_INNRETNING -> Innretningstyper.BORESKIP
        TypeInnretning.PLATTFORM_ELLER_ANNEN_FAST_INNRETNING -> Innretningstyper.PLATTFORM
    }

    private fun mapPaaSkip(arbeidssted: ArbeidsstedIUtlandetDto): List<MaritimtArbeid> {
        val paSkip = arbeidssted.paSkip ?: return emptyList()
        return listOf(
            MaritimtArbeid().apply {
                enhetNavn = paSkip.navnPaSkip
                fartsomradeKode = mapFarvann(paSkip.seilerI)
                flaggLandkode = paSkip.flaggland?.name
                territorialfarvannLandkode = paSkip.territorialfarvannLand?.name
                yrke = paSkip.yrketTilArbeidstaker
            }
        )
    }

    private fun mapFarvann(farvann: Farvann): Fartsomrader = when (farvann) {
        Farvann.INTERNASJONALT_FARVANN -> Fartsomrader.UTENRIKS
        Farvann.TERRITORIALFARVANN -> Fartsomrader.INNENRIKS
    }

    private fun mapLuftfart(arbeidssted: ArbeidsstedIUtlandetDto): List<LuftfartBase> {
        val omBordPaFly = arbeidssted.omBordPaFly ?: return emptyList()
        return listOf(
            LuftfartBase(
                hjemmebaseNavn = omBordPaFly.hjemmebaseNavn,
                hjemmebaseLand = omBordPaFly.hjemmebaseLand.name,
                typeFlyvninger = Flyvningstyper.INTERNASJONAL,
                erVanligHjemmebase = omBordPaFly.erVanligHjemmebase,
                vanligHjemmebaseLand = omBordPaFly.vanligHjemmebaseLand?.name,
                vanligHjemmebaseNavn = omBordPaFly.vanligHjemmebaseNavn
            )
        )
    }

    private fun mapPeriode(periodeDto: PeriodeDto?): Periode =
        periodeDto?.let { Periode(it.fraDato, it.tilDato) } ?: Periode()

    private fun mapSoeknadsland(landkode: LandKode?): Soeknadsland =
        Soeknadsland(landkode?.let { listOf(it.name) } ?: emptyList(), false)
}
