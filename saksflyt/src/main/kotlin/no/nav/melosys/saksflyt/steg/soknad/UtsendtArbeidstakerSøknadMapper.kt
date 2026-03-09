package no.nav.melosys.saksflyt.steg.soknad

import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.kodeverk.Flyvningstyper
import no.nav.melosys.domain.kodeverk.Innretningstyper
import no.nav.melosys.domain.kodeverk.begrunnelser.Fartsomrader
import no.nav.melosys.domain.mottatteopplysninger.Soeknad
import no.nav.melosys.domain.mottatteopplysninger.data.ArbeidssituasjonOgOevrig
import no.nav.melosys.domain.mottatteopplysninger.data.JuridiskArbeidsgiverNorge
import no.nav.melosys.domain.mottatteopplysninger.data.LoennOgGodtgjoerelse
import no.nav.melosys.domain.mottatteopplysninger.data.MedfolgendeFamilie
import no.nav.melosys.domain.mottatteopplysninger.data.OpplysningerOmBrukeren
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.domain.mottatteopplysninger.data.Utenlandsoppdraget
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.ArbeidPaaLand
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.LuftfartBase
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.MaritimtArbeid
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsstedIUtlandetDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsstedType
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Farvann
import no.nav.melosys.skjema.types.utsendtarbeidstaker.FastEllerVekslendeArbeidssted
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel
import no.nav.melosys.skjema.types.utsendtarbeidstaker.TypeInnretning
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtenlandsoppdragetDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerSkjemaDto
import no.nav.melosys.skjema.types.felles.LandKode
import no.nav.melosys.skjema.types.felles.PeriodeDto
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerSkjemaM2MDto

/**
 * Mapper digital søknadsdata (UtsendtArbeidstakerSkjemaM2MDto) til domenemodellen [Soeknad].
 *
 * Søknaden kan bestå av to deler (arbeidstakers del og arbeidsgivers del) som sendes inn
 * som separate skjema. Denne mapperen kombinerer data fra begge deler til én [Soeknad].
 */
object UtsendtArbeidstakerSøknadMapper {

    fun tilSoeknad(dto: UtsendtArbeidstakerSkjemaM2MDto): Soeknad {
        val (arbeidstakerSkjema, arbeidsgiverSkjema) = finnSkjemadeler(dto)
        val arbeidstakerData = arbeidstakerSkjema?.data as? UtsendtArbeidstakerArbeidstakersSkjemaDataDto
        val arbeidsgiverData = arbeidsgiverSkjema?.data as? UtsendtArbeidstakerArbeidsgiversSkjemaDataDto

        val kombinertData = (arbeidstakerSkjema?.data ?: arbeidsgiverSkjema?.data)
            as? UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto

        return Soeknad().apply {
            if (kombinertData != null) {
                // Kombinert skjema: bruk data fra begge delene i ett skjema
                val agData = kombinertData.arbeidsgiversData
                val atData = kombinertData.arbeidstakersData

                loennOgGodtgjoerelse = agData.arbeidstakerensLonn?.let {
                    LoennOgGodtgjoerelse(norskArbgUtbetalerLoenn = it.arbeidsgiverBetalerAllLonnOgNaturaytelserIUtsendingsperioden)
                }
                utenlandsoppdraget = mapUtenlandsoppdraget(agData.utenlandsoppdraget)

                arbeidssituasjonOgOevrig = ArbeidssituasjonOgOevrig().apply {
                    harLoennetArbeidMinstEnMndFoerUtsending = atData.arbeidssituasjon?.harVaertEllerSkalVaereILonnetArbeidFoerUtsending
                    beskrivelseArbeidSisteMnd = atData.arbeidssituasjon?.aktivitetIMaanedenFoerUtsendingen
                    harAndreArbeidsgivereIUtsendingsperioden = atData.arbeidssituasjon?.skalJobbeForFlereVirksomheter
                    erSkattepliktig = atData.skatteforholdOgInntekt?.erSkattepliktigTilNorgeIHeleutsendingsperioden
                    mottarYtelserUtlandet = atData.skatteforholdOgInntekt?.mottarPengestotteFraAnnetEosLandEllerSveits
                }

                // Søknadsland og periode fra kombinert data
                soeknadsland = mapSoeknadsland(kombinertData.utsendingsperiodeOgLand?.utsendelseLand)
                periode = mapPeriode(kombinertData.utsendingsperiodeOgLand?.utsendelsePeriode)

                // Arbeidssteder
                val arbeidssted = agData.arbeidsstedIUtlandet
                if (arbeidssted != null) {
                    when (arbeidssted.arbeidsstedType) {
                        ArbeidsstedType.PA_LAND -> arbeidPaaLand = mapArbeidPaaLand(arbeidssted)
                        ArbeidsstedType.OFFSHORE -> maritimtArbeid = mapOffshore(arbeidssted)
                        ArbeidsstedType.PA_SKIP -> maritimtArbeid = mapPaaSkip(arbeidssted)
                        ArbeidsstedType.OM_BORD_PA_FLY -> luftfartBaser = mapLuftfart(arbeidssted)
                    }
                }

                // Juridisk arbeidsgiver
                juridiskArbeidsgiverNorge = JuridiskArbeidsgiverNorge().apply {
                    erOffentligVirksomhet = agData.arbeidsgiverensVirksomhetINorge?.erArbeidsgiverenOffentligVirksomhet
                }

                // Personopplysninger
                val familiemedlemmer = atData.familiemedlemmer
                personOpplysninger = OpplysningerOmBrukeren().apply {
                    if (familiemedlemmer != null && familiemedlemmer.skalHaMedFamiliemedlemmer) {
                        medfolgendeFamilie = familiemedlemmer.familiemedlemmer.map { fm ->
                            MedfolgendeFamilie.tilBarnFraFnrOgNavn(
                                fm.fodselsnummer,
                                "${fm.fornavn} ${fm.etternavn}"
                            )
                        }
                    }
                }
            } else {
                // Separate skjemadeler (original logikk)
                loennOgGodtgjoerelse = arbeidsgiverData?.let { mapLoennOgGodtgjoerelse(it) }
                utenlandsoppdraget = arbeidsgiverData?.let { mapUtenlandsoppdraget(it.utenlandsoppdraget) }
                    ?: Utenlandsoppdraget()
                arbeidssituasjonOgOevrig = arbeidstakerData?.let { mapArbeidssituasjonOgOevrig(it) }
                    ?: ArbeidssituasjonOgOevrig()

                // Sett arvede mutable parent-felter. Arbeidsgiver først, arbeidstaker sist (presedens)
                arbeidsgiverData?.let { applyArbeidsgiverData(this, it) }
                arbeidstakerData?.let { applyArbeidstakerData(this, it) }
            }
        }
    }

