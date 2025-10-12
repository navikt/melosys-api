package no.nav.melosys.service.behandling

import io.getunleash.Unleash
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.avklartefakta.Avklartefakta
import no.nav.melosys.domain.avklartefakta.AvklartefaktaRegistrering
import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.featuretoggle.ToggleName
import org.apache.commons.beanutils.BeanUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.lang.reflect.InvocationTargetException
import java.time.LocalDate

@Service
class ReplikerBehandlingsresultatService(
    val behandlingsresultatService: BehandlingsresultatService,
    private val unleash: Unleash
) {

    @Transactional(rollbackFor = [Exception::class])
    fun replikerBehandlingsresultat(tidligsteInaktiveBehandling: Behandling, behandlingReplika: Behandling) {
        val behandlingsresultatOriginal: Behandlingsresultat =
            behandlingsresultatService.hentBehandlingsresultat(tidligsteInaktiveBehandling.id)

        val behandlingsresultatReplika = BeanUtils.cloneBean(behandlingsresultatOriginal) as Behandlingsresultat

        behandlingsresultatReplika.behandling = behandlingReplika
        behandlingsresultatReplika.id = null
        behandlingsresultatReplika.vedtakMetadata = null
        behandlingsresultatReplika.utfallRegistreringUnntak = null
        behandlingsresultatReplika.utfallUtpeking = null
        behandlingsresultatReplika.behandlingsmåte = Behandlingsmaate.MANUELT
        behandlingsresultatReplika.type = Behandlingsresultattyper.IKKE_FASTSATT

        replikerAvklartefakta(behandlingsresultatOriginal, behandlingsresultatReplika)
        replikerLovvalgsperioder(behandlingsresultatOriginal, behandlingsresultatReplika)
        replikerVilkaarsresultat(behandlingsresultatOriginal, behandlingsresultatReplika)
        replikerAnmodningsperioder(behandlingsresultatOriginal, behandlingsresultatReplika)
        replikerBehandlingsresultatBegrunnelser(behandlingsresultatOriginal, behandlingsresultatReplika)
        replikerKontrollResultater(behandlingsresultatOriginal, behandlingsresultatReplika)
        replikerUtpekingsperioder(behandlingsresultatOriginal, behandlingsresultatReplika)

        if (behandlingReplika.erEøsPensjonist()) {
            replikerHelseutgiftDekkesPeriode(behandlingsresultatOriginal, behandlingsresultatReplika)
            replikerTrygdeavgiftForPensjonist(behandlingsresultatOriginal, behandlingsresultatReplika)
            behandlingsresultatReplika.hentHelseutgiftDekkesPeriode().id = null
        } else {
            replikerMedlemAvFolketrygden(behandlingsresultatOriginal, behandlingsresultatReplika, behandlingReplika.type)
        }

        behandlingsresultatService.lagre(behandlingsresultatReplika)
    }

    @Throws(
        InvocationTargetException::class,
        NoSuchMethodException::class,
        InstantiationException::class,
        IllegalAccessException::class
    )
    private fun replikerMedlemAvFolketrygden(
        behandlingsresultatOriginal: Behandlingsresultat,
        behandlingsresultatReplika: Behandlingsresultat,
        behandlingstype: Behandlingstyper
    ) {
        replikerMedlemskapsperioderBasertPåBehandlingstype(behandlingsresultatOriginal, behandlingsresultatReplika, behandlingstype)
        replikerTrygdeavgift(behandlingsresultatOriginal, behandlingsresultatReplika)

        behandlingsresultatReplika.medlemskapsperioder.onEach { it.id = null }
    }

    private fun replikerMedlemskapsperioderBasertPåBehandlingstype(
        behandlingsresultatOriginal: Behandlingsresultat,
        behandlingsresultatReplika: Behandlingsresultat,
        behandlingstype: Behandlingstyper
    ) {
        behandlingsresultatReplika.medlemskapsperioder = HashSet()

        val filtrertMedlemskapsperioderOriginal = if (behandlingstype == Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT) {
            behandlingsresultatOriginal.medlemskapsperioder.filter { it.erInnvilget() || it.erOpphørt() }
        } else {
            behandlingsresultatOriginal.medlemskapsperioder.filter { it.erInnvilget() }
        }

        for (medlemskapsperiodeOriginal in filtrertMedlemskapsperioderOriginal) {
            val medlemskapsperiodeReplika = BeanUtils.cloneBean(medlemskapsperiodeOriginal) as Medlemskapsperiode
            medlemskapsperiodeReplika.behandlingsresultat = behandlingsresultatReplika
            behandlingsresultatReplika.medlemskapsperioder.add(medlemskapsperiodeReplika)
        }
    }

    private fun replikerTrygdeavgift(
        behandlingsresultatOriginal: Behandlingsresultat,
        behandlingsresultatReplika: Behandlingsresultat
    ) {
        val trygdeavgiftsperioderTilReplikering = filtrerTrygdeavgiftsperioder(behandlingsresultatOriginal.trygdeavgiftsperioder)

        // Bruker nye metoder som kloner og justerer datoer
        val inntektsperioderReplika = cloneOgJusterInntektsperioder(trygdeavgiftsperioderTilReplikering)
        val skatteforholdTilNorgeReplika = cloneOgJusterSkatteforhold(trygdeavgiftsperioderTilReplikering)

        behandlingsresultatReplika.medlemskapsperioder.forEach { medlemskapsperiodeReplika ->
            medlemskapsperiodeReplika.trygdeavgiftsperioder = HashSet()
        }

        trygdeavgiftsperioderTilReplikering.forEach { trygdeavgiftsperiodeOriginal ->
            val trygdeavgiftsperiodeReplika = trygdeavgiftsperiodeOriginal.copyEntity(
                id = trygdeavgiftsperiodeOriginal.id,
                grunnlagMedlemskapsperiode = behandlingsresultatReplika.medlemskapsperioder
                    .find { it.id == trygdeavgiftsperiodeOriginal.grunnlagMedlemskapsperiode?.id }
                    ?: throw IllegalStateException("Medlemskapsperiode ikke funnet"),
                // I de tilfellene bruker ikke skal betale avgift til Nav, er det ikke krav om at inntektsperioder må være satt.
                grunnlagInntekstperiode = inntektsperioderReplika
                    .find { it.id == trygdeavgiftsperiodeOriginal.grunnlagInntekstperiode?.id },
                grunnlagSkatteforholdTilNorge = skatteforholdTilNorgeReplika
                    .find { it.id == trygdeavgiftsperiodeOriginal.grunnlagSkatteforholdTilNorge?.id }
                    ?: throw IllegalStateException("SkatteforholdTilNorge ikke funnet"),
            )

            trygdeavgiftsperiodeReplika.grunnlagMedlemskapsperiode?.run {
                trygdeavgiftsperioder.add(trygdeavgiftsperiodeReplika)
            } ?: throw IllegalStateException("Medlemskapsperiode ikke funnet (dette skal ikke kunne skje)")
        }

        behandlingsresultatReplika.trygdeavgiftsperioder.forEach { trygdeavgiftsperiodeReplika ->
            trygdeavgiftsperiodeReplika.id = null
            trygdeavgiftsperiodeReplika.grunnlagInntekstperiode?.id = null
            trygdeavgiftsperiodeReplika.grunnlagSkatteforholdTilNorge?.id = null
            trygdeavgiftsperiodeReplika.grunnlagMedlemskapsperiode?.id = null
        }
    }

    private fun replikerTrygdeavgiftForPensjonist(
        behandlingsresultatOriginal: Behandlingsresultat,
        behandlingsresultatReplika: Behandlingsresultat
    ) {
        val trygdeavgiftsperioderTilReplikering = filtrerTrygdeavgiftsperioder(
            behandlingsresultatOriginal.hentHelseutgiftDekkesPeriode().trygdeavgiftsperioder
        )

        // Bruker nye metoder som kloner og justerer datoer
        val inntektsperioderReplika = cloneOgJusterInntektsperioder(trygdeavgiftsperioderTilReplikering)
        val skatteforholdTilNorgeReplika = cloneOgJusterSkatteforhold(trygdeavgiftsperioderTilReplikering)

        behandlingsresultatReplika.medlemskapsperioder = HashSet()
        behandlingsresultatReplika.hentHelseutgiftDekkesPeriode().trygdeavgiftsperioder = HashSet()

        trygdeavgiftsperioderTilReplikering.forEach { trygdeavgiftsperiodeOriginal ->
            val trygdeavgiftsperiodeReplika = trygdeavgiftsperiodeOriginal.copyEntity(
                id = trygdeavgiftsperiodeOriginal.id,
                grunnlagHelseutgiftDekkesPeriode = behandlingsresultatReplika.helseutgiftDekkesPeriode,
                grunnlagInntekstperiode = inntektsperioderReplika
                    .find { it.id == trygdeavgiftsperiodeOriginal.grunnlagInntekstperiode?.id },
                grunnlagSkatteforholdTilNorge = skatteforholdTilNorgeReplika
                    .find { it.id == trygdeavgiftsperiodeOriginal.grunnlagSkatteforholdTilNorge?.id }
                    ?: throw IllegalStateException("SkatteforholdTilNorge ikke funnet"),
            )

            trygdeavgiftsperiodeReplika.grunnlagHelseutgiftDekkesPeriode?.run {
                trygdeavgiftsperioder.add(trygdeavgiftsperiodeReplika)
            } ?: throw IllegalStateException("Helseutgift dekkes periode ikke funnet")
        }

        behandlingsresultatReplika.hentHelseutgiftDekkesPeriode().trygdeavgiftsperioder.forEach { trygdeavgiftsperiodeReplika ->
            trygdeavgiftsperiodeReplika.id = null
            trygdeavgiftsperiodeReplika.grunnlagHelseutgiftDekkesPeriode?.id = null
            trygdeavgiftsperiodeReplika.grunnlagInntekstperiode?.id = null
            trygdeavgiftsperiodeReplika.grunnlagSkatteforholdTilNorge?.id = null
        }
    }

    /**
     * Sjekker om ny årfiltrerings-logikk skal brukes
     */
    private fun skalBrukeNyÅrfiltrering(): Boolean {
        return unleash.isEnabled(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)
    }

    /**
     * Filtrerer og avkorter trygdeavgiftsperioder basert på årfiltrering-toggle
     */
    internal fun filtrerTrygdeavgiftsperioder(trygdeavgiftsperioder: Collection<Trygdeavgiftsperiode>): List<Trygdeavgiftsperiode> {
        return if (skalBrukeNyÅrfiltrering()) {
            val inneværendeÅr = LocalDate.now().year

            trygdeavgiftsperioder.forEach {
                if (it.periodeFra.year != it.periodeTil.year) {
                    throw IllegalStateException("Trygdeavgiftsperiode ${it.id} går over flere år (${it.periodeFra} - ${it.periodeTil})")
                }
            }

            trygdeavgiftsperioder.filter { trygdeavgiftsperiode ->
                // Kun perioder som overlapper med inneværende år og fremover
                trygdeavgiftsperiode.periodeTil.year >= inneværendeÅr
            }

        } else {
            trygdeavgiftsperioder.toList()
        }
    }

    /**
     * Clone og justerer inntektsperioder basert på filtrerte trygdeavgiftsperioder
     * Hvis toggle er på, avkorter periodene til å starte tidligst 1. januar inneværende år
     */
    private fun cloneOgJusterInntektsperioder(
        trygdeavgiftsperioderTilReplikering: List<Trygdeavgiftsperiode>
    ): List<Inntektsperiode> {
        val inntektsperioderTilReplikering = trygdeavgiftsperioderTilReplikering
            .mapNotNull { it.grunnlagInntekstperiode }
            .distinctBy { it.id }

        return if (skalBrukeNyÅrfiltrering()) {
            val førsteJanuar = LocalDate.now().withDayOfYear(1)

            inntektsperioderTilReplikering.map { inntektsperiode ->
                val clone = BeanUtils.cloneBean(inntektsperiode) as Inntektsperiode
                // Avkort periode hvis den starter før 1. januar inneværende år
                if (clone.fomDato.isBefore(førsteJanuar)) {
                    clone.fomDato = førsteJanuar
                }
                clone
            }
        } else {
            inntektsperioderTilReplikering.map {
                BeanUtils.cloneBean(it) as Inntektsperiode
            }
        }
    }

    /**
     * Kloner og justerer skatteforhold basert på filtrerte trygdeavgiftsperioder
     * Hvis toggle er på, avkorter periodene til å starte tidligst 1. januar inneværende år
     */
    private fun cloneOgJusterSkatteforhold(
        trygdeavgiftsperioderTilReplikering: List<Trygdeavgiftsperiode>
    ): List<SkatteforholdTilNorge> {
        val skatteforholdTilReplikering = trygdeavgiftsperioderTilReplikering
            .mapNotNull { it.grunnlagSkatteforholdTilNorge }
            .distinctBy { it.id }

        return if (skalBrukeNyÅrfiltrering()) {
            val førsteJanuar = LocalDate.now().withDayOfYear(1)

            skatteforholdTilReplikering.map { skatteforhold ->
                val clone = BeanUtils.cloneBean(skatteforhold) as SkatteforholdTilNorge
                // Avkort periode hvis den starter før 1. januar inneværende år
                if (clone.fomDato.isBefore(førsteJanuar)) {
                    clone.fomDato = førsteJanuar
                }
                clone
            }
        } else {
            skatteforholdTilReplikering.map {
                BeanUtils.cloneBean(it) as SkatteforholdTilNorge
            }
        }
    }
}

