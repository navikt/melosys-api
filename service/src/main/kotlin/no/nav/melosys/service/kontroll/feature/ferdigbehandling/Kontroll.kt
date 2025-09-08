package no.nav.melosys.service.kontroll.feature.ferdigbehandling

import mu.KotlinLogging
import no.nav.melosys.domain.Aktoer
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument
import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.person.Persondata
import no.nav.melosys.service.LovvalgsperiodeService
import no.nav.melosys.service.avgift.TrygdeavgiftMottakerService
import no.nav.melosys.service.avgift.TrygdeavgiftService
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.brev.UtkastBrevService
import no.nav.melosys.service.ftrl.medlemskapsperiode.MedlemskapsperiodeService
import no.nav.melosys.service.helseutgiftdekkesperiode.HelseutgiftDekkesPeriodeService
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.data.*
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.kontroll.FerdigbehandlingKontrollsett.hentRegelsettForAvslagOgHenleggelse
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.kontroll.FerdigbehandlingKontrollsett.hentRegelsettForEøsPensjonist
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.kontroll.FerdigbehandlingKontrollsett.hentRegelsettForVedtak
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.registeropplysninger.OrganisasjonOppslagService
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler
import no.nav.melosys.service.validering.Kontrollfeil
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger { }

@Component
class Kontroll(
    private val behandlingService: BehandlingService,
    private val lovvalgsperiodeService: LovvalgsperiodeService,
    private val avklarteVirksomheterService: AvklarteVirksomheterService,
    private val persondataFasade: PersondataFasade,
    private val organisasjonOppslagService: OrganisasjonOppslagService,
    private val saksbehandlingRegler: SaksbehandlingRegler,
    private val medlemskapsperiodeService: MedlemskapsperiodeService,
    private val utkastBrevService: UtkastBrevService,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val trygdeavgiftService: TrygdeavgiftService,
    private val trygdeavgiftMottakerService: TrygdeavgiftMottakerService,
    private val helseutgiftDekkesPeriodeService: HelseutgiftDekkesPeriodeService,
) {
    internal fun kontroller(
        behandlingId: Long,
        behandlingsresultattype: Behandlingsresultattyper?,
        kontrollerSomSkalIgnoreres: Set<Kontroll_begrunnelser>
    ): Collection<Kontrollfeil> {
        val behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingId)
        val sakstype = behandling.fagsak.type

        if (behandling.erEøsPensjonist())
            return kontrollerBrev(behandling)

        return kontrollerVedtak(behandlingId, sakstype, behandlingsresultattype, kontrollerSomSkalIgnoreres)
    }

    internal fun kontrollerBrev(behandling: Behandling): Collection<Kontrollfeil> = utførKontroller(behandling)

    internal fun kontrollerVedtak(
        behandlingID: Long,
        sakstype: Sakstyper,
        behandlingsresultattype: Behandlingsresultattyper?,
        kontrollerSomSkalIgnoreres: Set<Kontroll_begrunnelser>
    ): Collection<Kontrollfeil> =
        utførKontroller(behandlingID, sakstype, behandlingsresultattype).filter { skalViseFeil(it, kontrollerSomSkalIgnoreres, behandlingID) }


    private fun skalViseFeil(
        kontrollfeil: Kontrollfeil,
        kontrollerSomSkalIgnoreres: Set<Kontroll_begrunnelser> = emptySet(),
        behandlingID: Long
    ): Boolean {
        if (kontrollfeil.kode in kontrollerSomSkalIgnoreres) {
            log.info("Ignorerer kontroll ${kontrollfeil.kode.kode} for behandling $behandlingID")
            return false
        }
        return true
    }

    private fun utførKontroller(
        behandlingID: Long,
        sakstype: Sakstyper,
        behandlingsresultattype: Behandlingsresultattyper?
    ): Collection<Kontrollfeil> {
        val behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingID)

        if (behandlingsresultattype in listOf(Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL, Behandlingsresultattyper.HENLEGGELSE)) {
            return utførKontrollerForAvslagOgHenleggelse(behandling)
        }

        return utførKontroller(behandling, sakstype)
    }

    private fun utførKontrollerForAvslagOgHenleggelse(behandling: Behandling): Collection<Kontrollfeil> {
        val ferdigbehandlingKontrollData = hentKontrollDataForAvslagOgHenleggelse(behandling)
        return hentRegelsettForAvslagOgHenleggelse().mapNotNull { it.apply(ferdigbehandlingKontrollData) }
    }

    private fun utførKontroller(behandling: Behandling, sakstype: Sakstyper): Collection<Kontrollfeil> {
        val regelsettForVedtak = hentRegelsettForVedtak(
            sakstype = sakstype,
            harRegistreringUnntakFraMedlemskapFlyt = saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(behandling),
            harIkkeYrkesaktivFlyt = saksbehandlingRegler.harIkkeYrkesaktivFlyt(behandling)
        )

        val ferdigbehandlingKontrollData =
            if (sakstype == Sakstyper.FTRL) hentVedtakKontrollDataFTRL(behandling) else hentVedtakKontrollData(behandling)

        return regelsettForVedtak.mapNotNull { it.apply(ferdigbehandlingKontrollData) }
    }

    private fun utførKontroller(behandling: Behandling): Collection<Kontrollfeil> {
        return hentRegelsettForEøsPensjonist().mapNotNull {
            it.apply(hentBrevKontrollData(behandling))
        }
    }

    private fun hentKontrollDataForAvslagOgHenleggelse(behandling: Behandling): FerdigbehandlingKontrollData {
        val fullmektig = behandling.fagsak.finnFullmektig(Fullmaktstype.FULLMEKTIG_SØKNAD)
        val mottatteOpplysningerData =
            if (!saksbehandlingRegler.harIngenFlyt(behandling)) behandling.mottatteOpplysninger!!.mottatteOpplysningerData else null

        return FerdigbehandlingKontrollData(
            persondata = hentPersondata(behandling),
            mottatteOpplysningerData = mottatteOpplysningerData,
            saksopplysningerData = hentSaksopplysningerData(behandling),
            fullmektig = fullmektig,
            organisasjonDokument = hentOrganisasjonFullmektig(fullmektig),
            persondataTilFullmektig = hentPersondataFullmektig(fullmektig),
            brevUtkast = utkastBrevService.hentUtkast(behandling.id)
        )
    }

    private fun hentVedtakKontrollData(behandling: Behandling): FerdigbehandlingKontrollData {
        val fullmektig = behandling.fagsak.finnFullmektig(Fullmaktstype.FULLMEKTIG_SØKNAD)

        return FerdigbehandlingKontrollData(
            medlemskapDokument = behandling.hentMedlemskapDokument(),
            persondata = hentPersondata(behandling),
            mottatteOpplysningerData = behandling.mottatteOpplysninger!!.mottatteOpplysningerData,
            lovvalgsperiode = lovvalgsperiodeService.hentLovvalgsperiode(behandling.id),
            opprinneligLovvalgsperiode = lovvalgsperiodeService.finnOpprinneligLovvalgsperiode(behandling.id),
            saksopplysningerData = hentSaksopplysningerData(behandling),
            behandlingstema = behandling.tema,
            fullmektig = fullmektig,
            organisasjonDokument = hentOrganisasjonFullmektig(fullmektig),
            persondataTilFullmektig = hentPersondataFullmektig(fullmektig),
            brevUtkast = utkastBrevService.hentUtkast(behandling.id),
            antallArbeidsgivere = avklarteVirksomheterService.hentAntallAvklarteVirksomheter(behandling)
        )
    }

    private fun hentVedtakKontrollDataFTRL(behandling: Behandling): FerdigbehandlingKontrollData {
        val fullmektig = behandling.fagsak.finnFullmektig(Fullmaktstype.FULLMEKTIG_SØKNAD)
        val medlemskapsperioder = medlemskapsperiodeService.hentMedlemskapsperioder(behandling.id)
        val tidligereMedlemskapsperioder = behandling.fagsak.hentInaktiveBehandlinger()
            .map { medlemskapsperiodeService.hentMedlemskapsperioder(it.id) }.flatten()
        val medlemskapsdokument = behandling.hentMedlemskapDokument()

        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.id)
        val nyeTrygdeavgiftsperioder = behandlingsresultat.trygdeavgiftsperioder.toList()
        val tidligereTrygdeavgiftsperioderIAndreFagsaker = hentTidligereTrygdeavgiftsperioderIAndreFagsaker(behandling)

        val trygdeavgiftMottaker = trygdeavgiftMottakerService.getTrygdeavgiftMottaker(behandlingsresultat)
        val fullmektigSomBetalerTrygdeavgift = behandling.fagsak.finnFullmektig(Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT)

        return FerdigbehandlingKontrollData(
            medlemskapDokument = medlemskapsdokument,
            persondata = hentPersondata(behandling),
            mottatteOpplysningerData = behandling.mottatteOpplysninger!!.mottatteOpplysningerData,
            fullmektig = fullmektig,
            organisasjonDokument = hentOrganisasjonFullmektig(fullmektig),
            persondataTilFullmektig = hentPersondataFullmektig(fullmektig),
            medlemskapsperiodeData = MedlemskapsperiodeData(
                medlemskapsperioder,
                tidligereMedlemskapsperioder,
            ),
            brevUtkast = utkastBrevService.hentUtkast(behandling.id),
            trygdeavgiftperiodeData = TrygdeavgiftsperiodeData(
                nyeTrygdeavgiftsperioder,
                tidligereTrygdeavgiftsperioderIAndreFagsaker
            ),
            trygdeavgiftMottaker = trygdeavgiftMottaker,
            fullmektigSomBetalerTrygdeavgift = fullmektigSomBetalerTrygdeavgift,
            trygdeavgiftsperioderTidligereBehandling = hentTrygdeavgiftsperioderFraTidligereBehandling(behandling),
            behandlingstyper = behandling.type,
            harFattetÅrsavregningPåSak = harFattetÅrsavregning(behandling)
        )
    }

    private fun hentBrevKontrollData(behandling: Behandling): FerdigbehandlingKontrollData {
        val fullmektig = behandling.fagsak.finnFullmektig(Fullmaktstype.FULLMEKTIG_SØKNAD)
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.id)
        val medlemskapsDokument = behandling.hentMedlemskapDokument()

        val helseutgiftDekkesPeriode = helseutgiftDekkesPeriodeService.hentHelseutgiftDekkesPeriode(behandling.id)

        val tidligereHelseutgiftDekkesPerioder = hentTidligereHelseutgiftDekkesPerioderIAndreFagsaker(behandling)

        val nyeTrygdeavgiftsperioder = behandlingsresultat.eøsPensjonistTrygdeavgiftsperioder.toList()
        val tidligereEøsPensjonistTrygdeavgiftsperioderIAndreFagsaker = hentTidligereTrygdeavgiftsperioderIAndreFagsakerForEøsPensjonistKontroll(behandling)
        val tidligereTrygdeavgiftsperioderIAndreFagsaker = hentTidligereTrygdeavgiftsperioderIAndreFagsaker(behandling)

        return FerdigbehandlingKontrollData(
            medlemskapDokument = medlemskapsDokument,
            helseutgiftDekkesPeriodeData = HelseutgiftDekkesPeriodeData(
                helseutgiftDekkesPeriode,
                tidligereHelseutgiftDekkesPerioder
            ),
            persondata = hentPersondata(behandling),
            mottatteOpplysningerData = behandling.mottatteOpplysninger!!.mottatteOpplysningerData,
            brevUtkast = utkastBrevService.hentUtkast(behandling.id),
            fullmektig = fullmektig,
            organisasjonDokument = hentOrganisasjonFullmektig(fullmektig),
            persondataTilFullmektig = hentPersondataFullmektig(fullmektig),
            trygdeavgiftperiodeData = TrygdeavgiftsperiodeData(
                nyeTrygdeavgiftsperioder,
                tidligereEøsPensjonistTrygdeavgiftsperioderIAndreFagsaker,
                tidligereTrygdeavgiftsperioderIAndreFagsaker
            ),
            trygdeavgiftsperioderTidligereBehandling = hentTrygdeavgiftsperioderFraTidligereBehandling(behandling),
            erEøsPensjonist = behandling.erEøsPensjonist()
        )
    }

    private fun harFattetÅrsavregning(behandling: Behandling): Boolean {
        return behandling.fagsak.behandlinger
            .filter { it.type == Behandlingstyper.ÅRSAVREGNING && it.erAvsluttet() }
            .map { behandlingsresultatService.hentBehandlingsresultat(it.id) }
            .any { it.type == Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT }
    }

    private fun hentTrygdeavgiftsperioderFraTidligereBehandling(behandling: Behandling): List<Trygdeavgiftsperiode> {
        return behandling.opprinneligBehandling
            ?.let { behandlingsresultatService.hentBehandlingsresultat(it.id).trygdeavgiftsperioder.toList() } ?: emptyList()
    }

    private fun hentTidligereTrygdeavgiftsperioderIAndreFagsaker(
        behandling: Behandling,
        hentEøsPensjonistTrygdeavgiftsperioder: Boolean = false
    ): List<Trygdeavgiftsperiode> {
        val aktørId = behandling.fagsak.hentBrukersAktørID()
        val erEøsPensjonist = behandling.erEøsPensjonist()

        return behandlingsresultatService.finnAlleBehandlingsresultatForAktør(aktørId)
            .filter { tidligereResultat ->
                tidligereResultat.behandling.fagsak.saksnummer != behandling.fagsak.saksnummer
            }
            .filter { tidligereResultat ->
                trygdeavgiftService.harFakturerbarTrygdeavgift(
                    tidligereResultat,
                )
            }
            .flatMap {
                when {
                    erEøsPensjonist && !hentEøsPensjonistTrygdeavgiftsperioder ->
                        it.eøsPensjonistTrygdeavgiftsperioder

                    else -> it.trygdeavgiftsperioder
                }
            }

    }

    private fun hentTidligereTrygdeavgiftsperioderIAndreFagsakerForEøsPensjonistKontroll(behandling: Behandling): List<Trygdeavgiftsperiode> {
        return hentTidligereTrygdeavgiftsperioderIAndreFagsaker(behandling, hentEøsPensjonistTrygdeavgiftsperioder = behandling.erEøsPensjonist())
    }

    private fun hentTidligereHelseutgiftDekkesPerioderIAndreFagsaker(behandling: Behandling): List<HelseutgiftDekkesPeriode> {
        val tidligereBehandlingsResultat = behandlingsresultatService.finnAlleBehandlingsresultatForAktør(
            behandling.fagsak.hentBrukersAktørID()
        )

        return tidligereBehandlingsResultat
            .filter { it.behandling.fagsak.saksnummer != behandling.fagsak.saksnummer }
            .mapNotNull { it.helseutgiftDekkesPeriode }
    }

    private fun hentPersondata(behandling: Behandling): Persondata = persondataFasade.hentPerson(behandling.fagsak.hentBrukersAktørID())

    private fun hentPersondataFullmektig(fullmektig: Aktoer?): Persondata? =
        if (fullmektig != null && fullmektig.erPerson()) persondataFasade.hentPerson(fullmektig.personIdent) else null

    private fun hentOrganisasjonFullmektig(fullmektig: Aktoer?): OrganisasjonDokument? =
        if (fullmektig != null && fullmektig.erOrganisasjon()) organisasjonOppslagService.hentOrganisasjon(fullmektig.orgnr) else null

    private fun hentSaksopplysningerData(behandling: Behandling): SaksopplysningerData =
        SaksopplysningerData(avklarteVirksomheterService.harOpphørtAvklartVirksomhet(behandling))

}