    private fun applyArbeidsgiverData(søknad: Soeknad, data: UtsendtArbeidstakerArbeidsgiversSkjemaDataDto) {
        søknad.soeknadsland = mapSoeknadsland(data.utsendingsperiodeOgLand?.utsendelseLand)
        søknad.periode = mapPeriode(data.utsendingsperiodeOgLand?.utsendelsePeriode)
        mapArbeidssteder(søknad, data)
        søknad.juridiskArbeidsgiverNorge = mapJuridiskArbeidsgiverNorge(data)
    }

    private fun applyArbeidstakerData(søknad: Soeknad, data: UtsendtArbeidstakerArbeidstakersSkjemaDataDto) {
        søknad.soeknadsland = mapSoeknadsland(data.utsendingsperiodeOgLand?.utsendelseLand)
        søknad.periode = mapPeriode(data.utsendingsperiodeOgLand?.utsendelsePeriode)
        søknad.personOpplysninger = mapPersonopplysninger(data)
    }

    /**
     * Finner arbeidstaker- og arbeidsgiver-skjema fra M2M-DTOen.
     * Hovedskjemaet (skjema) og koblet skjema kan være enten arbeidstakers eller arbeidsgivers del,
     * eller et kombinert skjema som inneholder begge deler.
     *
     * Ved kombinert skjema returneres det som første element i paret.
     */
    internal fun finnSkjemadeler(
        dto: UtsendtArbeidstakerSkjemaM2MDto
    ): Pair<UtsendtArbeidstakerSkjemaDto?, UtsendtArbeidstakerSkjemaDto?> {
        val skjemaer = listOfNotNull(dto.skjema, dto.kobletSkjema)
        val kombinertSkjema = skjemaer.find { it.metadata.skjemadel == Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL }
        if (kombinertSkjema != null) {
            return kombinertSkjema to null
        }
        val arbeidstakerSkjema = skjemaer.find { it.metadata.skjemadel == Skjemadel.ARBEIDSTAKERS_DEL }
        val arbeidsgiverSkjema = skjemaer.find { it.metadata.skjemadel == Skjemadel.ARBEIDSGIVERS_DEL }
        return arbeidstakerSkjema to arbeidsgiverSkjema
    }

    // --- Søknadsland ---

    private fun mapSoeknadsland(landkode: LandKode?): Soeknadsland =
        Soeknadsland(landkode?.let { listOf(it.name) } ?: emptyList(), false)

    // --- Periode ---

    private fun mapPeriode(periodeDto: PeriodeDto?): Periode =
        periodeDto?.let { Periode(it.fraDato, it.tilDato) } ?: Periode()

    // --- Personopplysninger ---