private fun replikerUtpekingsperioder(
    behandlingsresultatOrig: Behandlingsresultat,
    behandlingsresultatsReplika: Behandlingsresultat
) {
    behandlingsresultatsReplika.utpekingsperioder = HashSet()
    for (utpekingsperiodeOriginal in behandlingsresultatOrig.utpekingsperioder) {
        val utpekingsperiodeReplika = BeanUtils.cloneBean(utpekingsperiodeOriginal) as Utpekingsperiode
        utpekingsperiodeReplika.behandlingsresultat = behandlingsresultatsReplika
        utpekingsperiodeReplika.id = null
        utpekingsperiodeReplika.medlPeriodeID = null
        utpekingsperiodeReplika.sendtUtland = null
        behandlingsresultatsReplika.utpekingsperioder.add(utpekingsperiodeReplika)
    }
}

private fun replikerAnmodningsperioder(
    behandlingsresultatOrig: Behandlingsresultat,
    behandlingsresultatReplika: Behandlingsresultat
) {
    behandlingsresultatReplika.anmodningsperioder = HashSet()
    for (anmodningsperiodeOriginal in behandlingsresultatOrig.anmodningsperioder) {
        val anmodningsperiodeReplika = BeanUtils.cloneBean(anmodningsperiodeOriginal) as Anmodningsperiode
        anmodningsperiodeReplika.behandlingsresultat = behandlingsresultatReplika
        anmodningsperiodeReplika.id = null
        anmodningsperiodeReplika.medlPeriodeID = null
        anmodningsperiodeReplika.setSendtUtland(false)
        anmodningsperiodeReplika.anmodningsperiodeSvar = null
        behandlingsresultatReplika.anmodningsperioder.add(anmodningsperiodeReplika)
    }
}

