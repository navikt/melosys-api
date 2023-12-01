package no.nav.melosys.service.kontroll.feature.postadresse

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Representerer
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.kontroll.KontrolldataFeilType
import no.nav.melosys.service.kontroll.regler.PersonRegler
import no.nav.melosys.service.persondata.PersondataService
import no.nav.melosys.service.registeropplysninger.OrganisasjonOppslagService
import no.nav.melosys.service.validering.Kontrollfeil
import org.springframework.stereotype.Service


@Service
class PostadresseKontrollService(
    private val behandlingService: BehandlingService,
    private val persondataService: PersondataService,
    private val organisasjonOppslagService: OrganisasjonOppslagService,
) {
    fun kontroller(kontekst: PostadressesjekkKontekst): List<Kontrollfeil> {
        kontekst.behandlingID?.let {
            val behandling = behandlingService.hentBehandling(it)
            oppdaterKontekstForBehandling(kontekst, behandling)
        }

        var harRegistrertAdresse = true
        if (!kontekst.brukerID.isNullOrBlank()) {
            val person = persondataService.hentPerson(kontekst.brukerID)
            harRegistrertAdresse = PersonRegler.harRegistrertAdresse(person)
        }

        if (!kontekst.orgnr.isNullOrBlank()) {
            val organisasjon = organisasjonOppslagService.hentOrganisasjon(kontekst.orgnr)
            harRegistrertAdresse = organisasjon.harRegistrertAdresse()
        }

        return if (harRegistrertAdresse) {
            emptyList()
        } else {
            listOf(Kontrollfeil(kontrollBegrunnelseFra(kontekst), KontrolldataFeilType.FEIL))
        }
    }

    private fun oppdaterKontekstForBehandling(kontekst: PostadressesjekkKontekst, behandling: Behandling) {
        val fullmektigForBruker = behandling.fagsak.finnRepresentantEllerFullmektig(Representerer.BRUKER)
        if (fullmektigForBruker.isPresent) {
            kontekst.oppdaterForFullmektigTilBruker(fullmektigForBruker.get())
        } else {
            kontekst.oppdaterForBruker(behandling.fagsak.hentBrukersAktørID())
        }
    }

    private fun kontrollBegrunnelseFra(kontekst: PostadressesjekkKontekst): Kontroll_begrunnelser {
        return when (kontekst.rolle) {
            Aktoersroller.REPRESENTANT, Aktoersroller.FULLMEKTIG -> {
                Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE_REPRESENTANT
            }
            else -> {
                Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE_BRUKER
            }
        }
    }
}
