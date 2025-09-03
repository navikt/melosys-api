package no.nav.melosys.service.behandling

import no.nav.melosys.domain.*
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.avklartefakta.Avklartefakta
import no.nav.melosys.domain.avklartefakta.AvklartefaktaRegistrering
import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import org.apache.commons.beanutils.BeanUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import io.getunleash.Unleash
import no.nav.melosys.featuretoggle.ToggleName
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
            behandlingsresultatReplika.helseutgiftDekkesPeriode.id = null
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

    /**
     * Sjekker om ny årfiltrerings-logikk skal brukes
     */
    private fun skalBrukeNyÅrfiltrering(): Boolean {
        return unleash.isEnabled("melosys.replikkering.trygdeavgift.årsfiltrering")
    }

    /**
     * Filtrerer og avkorter inntektsperioder basert på årfiltrering-toggle
     */
    internal fun filtrerInntektsperioder(inntektsperioder: Collection<Inntektsperiode>): List<Inntektsperiode> {
        return if (skalBrukeNyÅrfiltrering()) {
            val inneværendeÅr = LocalDate.now().year
            val første1Januar = LocalDate.of(inneværendeÅr, 1, 1)

            inntektsperioder.filter { inntektsperiode ->
                // Kun perioder som overlapper med inneværende år og fremover
                (inntektsperiode.tomDato?.year ?: Int.MAX_VALUE) >= inneværendeÅr
            }.map { inntektsperiode ->
                // Avkort periode slik at den starter tidligst 1. januar i inneværende år
                if (inntektsperiode.fomDato?.isBefore(første1Januar) == true) {
                    BeanUtils.cloneBean(inntektsperiode).apply {
                        (this as Inntektsperiode).fomDato = første1Januar
                    } as Inntektsperiode
                } else {
                    inntektsperiode
                }
            }
        } else {
            inntektsperioder.toList()
        }
    }

    /**
     * Filtrerer og avkorter skatteforhold basert på årfiltrering-toggle
     */
    internal fun filtrerSkatteforhold(skatteforhold: Collection<SkatteforholdTilNorge?>): List<SkatteforholdTilNorge> {
        val filterNotNull = skatteforhold.filterNotNull()
        return if (skalBrukeNyÅrfiltrering()) {
            val inneværendeÅr = LocalDate.now().year
            val første1Januar = LocalDate.of(inneværendeÅr, 1, 1)

            filterNotNull.filter { skatteforhold ->
                // Kun perioder som overlapper med inneværende år og fremover
                (skatteforhold.tomDato?.year ?: Int.MAX_VALUE) >= inneværendeÅr
            }.map { skatteforhold ->
                // Avkort periode slik at den starter tidligst 1. januar i inneværende år
                if (skatteforhold.fomDato?.isBefore(første1Januar) == true) {
                    BeanUtils.cloneBean(skatteforhold).apply {
                        (this as SkatteforholdTilNorge).fomDato = første1Januar
                    } as SkatteforholdTilNorge
                } else {
                    skatteforhold
                }
            }
        } else {
            filterNotNull
        }
    }

    /**
     * Filtrerer og avkorter trygdeavgiftsperioder basert på årfiltrering-toggle
     */
    internal fun filtrerTrygdeavgiftsperioder(trygdeavgiftsperioder: Collection<Trygdeavgiftsperiode>): List<Trygdeavgiftsperiode> {
        return if (skalBrukeNyÅrfiltrering()) {
            val inneværendeÅr = LocalDate.now().year
            val første1Januar = LocalDate.of(inneværendeÅr, 1, 1)

            trygdeavgiftsperioder.filter { trygdeavgiftsperiode ->
                // Kun perioder som overlapper med inneværende år og fremover
                trygdeavgiftsperiode.periodeTil.year >= inneværendeÅr
            }.map { trygdeavgiftsperiode ->
                // Avkort periode slik at den starter tidligst 1. januar i inneværende år
                if (trygdeavgiftsperiode.periodeFra.isBefore(første1Januar)) {
                    trygdeavgiftsperiode.copyEntity(
                        periodeFra = første1Januar
                    )
                } else {
                    trygdeavgiftsperiode
                }
            }
        } else {
            trygdeavgiftsperioder.toList()
        }
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
        val inntektsperioderTilReplikering = filtrerInntektsperioder(behandlingsresultatOriginal.hentInntektsperioder())
        val skatteforholdTilReplikering = filtrerSkatteforhold(behandlingsresultatOriginal.hentSkatteforholdTilNorge())

        val inntektsperioderReplika = inntektsperioderTilReplikering.map {
            BeanUtils.cloneBean(it) as Inntektsperiode
        }
        val skatteforholdTilNorgeReplika = skatteforholdTilReplikering.map {
            BeanUtils.cloneBean(it) as SkatteforholdTilNorge
        }

        behandlingsresultatReplika.medlemskapsperioder.forEach { medlemskapsperiodeReplika ->
            medlemskapsperiodeReplika.trygdeavgiftsperioder = HashSet()
        }

        val trygdeavgiftsperioderTilReplikering = filtrerTrygdeavgiftsperioder(behandlingsresultatOriginal.trygdeavgiftsperioder)

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
            behandlingsresultatOriginal.helseutgiftDekkesPeriode.trygdeavgiftsperioder
        )

        // Skatteforhold og inntektsperioder kommer fra de allerede-filtrerte trygdeavgiftsperiodene
        val alleInntektsperioder = trygdeavgiftsperioderTilReplikering
            .mapNotNull { it.grunnlagInntekstperiode }
        val alleSkatteforhold = trygdeavgiftsperioderTilReplikering
            .mapNotNull { it.grunnlagSkatteforholdTilNorge }

        val inntektsperioderReplika = alleInntektsperioder.map {
            BeanUtils.cloneBean(it) as Inntektsperiode
        }
        val skatteforholdTilNorgeReplika = alleSkatteforhold.map {
            BeanUtils.cloneBean(it) as SkatteforholdTilNorge
        }
        behandlingsresultatReplika.medlemskapsperioder = HashSet()
        behandlingsresultatReplika.helseutgiftDekkesPeriode.trygdeavgiftsperioder = HashSet()

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

        behandlingsresultatReplika.helseutgiftDekkesPeriode.trygdeavgiftsperioder.forEach { trygdeavgiftsperiodeReplika ->
            trygdeavgiftsperiodeReplika.id = null
            trygdeavgiftsperiodeReplika.grunnlagHelseutgiftDekkesPeriode?.id = null
            trygdeavgiftsperiodeReplika.grunnlagInntekstperiode?.id = null
            trygdeavgiftsperiodeReplika.grunnlagSkatteforholdTilNorge?.id = null
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