private fun replikerVilkaarsresultat(
    behandlingsresultatOrig: Behandlingsresultat,
    behandlingsresultatReplika: Behandlingsresultat
) {
    behandlingsresultatReplika.vilkaarsresultater = HashSet()
    for (vilkaarsresultatOriginal in behandlingsresultatOrig.vilkaarsresultater) {
        val vilkaarsresultatReplika = BeanUtils.cloneBean(vilkaarsresultatOriginal) as Vilkaarsresultat
        vilkaarsresultatReplika.behandlingsresultat = behandlingsresultatReplika
        vilkaarsresultatReplika.id = null
        vilkaarsresultatReplika.begrunnelser = HashSet()
        for (vilkaarBegrunnelseOriginal in vilkaarsresultatOriginal.begrunnelser) {
            val vilkaarBegrunnelsesReplika = BeanUtils.cloneBean(vilkaarBegrunnelseOriginal) as VilkaarBegrunnelse
            vilkaarBegrunnelsesReplika.vilkaarsresultat = vilkaarsresultatReplika
            vilkaarBegrunnelsesReplika.id = null
            vilkaarsresultatReplika.begrunnelser.add(vilkaarBegrunnelsesReplika)
        }
        behandlingsresultatReplika.vilkaarsresultater.add(vilkaarsresultatReplika)
    }
}

