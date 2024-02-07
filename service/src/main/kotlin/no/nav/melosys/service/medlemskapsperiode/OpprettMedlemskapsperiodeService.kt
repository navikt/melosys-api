package no.nav.melosys.service.medlemskapsperiode

import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.Vilkaar
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.repository.MedlemAvFolketrygdenRepository
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.behandling.UtledMottaksdato
import no.nav.melosys.service.lovligekombinasjoner.LovligeKombinasjonerMedlemskapsperiodeRegler
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
class OpprettMedlemskapsperiodeService(
    private val medlemAvFolketrygdenRepository: MedlemAvFolketrygdenRepository,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val utledMottaksdato: UtledMottaksdato,
    private val utledBestemmelserOgVilkår: UtledBestemmelserOgVilkår,
) {

    @Transactional
    fun opprettForslagPåMedlemskapsperioder(behandlingID: Long, bestemmelse: Folketrygdloven_kap2_bestemmelser?): Collection<Medlemskapsperiode> {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)
        val behandling = behandlingsresultat.behandling

        validerSakstype(behandling.fagsak)

        val søknad = behandling.mottatteOpplysninger.mottatteOpplysningerData as SøknadNorgeEllerUtenforEØS

        validerBestemmelse(bestemmelse, behandling.tema, søknad.trygdedekning)
        validerVilkår(behandlingsresultat, bestemmelse!!)

        val medlemAvFolketrygden = behandlingsresultat.medlemAvFolketrygden
            ?: MedlemAvFolketrygden().apply { this.behandlingsresultat = behandlingsresultat }

        if (medlemAvFolketrygden.medlemskapsperioder.isEmpty()) {
            val medlemskapsperioder: Collection<Medlemskapsperiode>
            val opprinneligBehandling = behandling.opprinneligBehandling

            if (behandling.erAndregangsbehandling() && opprinneligBehandling != null) {
                val opprinneligBehandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(opprinneligBehandling.id)
                medlemskapsperioder = UtledMedlemskapsperioder.lagMedlemskapsperioderForAndregangsbehandling(
                    opprinneligBehandlingsresultat,
                    bestemmelse,
                    søknad.trygdedekning,
                    behandling.type
                )
            } else {
                medlemskapsperioder = UtledMedlemskapsperioder.lagMedlemskapsperioder(
                    UtledMedlemskapsperioderDto(
                        søknad.periode,
                        søknad.trygdedekning,
                        utledMottaksdato.getMottaksdato(behandling),
                        søknad.hentArbeidsland(),
                        bestemmelse
                    )
                )
            }
            medlemAvFolketrygden.medlemskapsperioder.addAll(medlemskapsperioder)
            medlemskapsperioder.forEach { it.medlemAvFolketrygden = medlemAvFolketrygden }
        } else {
            medlemAvFolketrygden.medlemskapsperioder.forEach {
                if (!it.erOpphørt()) it.bestemmelse = bestemmelse
                it.medlemskapstype = UtledMedlemskapstype.av(bestemmelse)
            }
        }

        return medlemAvFolketrygdenRepository.save(medlemAvFolketrygden).medlemskapsperioder
    }

    private fun validerSakstype(fagsak: Fagsak) {
        if (!fagsak.erSakstypeFtrl()) {
            throw FunksjonellException("Kan ikke opprette medlemskapsperioder for sakstype ${fagsak.type}")
        }
    }

    private fun validerBestemmelse(
        bestemmelse: Folketrygdloven_kap2_bestemmelser?,
        behandlingstema: Behandlingstema,
        trygdedekning: Trygdedekninger
    ) {
        if (bestemmelse == null) {
            throw FunksjonellException("Bestemmelse er ikke satt. Krever bestemmelse ved opprettelse av forslag for medlemskapsperioder.")
        }
        val støttedeBestemmelser = hentStøttedeBestemmelserMedVilkår(behandlingstema)
        if (bestemmelse !in støttedeBestemmelser) {
            throw FunksjonellException("Støtter ikke perioder med bestemmelse $bestemmelse for behandlingstema $behandlingstema")
        }
        val lovligeBestemmelser = LovligeKombinasjonerMedlemskapsperiodeRegler.hentLovligeBestemmelser(trygdedekning)
        if (bestemmelse !in lovligeBestemmelser) {
            throw FunksjonellException("Ulovlig kombinasjon av bestemmelse $bestemmelse og trygdedekning $trygdedekning")
        }
    }

    private fun validerVilkår(behandlingsresultat: Behandlingsresultat, bestemmelse: Folketrygdloven_kap2_bestemmelser) {
        val vilkårForBestemmelse = hentVilkårForBestemmelse(bestemmelse, behandlingsresultat.behandling.tema)
        if (!behandlingsresultat.oppfyllerVilkår(vilkårForBestemmelse)) {
            throw FunksjonellException("Vilkår $vilkårForBestemmelse er påkrevd for bestemmelse $bestemmelse")
        }
    }

    fun hentStøttedeBestemmelserMedVilkår(behandlingstema: Behandlingstema): Map<Folketrygdloven_kap2_bestemmelser, Collection<Vilkaar>> =
        utledBestemmelserOgVilkår.hentStøttedeBestemmelserOgVilkår(behandlingstema)


    private fun hentVilkårForBestemmelse(bestemmelse: Folketrygdloven_kap2_bestemmelser, behandlingstema: Behandlingstema): Collection<Vilkaar> =
        hentStøttedeBestemmelserMedVilkår(behandlingstema).get(bestemmelse)
            ?: throw FunksjonellException("Finner ikke vilkår for bestemmelse $bestemmelse")
}

