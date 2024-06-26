package no.nav.melosys.service.behandling

import io.getunleash.Unleash
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.VilkaarBegrunnelse
import no.nav.melosys.domain.Vilkaarsresultat
import no.nav.melosys.domain.kodeverk.Kodeverk
import no.nav.melosys.domain.kodeverk.Vilkaar
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler
import no.nav.melosys.service.vilkaar.VilkaarDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
class VilkaarsresultatService(
    private val behandlingsresultatRepository: BehandlingsresultatRepository,
    private val saksbehandlingRegler: SaksbehandlingRegler,
    private val unleash: Unleash
) {
    private fun hentBehandlingsresultat(behandlingsid: Long): Behandlingsresultat =
        behandlingsresultatRepository.findById(behandlingsid)
            .orElseThrow { IkkeFunnetException("Kan ikke finne behandlingsresultat for behandling: $behandlingsid") }

    @Transactional(readOnly = true)
    fun hentVilkaar(behandlingID: Long): List<VilkaarDto> =
        hentBehandlingsresultat(behandlingID).vilkaarsresultater.map {
            VilkaarDto().apply {
                vilkaar = it.vilkaar.kode
                isOppfylt = it.isOppfylt
                begrunnelseKoder = it.begrunnelser.map { it.kode }.toSet()
                begrunnelseFritekst = it.begrunnelseFritekst
                begrunnelseFritekstEngelsk = it.begrunnelseFritekstEessi
            }
        }

    @Transactional(readOnly = true)
    fun finnVilkaarsresultat(behandlingID: Long, vilkaar: Vilkaar): Vilkaarsresultat? =
        hentBehandlingsresultat(behandlingID).vilkaarsresultater.firstOrNull { it.vilkaar == vilkaar }

    @Transactional(readOnly = true)
    fun finnUtsendingArbeidstakerVilkaarsresultat(behandlingID: Long): Vilkaarsresultat? {
        val utsendingsvilkår =
            if (unleash.isEnabled(ToggleName.MELOSYS_KONVENSJON_EFTA_LAND_OG_STORBRITANNIA)) listOf(
                Vilkaar.FO_883_2004_ART12_1,
                Vilkaar.KONV_EFTA_STORBRITANNIA_ART14_1,
                Vilkaar.KONV_EFTA_STORBRITANNIA_ART16_1,
            ) else listOf(
                Vilkaar.FO_883_2004_ART12_1,
            )

        return hentBehandlingsresultat(behandlingID).vilkaarsresultater.firstOrNull { it.vilkaar in utsendingsvilkår }
    }

    @Transactional(readOnly = true)
    fun finnUtsendingNæringsdrivendeVilkaarsresultat(behandlingID: Long): Vilkaarsresultat? {
        val utsendingsvilkår =
            if (unleash.isEnabled(ToggleName.MELOSYS_KONVENSJON_EFTA_LAND_OG_STORBRITANNIA)) listOf(
                Vilkaar.FO_883_2004_ART12_2,
                Vilkaar.KONV_EFTA_STORBRITANNIA_ART14_2,
                Vilkaar.KONV_EFTA_STORBRITANNIA_ART16_3,
            ) else listOf(
                Vilkaar.FO_883_2004_ART12_2
            )

        return hentBehandlingsresultat(behandlingID).vilkaarsresultater.firstOrNull { it.vilkaar in utsendingsvilkår }
    }

    @Transactional(readOnly = true)
    fun finnUnntaksVilkaarsresultat(behandlingID: Long): Vilkaarsresultat? {
        val unntaksvilkår =
            if (unleash.isEnabled(ToggleName.MELOSYS_KONVENSJON_EFTA_LAND_OG_STORBRITANNIA)) listOf(
                Vilkaar.FO_883_2004_ART16_1,
                Vilkaar.KONV_EFTA_STORBRITANNIA_ART18_1
            ) else listOf(
                Vilkaar.FO_883_2004_ART16_1
            )

        return hentBehandlingsresultat(behandlingID).vilkaarsresultater.firstOrNull { it.vilkaar in unntaksvilkår }
    }

    @Transactional(readOnly = true)
    fun oppfyllerVilkaar(behandlingID: Long, vilkaar: Vilkaar): Boolean =
        hentBehandlingsresultat(behandlingID).vilkaarsresultater.any { vilkaar == it.vilkaar && it.isOppfylt }

    @Transactional(readOnly = true)
    fun oppfyllerVilkaar(behandlingID: Long, vilkaar: List<Vilkaar>): Boolean =
        hentBehandlingsresultat(behandlingID).vilkaarsresultater.any { vilkaar.contains(it.vilkaar) && it.isOppfylt }

    @Transactional(readOnly = true)
    fun harVilkaarForUtsending(behandlingID: Long): Boolean =
        finnUtsendingArbeidstakerVilkaarsresultat(behandlingID) != null || finnUtsendingNæringsdrivendeVilkaarsresultat(behandlingID) != null

    @Transactional(readOnly = true)
    fun harVilkaarForUnntak(behandlingID: Long): Boolean = finnUnntaksVilkaarsresultat(behandlingID) != null

    @Transactional
    fun registrerVilkår(behandlingID: Long, vilkaarDtoer: List<VilkaarDto>) {
        validerVilkår(vilkaarDtoer)
        val behandlingsresultat = hentBehandlingsresultat(behandlingID)
        tilbakestillVilkårsresultatFraBehandlingsresultat(behandlingsresultat)
        // Flush fordi vi potensielt legger til samme vilkåret igjen. INSERT kommer før DELETE i Hibernate, som skaper UNIQUE constraint problemer uten flush.
        behandlingsresultatRepository.saveAndFlush(behandlingsresultat)
        for (vilkaarDto in vilkaarDtoer) {
            val vilkaarsresultat = lagVilkaarsresultat(
                behandlingsresultat,
                Vilkaar.valueOf(vilkaarDto.vilkaar),
                vilkaarDto.isOppfylt,
                vilkaarDto.begrunnelseKoder,
                vilkaarDto.begrunnelseFritekst,
                vilkaarDto.begrunnelseFritekstEngelsk
            )
            behandlingsresultat.vilkaarsresultater.add(vilkaarsresultat)
        }
        behandlingsresultatRepository.save(behandlingsresultat)
    }

    @Transactional
    fun tilbakestillVilkårsresultatFraBehandlingsresultatMedSaveAndFlush(behandlingID: Long) {
        val behandlingsresultat = hentBehandlingsresultat(behandlingID)
        tilbakestillVilkårsresultatFraBehandlingsresultat(behandlingsresultat)
        behandlingsresultatRepository.saveAndFlush(behandlingsresultat)
    }

    fun tilbakestillVilkårsresultatFraBehandlingsresultat(behandlingsresultat: Behandlingsresultat) {
        val behandling = behandlingsresultat.behandling
        if (behandling.fagsak.erSakstypeEøs() && !saksbehandlingRegler.harIngenFlyt(behandling)) {
            behandlingsresultat.vilkaarsresultater.removeIf { it.vilkaar !in IMMUTABLE_VILKAAR }
        } else {
            behandlingsresultat.vilkaarsresultater.clear()
        }
    }

    private fun validerVilkår(vilkaarDtoer: List<VilkaarDto>) {
        val nyeVilkår = vilkaarDtoer.map { it.vilkaar }
        for (immutableVilkår in IMMUTABLE_VILKAAR) {
            if (immutableVilkår.kode in nyeVilkår) {
                throw FunksjonellException("Kan ikke endre vilkår $immutableVilkår")
            }
        }
    }

    @Transactional
    fun oppdaterVilkaarsresultat(
        behandlingID: Long,
        vilkaar: Vilkaar,
        oppfylt: Boolean,
        begrunnelseKoder: Set<Kodeverk>
    ) {
        val behandlingsresultat = hentBehandlingsresultat(behandlingID)
        behandlingsresultat.vilkaarsresultater.clear()
        // Flush fordi vi potensielt legger til samme vilkåret igjen. INSERT kommer før DELETE i Hibernate, som skaper UNIQUE constraint problemer uten flush.
        behandlingsresultatRepository.saveAndFlush(behandlingsresultat)
        val vilkaarsresultat = lagVilkaarsresultat(
            behandlingsresultat,
            vilkaar,
            oppfylt,
            begrunnelseKoder.map { it.kode }.toSet()
        )
        behandlingsresultat.vilkaarsresultater.add(vilkaarsresultat)
        behandlingsresultatRepository.save(behandlingsresultat)
    }

    private fun lagVilkaarsresultat(
        behandlingsresultat: Behandlingsresultat,
        vilkaar: Vilkaar,
        oppfylt: Boolean,
        begrunnelseKoder: Set<String>,
        begrunnelseFritekst: String? = null,
        begrunnelseFritekstEngelsk: String? = null
    ): Vilkaarsresultat =
        Vilkaarsresultat().apply {
            this.behandlingsresultat = behandlingsresultat
            this.vilkaar = vilkaar
            this.isOppfylt = oppfylt
            this.begrunnelser = begrunnelseKoder.map { lagBegrunnelse(this, it) }.toSet()
            this.begrunnelseFritekst = begrunnelseFritekst
            this.begrunnelseFritekstEessi = begrunnelseFritekstEngelsk
        }

    private fun lagBegrunnelse(vilkaarsresultat: Vilkaarsresultat, begrunnelseKode: String) = VilkaarBegrunnelse().apply {
        this.vilkaarsresultat = vilkaarsresultat
        this.kode = begrunnelseKode
    }

    companion object {
        val IMMUTABLE_VILKAAR: Collection<Vilkaar> = setOf(Vilkaar.FO_883_2004_INNGANGSVILKAAR)
    }
}