@Throws(
    IllegalAccessException::class,
    InstantiationException::class,
    InvocationTargetException::class,
    NoSuchMethodException::class
)
private fun replikerLovvalgsperioder(
    behandlingsresultatOrig: Behandlingsresultat,
    behandlingsresultatReplika: Behandlingsresultat
) {
    behandlingsresultatReplika.lovvalgsperioder = HashSet()
    for (lovvalgsperiodeOriginal in behandlingsresultatOrig.lovvalgsperioder) {
        val lovvalgsperiodeReplika = BeanUtils.cloneBean(lovvalgsperiodeOriginal) as Lovvalgsperiode
        lovvalgsperiodeReplika.behandlingsresultat = behandlingsresultatReplika
        lovvalgsperiodeReplika.id = null
        behandlingsresultatReplika.lovvalgsperioder.add(lovvalgsperiodeReplika)
    }
}

private fun replikerHelseutgiftDekkesPeriode(
    behandlingsresultatOrig: Behandlingsresultat,
    behandlingsresultatReplika: Behandlingsresultat
) {
    behandlingsresultatReplika.helseutgiftDekkesPeriode = null

    val orig = behandlingsresultatOrig.helseutgiftDekkesPeriode ?: return

    val helseutgiftDekkesPeriodeReplika = HelseutgiftDekkesPeriode(
        behandlingsresultat = behandlingsresultatReplika,
        fomDato = orig.fomDato,
        tomDato = orig.tomDato,
        bostedLandkode = orig.bostedLandkode
    ).apply {
        id = null
    }

    behandlingsresultatReplika.helseutgiftDekkesPeriode = helseutgiftDekkesPeriodeReplika
}

