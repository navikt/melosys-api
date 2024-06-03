package no.nav.melosys.service.behandling

import no.nav.melosys.domain.*
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.avklartefakta.Avklartefakta
import no.nav.melosys.domain.avklartefakta.AvklartefaktaRegistrering
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import org.apache.commons.beanutils.BeanUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.lang.reflect.InvocationTargetException

@Service
class ReplikerBehandlingsresultatService(val behandlingsresultatService: BehandlingsresultatService) {

    @Transactional(rollbackFor = [Exception::class])
    @Throws(
        InvocationTargetException::class,
        NoSuchMethodException::class,
        InstantiationException::class,
        IllegalAccessException::class
    )
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
        replikerMedlemAvFolketrygden(behandlingsresultatOriginal, behandlingsresultatReplika, behandlingReplika.type)

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

    @Throws(
        InvocationTargetException::class,
        NoSuchMethodException::class,
        InstantiationException::class,
        IllegalAccessException::class
    )
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

    @Throws(
        InvocationTargetException::class,
        NoSuchMethodException::class,
        InstantiationException::class,
        IllegalAccessException::class
    )
    private fun replikerTrygdeavgift(
        behandlingsresultatOriginal: Behandlingsresultat,
        behandlingsresultatReplika: Behandlingsresultat,
    ) {
        val inntektsperioderReplika = behandlingsresultatOriginal.hentInntektsperioder().map {
            BeanUtils.cloneBean(it) as Inntektsperiode
        }
        val skatteforholdTilNorgeReplika = behandlingsresultatOriginal.hentSkatteforholdTilNorge().map {
            BeanUtils.cloneBean(it) as SkatteforholdTilNorge
        }

        behandlingsresultatReplika.medlemskapsperioder.forEach{
            it.trygdeavgiftsperioder = HashSet()
        }
        for (trygdeavgiftsperiodeOriginal in behandlingsresultatOriginal.trygdeavgiftsperioder) {
            val trygdeavgiftsperiodeReplika = BeanUtils.cloneBean(trygdeavgiftsperiodeOriginal) as Trygdeavgiftsperiode

            trygdeavgiftsperiodeReplika.grunnlagMedlemskapsperiode = behandlingsresultatReplika.medlemskapsperioder
                .find { it.id == trygdeavgiftsperiodeOriginal.grunnlagMedlemskapsperiode.id }
            trygdeavgiftsperiodeReplika.grunnlagInntekstperiode =
                inntektsperioderReplika.find { it.id == trygdeavgiftsperiodeOriginal.grunnlagInntekstperiode.id }
            trygdeavgiftsperiodeReplika.grunnlagSkatteforholdTilNorge =
                skatteforholdTilNorgeReplika.find { it.id == trygdeavgiftsperiodeOriginal.grunnlagSkatteforholdTilNorge.id }

            trygdeavgiftsperiodeReplika.grunnlagMedlemskapsperiode.trygdeavgiftsperioder.add(trygdeavgiftsperiodeReplika)
        }

        behandlingsresultatReplika.trygdeavgiftsperioder.onEach {
            it.id = null
            it.grunnlagInntekstperiode.id = null
            it.grunnlagMedlemskapsperiode.id = null
            it.grunnlagSkatteforholdTilNorge.id = null
        }
    }

    @Throws(
        InvocationTargetException::class,
        NoSuchMethodException::class,
        InstantiationException::class,
        IllegalAccessException::class
    )
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

    @Throws(
        InvocationTargetException::class,
        NoSuchMethodException::class,
        InstantiationException::class,
        IllegalAccessException::class
    )
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

    @Throws(
        IllegalAccessException::class,
        InstantiationException::class,
        InvocationTargetException::class,
        NoSuchMethodException::class
    )
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

    @Throws(
        InvocationTargetException::class,
        NoSuchMethodException::class,
        InstantiationException::class,
        IllegalAccessException::class
    )
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

}