    private fun mapPersonopplysninger(
        arbeidstakerData: UtsendtArbeidstakerArbeidstakersSkjemaDataDto
    ): OpplysningerOmBrukeren {
        val personopplysninger = OpplysningerOmBrukeren()
        val familiemedlemmer = arbeidstakerData.familiemedlemmer
        if (familiemedlemmer != null && familiemedlemmer.skalHaMedFamiliemedlemmer) {
            personopplysninger.medfolgendeFamilie = familiemedlemmer.familiemedlemmer.map { fm ->
                MedfolgendeFamilie.tilBarnFraFnrOgNavn(
                    fm.fodselsnummer,
                    "${fm.fornavn} ${fm.etternavn}"
                )
            }
        }
        return personopplysninger
    }

    // --- Arbeidssteder ---

    private fun mapArbeidssteder(
        søknad: Soeknad,
        arbeidsgiverData: UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
    ) {
        val arbeidssted = arbeidsgiverData.arbeidsstedIUtlandet ?: return
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

            val fastArbeidssted = paLand.fastArbeidssted
            if (fastArbeidssted != null) {
                fysiskeArbeidssteder = listOf(
                    FysiskArbeidssted(
                        virksomhetNavn = paLand.navnPaVirksomhet,
                        adresse = StrukturertAdresse(
                            gatenavn = fastArbeidssted.vegadresse,
                            husnummerEtasjeLeilighet = fastArbeidssted.nummer,
                            postnummer = fastArbeidssted.postkode,
                            poststed = fastArbeidssted.bySted,
                            region = null,
                            landkode = null
                        )
                    )
                )
            } else {
                fysiskeArbeidssteder = listOf(
                    FysiskArbeidssted(virksomhetNavn = paLand.navnPaVirksomhet)
                )
            }
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
                typeFlyvninger = Flyvningstyper.INTERNASJONAL
            )
        )
    }

    // --- Utenlandsoppdraget ---

    private fun mapUtenlandsoppdraget(utenlandsoppdraget: UtenlandsoppdragetDto?): Utenlandsoppdraget {
        if (utenlandsoppdraget == null) return Utenlandsoppdraget()

        val samletPeriode = if (utenlandsoppdraget.arbeidstakerErstatterAnnenPerson) {
            utenlandsoppdraget.forrigeArbeidstakerUtsendelsePeriode?.let {
                Periode(it.fraDato, it.tilDato)
            } ?: Periode()
        } else {
            Periode()
        }

        return Utenlandsoppdraget(
            samletUtsendingsperiode = samletPeriode,
            erUtsendelseForOppdragIUtlandet = utenlandsoppdraget.arbeidsgiverHarOppdragILandet,
            erFortsattAnsattEtterOppdraget = utenlandsoppdraget.arbeidstakerForblirAnsattIHelePerioden,
            erAnsattForOppdragIUtlandet = utenlandsoppdraget.arbeidstakerBleAnsattForUtenlandsoppdraget,
            erDrattPaaEgetInitiativ = null,
            erErstatningTidligereUtsendte = utenlandsoppdraget.arbeidstakerErstatterAnnenPerson
        )
    }

    // --- Juridisk arbeidsgiver Norge ---

    private fun mapJuridiskArbeidsgiverNorge(
        arbeidsgiverData: UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
    ): JuridiskArbeidsgiverNorge {
        val virksomhet = arbeidsgiverData.arbeidsgiverensVirksomhetINorge
        return JuridiskArbeidsgiverNorge().apply {
            erOffentligVirksomhet = virksomhet?.erArbeidsgiverenOffentligVirksomhet
        }
    }

    // --- Lønn og godtgjørelse ---

    private fun mapLoennOgGodtgjoerelse(
        arbeidsgiverData: UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
    ): LoennOgGodtgjoerelse {
        val lonn = arbeidsgiverData.arbeidstakerensLonn
        return LoennOgGodtgjoerelse(
            norskArbgUtbetalerLoenn = lonn?.arbeidsgiverBetalerAllLonnOgNaturaytelserIUtsendingsperioden
        )
    }

    // --- Arbeidssituasjon og øvrig ---

    private fun mapArbeidssituasjonOgOevrig(
        arbeidstakerData: UtsendtArbeidstakerArbeidstakersSkjemaDataDto
    ): ArbeidssituasjonOgOevrig {
        val arbeidssituasjon = arbeidstakerData.arbeidssituasjon
        val skatt = arbeidstakerData.skatteforholdOgInntekt

        return ArbeidssituasjonOgOevrig().apply {
            harLoennetArbeidMinstEnMndFoerUtsending = arbeidssituasjon?.harVaertEllerSkalVaereILonnetArbeidFoerUtsending
            beskrivelseArbeidSisteMnd = arbeidssituasjon?.aktivitetIMaanedenFoerUtsendingen
            harAndreArbeidsgivereIUtsendingsperioden = arbeidssituasjon?.skalJobbeForFlereVirksomheter
            erSkattepliktig = skatt?.erSkattepliktigTilNorgeIHeleutsendingsperioden
            mottarYtelserUtlandet = skatt?.mottarPengestotteFraAnnetEosLandEllerSveits
        }
    }
}