@Throws(
    IllegalAccessException::class,
    InstantiationException::class,
    InvocationTargetException::class,
    NoSuchMethodException::class
)
private fun replikerBehandlingsresultatBegrunnelser(
    behandlingsresultatOrig: Behandlingsresultat,
    behandlingsresultatReplika: Behandlingsresultat
) {
    behandlingsresultatReplika.behandlingsresultatBegrunnelser = HashSet()
    for (behandlingsresultatBegrunnelseOriginal in behandlingsresultatOrig.behandlingsresultatBegrunnelser) {
        val behandlingsresultatBegrunnelseReplika =
            BeanUtils.cloneBean(behandlingsresultatBegrunnelseOriginal) as BehandlingsresultatBegrunnelse
        behandlingsresultatBegrunnelseReplika.behandlingsresultat = behandlingsresultatReplika
        behandlingsresultatBegrunnelseReplika.id = null
        behandlingsresultatReplika.behandlingsresultatBegrunnelser.add(behandlingsresultatBegrunnelseReplika)
    }
}

@Throws(
    IllegalAccessException::class,
    InstantiationException::class,
    InvocationTargetException::class,
    NoSuchMethodException::class
)
private fun replikerAvklartefakta(
    behandlingsresultatOrig: Behandlingsresultat,
    behandlingsresultatReplika: Behandlingsresultat
) {
    behandlingsresultatReplika.avklartefakta = HashSet()
    for (avklartefaktaOriginal in behandlingsresultatOrig.avklartefakta) {
        val avklartefaktaReplika = BeanUtils.cloneBean(avklartefaktaOriginal) as Avklartefakta
        avklartefaktaReplika.behandlingsresultat = behandlingsresultatReplika
        avklartefaktaReplika.id = null
        avklartefaktaReplika.registreringer = HashSet()
        for (avklartefaktaRegistreringOriginal in avklartefaktaOriginal.registreringer) {
            val avklartefaktaRegistreringReplika =
                BeanUtils.cloneBean(avklartefaktaRegistreringOriginal) as AvklartefaktaRegistrering
            avklartefaktaRegistreringReplika.avklartefakta = avklartefaktaReplika
            avklartefaktaRegistreringReplika.id = null
            avklartefaktaReplika.registreringer.add(avklartefaktaRegistreringReplika)
        }
        behandlingsresultatReplika.avklartefakta.add(avklartefaktaReplika)
    }
}

private fun replikerKontrollResultater(
    behandlingsresultatOrig: Behandlingsresultat,
    behandlingsresultatReplika: Behandlingsresultat
) {
    behandlingsresultatReplika.kontrollresultater = HashSet()
    for (kontrollresultatOriginal in behandlingsresultatOrig.kontrollresultater) {
        val kontrollresultatReplika = BeanUtils.cloneBean(kontrollresultatOriginal) as Kontrollresultat
        kontrollresultatReplika.behandlingsresultat = behandlingsresultatReplika
        kontrollresultatReplika.id = null
        behandlingsresultatReplika.kontrollresultater.add(kontrollresultatReplika)
    }
}
