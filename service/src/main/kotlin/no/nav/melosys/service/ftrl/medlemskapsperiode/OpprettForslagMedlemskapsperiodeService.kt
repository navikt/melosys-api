package no.nav.melosys.service.ftrl.medlemskapsperiode

import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.kodeverk.Bestemmelse
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.Vilkaar
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS
import no.nav.melosys.exception.FunksjonellException
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
    private val behandlingsresultatService: BehandlingsresultatService,
    private val utledMottaksdato: UtledMottaksdato,
    private val avklartefaktaService: AvklartefaktaService,
    private val vilkårForBestemmlese: VilkårForBestemmelse,
) {

    @Transactional
    fun opprettForslagPåMedlemskapsperioder(behandlingID: Long, bestemmelse: Bestemmelse?): Collection<Medlemskapsperiode> {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)
        val behandling = behandlingsresultat.behandling

        validerSakstype(behandling.fagsak)

        val søknad = behandling.mottatteOpplysninger.mottatteOpplysningerData as SøknadNorgeEllerUtenforEØS

        validerBestemmelse(bestemmelse, søknad.trygdedekning)
        validerVilkår(behandlingsresultat, bestemmelse!!)

        if (behandlingsresultat.medlemskapsperioder.isEmpty()) {
            val medlemskapsperioder: Collection<Medlemskapsperiode>
            val opprinneligBehandling = behandling.opprinneligBehandling

            if (behandling.erAndregangsbehandling() && opprinneligBehandling != null) {
                val opprinneligeMedlemskapsperioder = behandlingsresultatService.hentBehandlingsresultat(opprinneligBehandling.id)
                    .medlemskapsperioder ?: emptyList()
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
                        bestemmelse
                    )
                )
            }
            behandlingsresultat.medlemskapsperioder.addAll(medlemskapsperioder)
            medlemskapsperioder.forEach { it.behandlingsresultat = behandlingsresultat }
        } else {
            behandlingsresultat.medlemskapsperioder.forEach {
                if (!it.erOpphørt()) it.bestemmelse = bestemmelse
                it.medlemskapstype = UtledMedlemskapstype.av(bestemmelse)
            }
        }

        return behandlingsresultatService.lagre(behandlingsresultat).medlemskapsperioder
    }

    private fun validerSakstype(fagsak: Fagsak) {
        if (!fagsak.erSakstypeFtrl()) {
            throw FunksjonellException("Kan ikke opprette medlemskapsperioder for sakstype ${fagsak.type}")
        }
    }

    private fun validerBestemmelse(bestemmelse: Bestemmelse?, trygdedekning: Trygdedekninger) {
        if (bestemmelse == null) {
            throw FunksjonellException("Bestemmelse er ikke satt. Krever bestemmelse ved opprettelse av forslag for medlemskapsperioder.")
        }

        if (!LovligeKombinasjonerTrygdedekningBestemmelse.erBestemmelseGyldigForTrygdedekning(bestemmelse, trygdedekning)) {
            throw FunksjonellException("Ulovlig kombinasjon av bestemmelse $bestemmelse og trygdedekning $trygdedekning")
        }
    }

    private fun validerVilkår(behandlingsresultat: Behandlingsresultat, bestemmelse: Bestemmelse) {
        val vilkårForBestemmelse = hentVilkårForBestemmelse(bestemmelse, behandlingsresultat.behandling.tema, behandlingsresultat.behandling.id)
        if (!behandlingsresultat.oppfyllerVilkår(vilkårForBestemmelse)) {
            throw FunksjonellException("Vilkår $vilkårForBestemmelse er påkrevd for bestemmelse $bestemmelse")
        }
    }

    private fun hentVilkårForBestemmelse(
        bestemmelse: Bestemmelse,
        behandlingstema: Behandlingstema,
        behandlingID: Long
    ): Collection<Vilkaar> {
        val avklarteFaktaMap = avklartefaktaService.hentAlleAvklarteFakta(behandlingID).filter { it.avklartefaktaType != null }
            .associate { it.avklartefaktaType to it.fakta.joinToString() }
        return vilkårForBestemmlese.hentVilkår(bestemmelse, behandlingstema, avklarteFaktaMap, behandlingID).map(Vilkår::vilkår)
    }
}

