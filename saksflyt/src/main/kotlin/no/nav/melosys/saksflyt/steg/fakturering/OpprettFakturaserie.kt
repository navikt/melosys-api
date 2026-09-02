package no.nav.melosys.saksflyt.steg.fakturering

import io.getunleash.Unleash
import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.Betalingstype
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.integrasjon.faktureringskomponenten.FaktureringskomponentenClient
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.*
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.avgift.TrygdeavgiftService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.service.persondata.PersondataService
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val log = KotlinLogging.logger { }

@Component
class OpprettFakturaserie(
    private val behandlingService: BehandlingService,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val faktureringskomponentenClient: FaktureringskomponentenClient,
    private val pdlService: PersondataService,
    private val trygdeavgiftService: TrygdeavgiftService,
    private val unleash: Unleash,
    private val oppgaveService: OppgaveService
) : StegBehandler {

    override fun inngangsSteg() = ProsessSteg.OPPRETT_FAKTURASERIE

    override fun utfør(prosessinstans: Prosessinstans) {
        val behandling = prosessinstans.hentBehandling
        val behandlingID = behandling.id
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)
        val erLovvalgMedTrygdeavgiftsperiode =
            behandling.fagsak.erLovvalg() && behandlingsresultat.trygdeavgiftsperioder.isNotEmpty()

        if (!erLovvalgMedTrygdeavgiftsperiode && prosessinstans.type == ProsessType.IVERKSETT_VEDTAK_EOS) {
            return
        }

        val saksbehandlerIdent = prosessinstans.hentData(ProsessDataKey.SAKSBEHANDLER)

        val fjernetFakturerbarTrygdeavgift = andregangsvurderingHarFjernetFakturerbarTrygdeavgift(behandling, behandlingsresultat)

        when {
            behandlingsresultat.erOpphørt() ->
                kansellerVedOpphør(behandling, behandlingsresultat, saksbehandlerIdent)

            fjernetFakturerbarTrygdeavgift && !erIkkeTidligerePerioderAktiv() ->
                kansellerVedBortfaltTrygdeavgift(behandling, behandlingsresultat, saksbehandlerIdent)

            skalOppretteFakturaserie(behandlingsresultat)
                || fjernetFakturerbarTrygdeavgift
                || skalAvregneInneværendeOgFremtidigePerioderTilNull(behandlingsresultat) -> {
                log.info("Oppretter fakturaserie for behandling: $behandlingID")
                opprettFakturaserieOgLagreReferanse(behandlingsresultat, mapFakturaserieDto(behandlingsresultat, prosessinstans), saksbehandlerIdent)
            }

            else -> log.info("Ingen fakturaserie opprettet for behandling: $behandlingID")
        }
    }

    private fun kansellerVedOpphør(behandling: Behandling, behandlingsresultat: Behandlingsresultat, saksbehandlerIdent: String) {
        val behandlingID = behandling.id
        val opprinneligFakturaserieReferanse =
            behandlingsresultatService.hentBehandlingsresultat(behandling.hentOpprinneligBehandling().id).hentFakturaserieReferanse()
        if (erAlleredeKansellert(behandlingsresultat, opprinneligFakturaserieReferanse)) {
            log.info("Fakturaserie allerede kansellert for behandling: $behandlingID, hopper over kansellering")
        } else {
            log.info("Kansellerer fakturaserie ved opphør for behandling: $behandlingID med fakturaseriereferanse: $opprinneligFakturaserieReferanse")
            kansellerFakturaserieOgLagreReferanse(behandlingsresultat, opprinneligFakturaserieReferanse, saksbehandlerIdent)
        }
        avsluttAktiveÅrsavregninger(behandling.fagsak)
    }

    /**
     * Andregangsbehandling der forrige behandling hadde fakturerbar trygdeavgift, men den nye vurderingen ikke har det.
     * Typisk ved overgang fra ikke skattepliktig til skattepliktig: trygdeavgiften beregnes fortsatt, men med beløp null,
     * og NAV skal ikke lenger kreve inn avgift (MELOSYS-8220).
     *
     * Brukes kun når [ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER] er av. Da kjenner Melosys alle år
     * i medlemskapet, og faktureringskomponenten avviser tom periodeliste. Kansellering krediterer alt som er fakturert,
     * inkludert årsavregninger. Med togglen på sendes i stedet tom periodeliste med forrige referanse (grenen for
     * opprettelse), og faktureringskomponenten avregner kun inneværende år og fremover.
     */
    private fun kansellerVedBortfaltTrygdeavgift(behandling: Behandling, behandlingsresultat: Behandlingsresultat, saksbehandlerIdent: String) {
        val behandlingID = behandling.id
        val opprinneligFakturaserieReferanse =
            behandlingsresultatService.hentBehandlingsresultat(behandling.hentOpprinneligBehandling().id).fakturaserieReferanse
        if (opprinneligFakturaserieReferanse == null) {
            log.info("Trygdeavgift bortfalt for behandling: $behandlingID, men opprinnelig behandling har ingen fakturaserie. Ingen kansellering")
            return
        }
        if (erAlleredeKansellert(behandlingsresultat, opprinneligFakturaserieReferanse)) {
            log.info("Fakturaserie allerede kansellert for behandling: $behandlingID, hopper over kansellering")
        } else {
            log.info("Trygdeavgift bortfalt for behandling: $behandlingID, kansellerer fakturaserie med fakturaseriereferanse: $opprinneligFakturaserieReferanse")
            kansellerFakturaserieOgLagreReferanse(behandlingsresultat, opprinneligFakturaserieReferanse, saksbehandlerIdent)
        }
        avsluttAktiveÅrsavregninger(behandling.fagsak)
    }

    /**
     * Ved rekjøring av steget er referansen allerede byttet ut med referansen fra kanselleringen.
     * Ny vurdering arver referansen fra opprinnelig behandling ved replikering, så lik referanse betyr «ikke kansellert ennå».
     */
    private fun erAlleredeKansellert(behandlingsresultat: Behandlingsresultat, opprinneligFakturaserieReferanse: String): Boolean =
        behandlingsresultat.fakturaserieReferanse != null && behandlingsresultat.fakturaserieReferanse != opprinneligFakturaserieReferanse

    private fun erIkkeTidligerePerioderAktiv(): Boolean =
        unleash.isEnabled(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)

    private fun andregangsvurderingHarFjernetFakturerbarTrygdeavgift(behandling: Behandling, behandlingsresultat: Behandlingsresultat): Boolean =
        behandling.erAndregangsbehandling()
            && harOpprinneligBehandlingFakturerbarTrygdeavgift(behandling)
            && !trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultat)

    private fun kansellerFakturaserieOgLagreReferanse(
        behandlingsresultat: Behandlingsresultat,
        opprinneligFakturaserieReferanse: String,
        saksbehandlerIdent: String
    ) {
        val alleÅrsavregningBehandlinger = behandlingsresultat.behandling?.fagsak?.hentAlleÅrsavregninger().orEmpty()
        val årsavregningRefs = alleÅrsavregningBehandlinger
            .mapNotNull { behandlingsresultatService.hentBehandlingsresultat(it.id).fakturaserieReferanse }
        val fakturaserieResponse =
            faktureringskomponentenClient.kansellerFakturaserie(opprinneligFakturaserieReferanse, saksbehandlerIdent, årsavregningRefs)
        behandlingsresultat.fakturaserieReferanse = fakturaserieResponse.fakturaserieReferanse
        behandlingsresultatService.lagre(behandlingsresultat)
    }

    private fun avsluttAktiveÅrsavregninger(fagsak: Fagsak) {
        fagsak.hentAktiveÅrsavregninger().forEach {
            log.info("Avslutter aktiv årsavregning fordi trygdeavgift til NAV bortfaller, behandlingId: ${it.id}")
            oppgaveService.ferdigstillOppgaveMedBehandlingID(it.id)
            behandlingsresultatService.oppdaterBehandlingsresultattype(it.id, Behandlingsresultattyper.FERDIGBEHANDLET)
            behandlingService.avsluttBehandling(it.id)
        }
    }

    private fun skalOppretteFakturaserie(behandlingsresultat: Behandlingsresultat): Boolean =
        trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultat)
            && skalFaktureres(behandlingsresultat)

    /**
     * Dette er et spesialtilfelle hvor førstegangsbehandlingen har fakturert, men ny vurdering setter medlemskapsperiodene
     * til kun tidligere år. Vi trenger da å avregne i faktureringskomponenten med tidligere fakturaserieref og tom periode.
     */
    private fun skalAvregneInneværendeOgFremtidigePerioderTilNull(behandlingsresultat: Behandlingsresultat): Boolean {
        if (!erIkkeTidligerePerioderAktiv()) {
            return false
        }

        return erNyVurderingUtenPerioderMedTidligereFakturering(behandlingsresultat)
            && opprinneligBehandlingHarInneværendeEllerFremtidigeAvgiftsperioder(behandlingsresultat)
    }

    private fun erNyVurderingUtenPerioderMedTidligereFakturering(behandlingsresultat: Behandlingsresultat): Boolean {
        val behandling = behandlingsresultat.hentBehandling()
        return behandling.erNyVurdering()
            && behandlingsresultat.trygdeavgiftsperioder.isEmpty()
            && hentSisteFakturaserieReferanse(behandling) != null
    }

    private fun opprinneligBehandlingHarInneværendeEllerFremtidigeAvgiftsperioder(behandlingsresultat: Behandlingsresultat): Boolean {
        val opprinneligBehandling = behandlingsresultat.behandling?.opprinneligBehandling ?: return false
        val opprinneligBehandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(opprinneligBehandling.id)

        return opprinneligBehandlingsresultat.trygdeavgiftsperioder.isNotEmpty() && opprinneligBehandlingsresultat.trygdeavgiftsperioder.any { it.periodeTil.year >= LocalDate.now().year }
    }


    private fun opprettFakturaserieOgLagreReferanse(
        behandlingsresultat: Behandlingsresultat,
        fakturaserieDto: FakturaserieDto,
        saksbehandlerIdent: String
    ) {
        val fakturaserieResponse = faktureringskomponentenClient.lagFakturaserie(fakturaserieDto, saksbehandlerIdent)
        behandlingsresultat.fakturaserieReferanse = fakturaserieResponse.fakturaserieReferanse
        behandlingsresultatService.lagre(behandlingsresultat)
    }

    private fun mapFakturaserieDto(behandlingsresultat: Behandlingsresultat, prosessinstans: Prosessinstans): FakturaserieDto {
        val behandling = behandlingService.hentBehandling(behandlingsresultat.hentId())
        val fagsak = behandling.fagsak
        val fullmektig = fagsak.finnFullmektig(Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT)
        val foedselsNr = pdlService.finnFolkeregisterident(fagsak.hentBrukersAktørID())
            .orElseThrow { FunksjonellException("Kunne ikke finne fødselsnummer fra PDL") }
        val vedtaksdato =
            DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.systemDefault()).format(behandlingsresultat.hentVedtakMetadata().vedtaksdato)
        val erEøsPensjonist = behandling.erEøsPensjonist()
        val erLovvalg = fagsak.erLovvalg()

        return FakturaserieDto(
            fodselsnummer = foedselsNr,
            fakturaserieReferanse = hentSisteFakturaserieReferanse(behandling),
            referanseNAV = "Medlemskap og avgift",
            fullmektig = FullmektigDto(fullmektig),
            fakturaGjelderInnbetalingstype = Innbetalingstype.TRYGDEAVGIFT,
            intervall = hentBetalingsIntervall(prosessinstans),
            referanseBruker = if (erEøsPensjonist) "Informasjon om trygdeavgift datert $vedtaksdato" else "Vedtak om medlemskap datert $vedtaksdato",
            perioder = mapTilFakturaperioder(
                    behandlingsresultat.trygdeavgiftsperioder.filter { it.harAvgift() },
                    inkluderDekning = !erEøsPensjonist && !erLovvalg
                )
        )
    }

    private fun skalFaktureres(behandlingsresultat: Behandlingsresultat): Boolean =
        !behandlingsresultat.hentBehandling().erPensjonist() ||
            behandlingsresultat.hentBehandling().fagsak.betalingsvalg == Betalingstype.FAKTURA


    private fun harOpprinneligBehandlingFakturerbarTrygdeavgift(behandling: Behandling): Boolean =
        behandling.opprinneligBehandling?.let {
            trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultatService.hentBehandlingsresultat(it.id))
        } ?: false

    private fun hentBetalingsIntervall(prosessinstans: Prosessinstans): FaktureringIntervall =
        prosessinstans.finnData<FaktureringIntervall>(ProsessDataKey.BETALINGSINTERVALL, FaktureringIntervall.KVARTAL)

    private fun hentSisteFakturaserieReferanse(behandling: Behandling): String? =
        behandling.fagsak.behandlinger
            .asSequence()
            .filter { it.erInaktiv() && !it.erÅrsavregning() && it.id != behandling.id }
            .map {
                behandlingsresultatService.hentBehandlingsresultat(it.id)
            }
            .sortedByDescending { it.vedtakMetadata?.vedtaksdato }
            .map { it.fakturaserieReferanse }
            .firstOrNull()

}
