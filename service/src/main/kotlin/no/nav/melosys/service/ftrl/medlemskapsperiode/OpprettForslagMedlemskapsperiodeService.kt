package no.nav.melosys.service.ftrl.medlemskapsperiode

import io.getunleash.Unleash
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
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.repository.MedlemAvFolketrygdenRepository
import no.nav.melosys.service.avklartefakta.AvklartefaktaService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.behandling.UtledMottaksdato
import no.nav.melosys.service.ftrl.bestemmelse.LovligeKombinasjonerTrygdedekningBestemmelse
import no.nav.melosys.service.ftrl.bestemmelse.vilkaar.Vilkår
import no.nav.melosys.service.ftrl.bestemmelse.vilkaar.VilkårForBestemmelse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
class OpprettForslagMedlemskapsperiodeService(
    private val medlemAvFolketrygdenRepository: MedlemAvFolketrygdenRepository,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val utledMottaksdato: UtledMottaksdato,
    private val utledBestemmelserOgVilkår: UtledBestemmelserOgVilkår,
    private val unleash: Unleash,
    private val avklartefaktaService: AvklartefaktaService,
    private val vilkårForBestemmlese: VilkårForBestemmelse,
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
                val opprinneligeMedlemskapsperioder = behandlingsresultatService.hentBehandlingsresultat(opprinneligBehandling.id)
                    ?.medlemAvFolketrygden?.medlemskapsperioder ?: emptyList()
                medlemskapsperioder = UtledMedlemskapsperioder.lagMedlemskapsperioderForAndregangsbehandling(
                    UtledMedlemskapsperioderDto(søknad.periode, søknad.trygdedekning, null, bestemmelse),
                    opprinneligeMedlemskapsperioder,
                    behandling.type
                )
            } else {
                medlemskapsperioder = UtledMedlemskapsperioder.lagMedlemskapsperioder(
                    UtledMedlemskapsperioderDto(
                        søknad.periode,
                        søknad.trygdedekning,
                        utledMottaksdato.getMottaksdato(behandling),
                        bestemmelse,
                    ),
                    unleash
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

        if (toggleIsEnabled(behandlingstema)) {
            if (!LovligeKombinasjonerTrygdedekningBestemmelse.erBestemmelseGyldigForTrygdedekning(bestemmelse, trygdedekning)) {
                throw FunksjonellException("Ulovlig kombinasjon av bestemmelse $bestemmelse og trygdedekning $trygdedekning")
            }
        } else {
            val støttedeBestemmelser = hentStøttedeBestemmelserMedVilkår(behandlingstema)
            if (bestemmelse !in støttedeBestemmelser) {
                throw FunksjonellException("Støtter ikke perioder med bestemmelse $bestemmelse for behandlingstema $behandlingstema")
            }

            if (!LovligeKombinasjonerTrygdedekningBestemmelse.erBestemmelseGyldigForTrygdedekning(bestemmelse, trygdedekning)) {
                throw FunksjonellException("Ulovlig kombinasjon av bestemmelse $bestemmelse og trygdedekning $trygdedekning")
            }
        }
    }

    private fun validerVilkår(behandlingsresultat: Behandlingsresultat, bestemmelse: Folketrygdloven_kap2_bestemmelser) {
        val vilkårForBestemmelse = hentVilkårForBestemmelse(bestemmelse, behandlingsresultat.behandling.tema, behandlingsresultat.behandling.id)
        if (!behandlingsresultat.oppfyllerVilkår(vilkårForBestemmelse)) {
            throw FunksjonellException("Vilkår $vilkårForBestemmelse er påkrevd for bestemmelse $bestemmelse")
        }
    }

    fun hentStøttedeBestemmelserMedVilkår(behandlingstema: Behandlingstema): Map<Folketrygdloven_kap2_bestemmelser, Collection<Vilkaar>> =
        utledBestemmelserOgVilkår.hentStøttedeBestemmelserOgVilkår(behandlingstema)


    private fun hentVilkårForBestemmelse(
        bestemmelse: Folketrygdloven_kap2_bestemmelser,
        behandlingstema: Behandlingstema,
        behandlingID: Long
    ): Collection<Vilkaar> {
        if (toggleIsEnabled(behandlingstema)) {
            val avklarteFaktaMap = avklartefaktaService.hentAlleAvklarteFakta(behandlingID).filter { it.avklartefaktaType != null }
                .associate { it.avklartefaktaType to it.fakta.joinToString() }
            return vilkårForBestemmlese.hentVilkår(bestemmelse, behandlingstema, avklarteFaktaMap, behandlingID).map(Vilkår::vilkår)
        }
        return hentStøttedeBestemmelserMedVilkår(behandlingstema).get(bestemmelse)
            ?: throw FunksjonellException("Finner ikke vilkår for bestemmelse $bestemmelse")
    }

    private fun toggleIsEnabled(behandlingstema: Behandlingstema) =
        if (behandlingstema == Behandlingstema.YRKESAKTIV) unleash.isEnabled(ToggleName.MELOSYS_FTRL_YRKESAKTIV_PLIKTIGE_BESTEMMELSER)
        else unleash.isEnabled(ToggleName.MELOSYS_FTRL_IKKE_YRKESAKTIV)

}

