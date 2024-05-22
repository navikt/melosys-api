package no.nav.melosys.service.behandling

import no.nav.melosys.domain.*
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.avklartefakta.Avklartefakta
import no.nav.melosys.domain.avklartefakta.AvklartefaktaRegistrering
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden
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
        val behandlingsresultatOrig: Behandlingsresultat =
            behandlingsresultatService.hentBehandlingsresultat(tidligsteInaktiveBehandling.id)

        val behandlingsresultatReplika = BeanUtils.cloneBean(behandlingsresultatOrig) as Behandlingsresultat

        behandlingsresultatReplika.behandling = behandlingReplika
        behandlingsresultatReplika.id = null
        behandlingsresultatReplika.vedtakMetadata = null
        behandlingsresultatReplika.utfallRegistreringUnntak = null
        behandlingsresultatReplika.utfallUtpeking = null
        behandlingsresultatReplika.behandlingsmåte = Behandlingsmaate.MANUELT
        behandlingsresultatReplika.type = Behandlingsresultattyper.IKKE_FASTSATT

        replikerAvklartefakta(behandlingsresultatOrig, behandlingsresultatReplika)
        replikerLovvalgsperioder(behandlingsresultatOrig, behandlingsresultatReplika)
        replikerVilkaarsresultat(behandlingsresultatOrig, behandlingsresultatReplika)
        replikerAnmodningsperioder(behandlingsresultatOrig, behandlingsresultatReplika)
        replikerBehandlingsresultatBegrunnelser(behandlingsresultatOrig, behandlingsresultatReplika)
        replikerKontrollResultater(behandlingsresultatOrig, behandlingsresultatReplika)
        replikerUtpekingsperioder(behandlingsresultatOrig, behandlingsresultatReplika)
        replikerMedlemAvFolketrygden(behandlingsresultatOrig, behandlingsresultatReplika, behandlingReplika.type)

        behandlingsresultatService.lagre(behandlingsresultatReplika)
    }

    @Throws(
        InvocationTargetException::class,
        NoSuchMethodException::class,
        InstantiationException::class,
        IllegalAccessException::class
    )
    private fun replikerMedlemAvFolketrygden(
        behandlingsresultatOrig: Behandlingsresultat,
        behandlingsresultatReplika: Behandlingsresultat,
        behandlingstype: Behandlingstyper
    ) {
        if (behandlingsresultatOrig.medlemAvFolketrygden == null) return
        val medlemAvFolketrygdenReplika = BeanUtils.cloneBean(behandlingsresultatOrig.medlemAvFolketrygden) as MedlemAvFolketrygden
        medlemAvFolketrygdenReplika.behandlingsresultat = behandlingsresultatReplika
        medlemAvFolketrygdenReplika.id = null
        behandlingsresultatReplika.medlemAvFolketrygden = medlemAvFolketrygdenReplika

        replikerMedlemskapsperioderBasertPåBehandlingstype(behandlingsresultatOrig, medlemAvFolketrygdenReplika, behandlingstype)
        replikerFastsattTrygdeavgift(behandlingsresultatOrig.medlemAvFolketrygden, medlemAvFolketrygdenReplika)

        medlemAvFolketrygdenReplika.medlemskapsperioder.onEach { it.id = null }
    }

    @Throws(
        InvocationTargetException::class,
        NoSuchMethodException::class,
        InstantiationException::class,
        IllegalAccessException::class
    )
    private fun replikerMedlemskapsperioderBasertPåBehandlingstype(
        behandlingsresultatOrig: Behandlingsresultat,
        medlemAvFolketrygdenReplika: MedlemAvFolketrygden,
        behandlingstype: Behandlingstyper
    ) {
        medlemAvFolketrygdenReplika.medlemskapsperioder = HashSet()

        val filtrertMedlemskapsperioderOriginal = if (behandlingstype == Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT) {
            behandlingsresultatOrig.medlemAvFolketrygden.medlemskapsperioder.filter { it.erInnvilget() || it.erOpphørt() }
        } else {
            behandlingsresultatOrig.medlemAvFolketrygden.medlemskapsperioder.filter { it.erInnvilget() }
        }

        for (medlemskapsperiodeOriginal in filtrertMedlemskapsperioderOriginal) {
            val medlemskapsperiodeReplika = BeanUtils.cloneBean(medlemskapsperiodeOriginal) as Medlemskapsperiode
            medlemskapsperiodeReplika.medlemAvFolketrygden = medlemAvFolketrygdenReplika
            medlemAvFolketrygdenReplika.medlemskapsperioder.add(medlemskapsperiodeReplika)
        }
    }

    @Throws(
        InvocationTargetException::class,
        NoSuchMethodException::class,
        InstantiationException::class,
        IllegalAccessException::class
    )
    private fun replikerFastsattTrygdeavgift(
        medlemAvFolketrygdenOriginal: MedlemAvFolketrygden,
        medlemAvFolketrygdenReplika: MedlemAvFolketrygden
    ) {
        if (medlemAvFolketrygdenOriginal.fastsattTrygdeavgift == null) return
        val fastsattTrygdeavgiftReplika = BeanUtils.cloneBean(medlemAvFolketrygdenOriginal.fastsattTrygdeavgift) as FastsattTrygdeavgift
        fastsattTrygdeavgiftReplika.medlemAvFolketrygden = medlemAvFolketrygdenReplika
        fastsattTrygdeavgiftReplika.id = null
        medlemAvFolketrygdenReplika.fastsattTrygdeavgift = fastsattTrygdeavgiftReplika

        replikerTrygdeavgiftsperioder(
            medlemAvFolketrygdenOriginal,
            medlemAvFolketrygdenReplika
        )
    }

    @Throws(
        InvocationTargetException::class,
        NoSuchMethodException::class,
        InstantiationException::class,
        IllegalAccessException::class
    )
    private fun replikerTrygdeavgiftsperioder(
        medlemAvFolketrygdenOriginal: MedlemAvFolketrygden,
        medlemAvFolketrygdenReplika: MedlemAvFolketrygden
    ) {
        val fastsattTrygdeavgiftReplika = medlemAvFolketrygdenReplika.fastsattTrygdeavgift
        val inntektsperioderReplika = medlemAvFolketrygdenOriginal.fastsattTrygdeavgift.hentInntektsperioder().map {
            BeanUtils.cloneBean(it) as Inntektsperiode
        }
        val skatteforholdTilNorgeReplika = medlemAvFolketrygdenOriginal.fastsattTrygdeavgift.hentSkatteforholdTilNorge().map {
            BeanUtils.cloneBean(it) as SkatteforholdTilNorge
        }

        fastsattTrygdeavgiftReplika.trygdeavgiftsperioder = HashSet()
        for (trygdeavgiftsperiodeOriginal in medlemAvFolketrygdenOriginal.fastsattTrygdeavgift.trygdeavgiftsperioder) {
            val trygdeavgiftsperiodeReplika = BeanUtils.cloneBean(trygdeavgiftsperiodeOriginal) as Trygdeavgiftsperiode
            trygdeavgiftsperiodeReplika.fastsattTrygdeavgift = fastsattTrygdeavgiftReplika

            trygdeavgiftsperiodeReplika.grunnlagMedlemskapsperiode = medlemAvFolketrygdenReplika.medlemskapsperioder
                .find { it.id == trygdeavgiftsperiodeOriginal.grunnlagMedlemskapsperiode.id }
            trygdeavgiftsperiodeReplika.grunnlagInntekstperiode =
                inntektsperioderReplika.find { it.id == trygdeavgiftsperiodeOriginal.grunnlagInntekstperiode.id }
            trygdeavgiftsperiodeReplika.grunnlagSkatteforholdTilNorge =
                skatteforholdTilNorgeReplika.find { it.id == trygdeavgiftsperiodeOriginal.grunnlagSkatteforholdTilNorge.id }

            fastsattTrygdeavgiftReplika.trygdeavgiftsperioder.add(trygdeavgiftsperiodeReplika)
        }

        fastsattTrygdeavgiftReplika.trygdeavgiftsperioder.onEach {
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
