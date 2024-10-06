package no.nav.melosys.itest

import io.kotest.matchers.nulls.shouldNotBeNull
import no.nav.melosys.domain.Aktoer
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.RegistreringsInfo
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.repository.BehandlingRepository
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.service.dokument.DokgenService
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.LocalDate

internal class BrevUtkastIT(
    @Autowired private val dokgenService: DokgenService,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingRepository: BehandlingRepository,
) : ComponentTestBase() {

    @Test
    fun `Lag utkast til brev for behandling uten behandlingsårsak`() {
        val behandling = lagFagsakMedBehandling()
        val bestillingDTO = BrevbestillingDto().apply {
            produserbardokument = Produserbaredokumenter.INNVILGELSE_FOLKETRYGDLOVEN
            mottaker = Mottakerroller.BRUKER
            begrunnelseFritekst = ""
            fritekst = ""
        }

        val utkast = dokgenService.produserUtkast(behandling.id, bestillingDTO)

        utkast.shouldNotBeNull()
    }

    private fun lagFagsakMedBehandling(): Behandling {
        Fagsak("MEL-test", null, Sakstyper.TRYGDEAVTALE, Sakstemaer.MEDLEMSKAP_LOVVALG, Saksstatuser.LOVVALG_AVKLART)
            .apply {
                val sak = this
                aktører.add(Aktoer().apply {
                    fagsak = sak
                    rolle =  Aktoersroller.BRUKER
                    aktørId = "2222222222222"
                })
            }
            .apply { leggTilRegisteringInfo() }
            .also { sak ->
                fagsakRepository.save(sak)

                val behandling = Behandling().apply {
                    fagsak = sak
                    leggTilRegisteringInfo()
                    behandlingsfrist = LocalDate.now().plusYears(1)
                    status = Behandlingsstatus.AVSLUTTET
                    type = Behandlingstyper.FØRSTEGANG
                    tema = Behandlingstema.YRKESAKTIV
                }.also { behandlingRepository.save(it) }

                return behandling
            }
    }

    private fun RegistreringsInfo.leggTilRegisteringInfo() {
        registrertDato = Instant.now()
        endretDato = Instant.now()
        endretAv = "bla"
    }
}
