package no.nav.melosys.tjenester.gui.brev

import io.getunleash.Unleash
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.featuretoggle.ToggleName.MELOSYS_TRYGDEAVTALE_KOREA
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.brev.BrevAdresse
import no.nav.melosys.service.brev.BrevmalListeService
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler
import no.nav.melosys.tjenester.gui.brev.BrevFelt.FELT_DISTRIBUSJONSTYPE
import no.nav.melosys.tjenester.gui.brev.BrevFelt.FELT_DOKUMENT_TITTEL
import no.nav.melosys.tjenester.gui.brev.BrevFelt.FELT_FRITEKST
import no.nav.melosys.tjenester.gui.brev.BrevFelt.FELT_FRITEKSTVEDLEGG
import no.nav.melosys.tjenester.gui.brev.BrevFelt.FELT_MANGLER_FRITEKST
import no.nav.melosys.tjenester.gui.brev.BrevFelt.FELT_STANDARDTEKST_SJEKKBOKS
import no.nav.melosys.tjenester.gui.brev.BrevFelt.FELT_VEDLEGG
import no.nav.melosys.tjenester.gui.brev.BrevFelt.lagBrevTittelFelt
import no.nav.melosys.tjenester.gui.brev.BrevFelt.lagErstatterStandardtekstRadioFritekst
import no.nav.melosys.tjenester.gui.brev.BrevFelt.lagUtenlandskTrygdemyndighetMottakerFelt
import no.nav.melosys.tjenester.gui.dto.brev.*
import org.springframework.stereotype.Component

@Component
class BrevmalListeBygger(
    private val brevmalListeService: BrevmalListeService,
    private val behandlingService: BehandlingService,
    private val saksbehandlingRegler: SaksbehandlingRegler,
    private val utenlandskMyndighetService: UtenlandskMyndighetService,
    private val unleash: Unleash
) {
    fun byggBrevmalDtoListe(behandlingId: Long): List<BrevmalResponse> =
        hentTilgjengeligeMottakere(behandlingId).map { mottakerTilBrevmalDto(behandlingId, it) }

    private fun mottakerTilBrevmalDto(behandlingID: Long, mottaker: MottakerDto): BrevmalResponse {
        val produserbareDokumenter = brevmalListeService.hentMuligeProduserbaredokumenter(behandlingID, mottaker.rolle)
        val typer = produserbareDokumenter.mapNotNull {
            when (it) {
                Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD, Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE ->
                    lagBrevmalForMELDING_FORVENTET_SAKSBEHANDLINGSTID(it)

                Produserbaredokumenter.MANGELBREV_BRUKER, Produserbaredokumenter.MANGELBREV_ARBEIDSGIVER ->
                    lagBrevmalForMANGELBREV(it, behandlingID)

                Produserbaredokumenter.INNHENTING_AV_INNTEKTSOPPLYSNINGER ->
                    lagBrevmalForINNHENTING_AV_INNTEKTSOPPLYSNINGER(it)

                Produserbaredokumenter.GENERELT_FRITEKSTBREV_BRUKER,
                Produserbaredokumenter.GENERELT_FRITEKSTBREV_ARBEIDSGIVER,
                Produserbaredokumenter.GENERELT_FRITEKSTBREV_VIRKSOMHET ->
                    lagBrevmalForGENERELT_FRITEKSTBREV(it, behandlingID)

                Produserbaredokumenter.UTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV ->
                    lagBrevmalForUTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV(it, behandlingID)

                Produserbaredokumenter.FRITEKSTBREV -> lagBrevmalForFRITEKSTBREV(it)
                else -> null
            }
        }
        return BrevmalResponse(mottaker, typer)
    }

    private fun hentTilgjengeligeMottakere(behandlingId: Long): List<MottakerDto> {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val fagsak = behandling.fagsak
        val mottakere: MutableList<MottakerDto> = ArrayList()
        when (fagsak.hovedpartRolle) {
            Aktoersroller.BRUKER -> {
                mottakere.add(
                    lagMottakerMedAdresseOgFeilmelding(
                        behandlingId,
                        Mottakerroller.BRUKER,
                        fagsak.harBrukerFullmektig()
                    )
                )
                if (!saksbehandlingRegler.harIngenFlyt(behandling)) {
                    mottakere.add(lagMottakerMedAdresseOgFeilmelding(behandlingId, Mottakerroller.ARBEIDSGIVER, false))
                }
                if (fagsak.erSakstypeTrygdeavtale() || fagsak.erSakstypeEøs()) {
                    mottakere.add(lagMottakerForUtenlandskTrygdemyndighet(behandling, fagsak.erSakstypeTrygdeavtale()))
                }
                mottakere.add(lagMottakerMedRolle(Mottakerroller.ANNEN_ORGANISASJON))
                mottakere.add(lagMottakerMedRolle(Mottakerroller.NORSK_MYNDIGHET))
            }

            Aktoersroller.VIRKSOMHET -> {
                mottakere.add(lagMottakerMedAdresseOgFeilmelding(behandlingId, Mottakerroller.VIRKSOMHET, false))
                mottakere.add(lagMottakerMedRolle(Mottakerroller.ANNEN_ORGANISASJON))
            }

            else -> throw FunksjonellException("Sak må ha hovedpart for å kunne sende brev")
        }
        return mottakere
    }

    private fun lagMottakerMedAdresseOgFeilmelding(
        behandlingId: Long,
        rolle: Mottakerroller,
        harBrukerFullmektig: Boolean
    ): MottakerDto {
        val mottakerDto = lagMottakerMedRolle(rolle)
        if (harBrukerFullmektig) {
            leggTilAdresseOgFeilmelding(mottakerDto, Mottakerroller.FULLMEKTIG, behandlingId)
        } else {
            leggTilAdresseOgFeilmelding(mottakerDto, rolle, behandlingId)
        }
        return mottakerDto
    }

    private fun lagMottakerMedRolle(mottakerrolle: Mottakerroller): MottakerDto = MottakerDto().apply {
        rolle = mottakerrolle
        type = hentTypeFraRolle(mottakerrolle)
    }

    private fun lagMottakerForUtenlandskTrygdemyndighet(
        behandling: Behandling,
        erSakstypeTrygdeavtale: Boolean
    ): MottakerDto {
        val mottakerDto = lagMottakerMedRolle(Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET)
        if (erSakstypeTrygdeavtale && !saksbehandlingRegler.harIngenFlyt(behandling) && !behandling.harLand()) {
            mottakerDto.feilmelding =
                FeilmeldingDto(UTENLANDSK_TRYGDEMYNDIGHET_BEHANDLING_MANGLER_LAND)
        }
        return mottakerDto
    }

    private fun hentTypeFraRolle(rolle: Mottakerroller): String = when (rolle) {
        Mottakerroller.BRUKER -> MottakerType.BRUKER_ELLER_BRUKERS_FULLMEKTIG
        Mottakerroller.VIRKSOMHET -> MottakerType.VIRKSOMHET
        Mottakerroller.ARBEIDSGIVER -> MottakerType.ARBEIDSGIVER_ELLER_ARBEIDSGIVERS_FULLMEKTIG
        Mottakerroller.ANNEN_ORGANISASJON -> MottakerType.ANNEN_ORGANISASJON
        Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET -> MottakerType.UTENLANDSK_TRYGDEMYNDIGHET
        Mottakerroller.NORSK_MYNDIGHET -> MottakerType.NORSK_MYNDIGHET
        else -> throw FunksjonellException("Vi støtter ikke brev med mottakerrolle: ${rolle.kode}")
    }.beskrivelse

    private fun leggTilAdresseOgFeilmelding(mottakerDto: MottakerDto, rolle: Mottakerroller, behandlingId: Long) {
        try {
            val brevAdresser = brevmalListeService.hentBrevAdresseTilMottakere(behandlingId, rolle)
            if (brevAdresser.all(BrevAdresse::ugyldig)) {
                when (rolle) {
                    Mottakerroller.BRUKER -> {
                        val feilmelding =
                            Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE_BRUKER.beskrivelse.replace(
                                "Ingen gyldig adresse funnet. ",
                                ""
                            )
                        mottakerDto.feilmelding =
                            FeilmeldingDto(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE, feilmelding)
                    }

                    Mottakerroller.FULLMEKTIG -> {
                        val feilmelding =
                            Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE_REPRESENTANT.beskrivelse.replace(
                                "\"Ingen gyldig adresse funnet. ",
                                ""
                            )
                        mottakerDto.feilmelding =
                            FeilmeldingDto(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE, feilmelding)
                    }

                    Mottakerroller.VIRKSOMHET -> mottakerDto.feilmelding =
                        FeilmeldingDto(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE)

                    Mottakerroller.ARBEIDSGIVER -> mottakerDto.feilmelding =
                        FeilmeldingDto(ARBEIDSGIVER_MANGLER_ADRESSE)

                    else -> throw FunksjonellException("Vi har ikke støtte for tom adresse for $rolle")
                }
            } else {
                mottakerDto.adresser = brevAdresser
            }
        } catch (e: TekniskException) {
            if ("Finner ikke arbeidsforholddokument" == e.message) {
                mottakerDto.feilmelding = FeilmeldingDto(Kontroll_begrunnelser.INGEN_ARBEIDSGIVERE)
            } else if (rolle == Mottakerroller.ARBEIDSGIVER) {
                mottakerDto.feilmelding = FeilmeldingDto(ARBEIDSGIVER_MANGLER_ADRESSE)
            } else {
                mottakerDto.feilmelding = FeilmeldingDto(e.message)
            }
        }
    }

    private fun lagBrevmalForMELDING_FORVENTET_SAKSBEHANDLINGSTID(produserbartdokument: Produserbaredokumenter): BrevmalTypeDto {
        return BrevmalTypeDto.Builder().medType(produserbartdokument).build()
    }

    private fun lagBrevmalForMANGELBREV(
        produserbartdokument: Produserbaredokumenter,
        behandlingId: Long
    ): BrevmalTypeDto {
        val brevmalFeltDto: BrevmalFeltDto
        val behandling = behandlingService.hentBehandling(behandlingId)
        brevmalFeltDto = if (harStandardTekstIMangelbrev(behandling)) {
            lagErstatterStandardtekstRadioFritekst(FeltvalgAlternativDto(FeltvalgAlternativKode.STANDARD))
        } else {
            lagErstatterStandardtekstRadioFritekst()
        }
        return BrevmalTypeDto.Builder()
            .medType(produserbartdokument)
            .medFelter(
                listOf(
                    brevmalFeltDto,
                    FELT_MANGLER_FRITEKST
                )
            )
            .build()
    }

    private fun harStandardTekstIMangelbrev(behandling: Behandling): Boolean {
        return behandling.fagsak.tema == Sakstemaer.MEDLEMSKAP_LOVVALG && behandling.type == Behandlingstyper.FØRSTEGANG
    }

    private fun lagBrevmalForINNHENTING_AV_INNTEKTSOPPLYSNINGER(produserbartDokument: Produserbaredokumenter): BrevmalTypeDto { //TODO FIKS HER
        /*val feltvalgAlternativ = mutableListOf(
            FeltvalgAlternativDto(
                FeltvalgAlternativKode.STANDARD.kode,
                FeltvalgAlternativKode.STANDARD.beskrivelse,
                false
                )
        )*/
        val feltvalgAlternativ2 = mutableListOf(
            FeltvalgAlternativDto(
                FeltvalgAlternativKode.FRITEKST.kode,
                FeltvalgAlternativKode.FRITEKST.beskrivelse,
                true
            )
        )
        val felt1 = BrevmalFeltDto.Builder()
            .medKodeOgBeskrivelse(BrevmalFeltKode.FRITEKST)
            .medFeltType(FeltType.FRITEKST)
            .medValg(FeltValgDto(feltvalgAlternativ2, FeltValgType.CHECKBOX))
            .build()
        val felt2 = BrevmalFeltDto.Builder()
            .medKodeOgBeskrivelse(BrevmalFeltKode.STANDARDTEKST)
            .medFeltType(FeltType.SJEKKBOKS)
            .build()
        return BrevmalTypeDto.Builder()
            .medType(produserbartDokument)
            .medFelter(
                listOf(
                    felt1,
                    felt2
                )
            )
            .build()
    }

    private fun lagBrevmalForGENERELT_FRITEKSTBREV(
        produserbartdokument: Produserbaredokumenter,
        behandlingId: Long
    ): BrevmalTypeDto {
        return BrevmalTypeDto.Builder()
            .medType(produserbartdokument)
            .medFelter(
                listOf(
                    FELT_DISTRIBUSJONSTYPE,
                    lagBrevTittelFelt(hentBrevTittelValg(behandlingId)),
                    FELT_DOKUMENT_TITTEL,
                    FELT_STANDARDTEKST_SJEKKBOKS,
                    FELT_FRITEKST,
                    FELT_VEDLEGG,
                    FELT_FRITEKSTVEDLEGG
                )
            )
            .build()
    }

    private fun lagBrevmalForFRITEKSTBREV(produserbartdokument: Produserbaredokumenter): BrevmalTypeDto {
        return BrevmalTypeDto.Builder()
            .medType(produserbartdokument)
            .medFelter(
                listOf(
                    FELT_DISTRIBUSJONSTYPE,
                    lagBrevTittelFelt(hentBrevTittelValg()),
                    FELT_DOKUMENT_TITTEL,
                    FELT_FRITEKST,
                    FELT_VEDLEGG,
                    FELT_FRITEKSTVEDLEGG
                )
            )
            .build()
    }

    private fun lagBrevmalForUTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV(
        produserbartdokument: Produserbaredokumenter,
        behandlingId: Long
    ): BrevmalTypeDto {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val fagsak = behandling.fagsak
        val felter = mutableListOf(
            FELT_DISTRIBUSJONSTYPE,
            lagBrevTittelFelt(hentBrevTittelValg(behandlingId)),
            FELT_DOKUMENT_TITTEL,
            FELT_FRITEKST,
            FELT_VEDLEGG,
            FELT_FRITEKSTVEDLEGG
        )
        if (fagsak.erSakstypeEøs() || saksbehandlingRegler.harIngenFlyt(behandling)) {
            val brevmalFeltDto =
                lagUtenlandskTrygdemyndighetMottakerFelt(hentUtenlandskMyndighetMottakerValg(fagsak.erSakstypeEøs()))
            felter.add(0, brevmalFeltDto)
        }
        return BrevmalTypeDto.Builder()
            .medType(produserbartdokument)
            .medFelter(felter)
            .build()
    }

    private fun hentUtenlandskMyndighetMottakerValg(sakstypeErEøs: Boolean): FeltValgDto {
        if (sakstypeErEøs) {
            val utenlandskMyndighetFærøyene = utenlandskMyndighetService.hentUtenlandskMyndighet(Land_iso2.FO)
            val utenlandskMyndighetGrønland = utenlandskMyndighetService.hentUtenlandskMyndighet(Land_iso2.GL)
            return FeltValgDto(
                listOf(
                    FeltvalgAlternativDto(
                        utenlandskMyndighetFærøyene.hentInstitusjonID(),
                        "Trygdemyndighetene i ${utenlandskMyndighetFærøyene.landkode.beskrivelse}", true
                    ),
                    FeltvalgAlternativDto(
                        utenlandskMyndighetGrønland.hentInstitusjonID(),
                        "Trygdemyndighetene i ${utenlandskMyndighetGrønland.landkode.beskrivelse}", true
                    )
                ),
                FeltValgType.SELECT
            )
        }
        val koreaToggleEnabled = unleash.isEnabled(MELOSYS_TRYGDEAVTALE_KOREA)
        val trygdeavtaleLandkoder = Trygdeavtale_myndighetsland.values().map { it.kode }
        return FeltValgDto(
            utenlandskMyndighetService.hentAlleUtenlandskeMyndigheter()
                .filter { trygdeavtaleLandkoder.contains(it.landkode.kode) }
                .filter {
                    if (koreaToggleEnabled) return@filter true
                    it.landkode != Land_iso2.KR
                }
                .map {
                    val beskrivelse = "Trygdemyndighetene i ${it.landkode.beskrivelse}"
                    FeltvalgAlternativDto(it.hentInstitusjonID(), beskrivelse, true)
                }
                .sortedBy { it.beskrivelse },
            FeltValgType.SELECT
        )
    }

    private fun hentBrevTittelValg(behandlingId: Long): FeltValgDto {
        val fagsak = behandlingService.hentBehandling(behandlingId).fagsak
        if (fagsak.hovedpartRolle == Aktoersroller.VIRKSOMHET) {
            return FeltValgDto(
                listOf(FeltvalgAlternativDto(FeltvalgAlternativKode.FRITEKST, true)),
                FeltValgType.SELECT
            )
        }
        val valgAlternativer: MutableList<FeltvalgAlternativDto> = ArrayList()
        when (fagsak.type) {
            Sakstyper.EU_EOS -> valgAlternativer.add(FeltvalgAlternativDto(FeltvalgAlternativKode.HENVENDELSE_OM_TRYGDETILHØRLIGHET))
            Sakstyper.FTRL -> {
                valgAlternativer.add(FeltvalgAlternativDto(FeltvalgAlternativKode.CONFIRMATION_OF_MEMBERSHIP))
                valgAlternativer.add(FeltvalgAlternativDto(FeltvalgAlternativKode.BEKREFTELSE_PÅ_MEDLEMSKAP))
                valgAlternativer.add(FeltvalgAlternativDto(FeltvalgAlternativKode.HENVENDELSE_OM_MEDLEMSKAP))
            }

            Sakstyper.TRYGDEAVTALE -> valgAlternativer.add(FeltvalgAlternativDto(FeltvalgAlternativKode.ENGELSK_FRITEKSTBREV))
        }
        valgAlternativer.add(FeltvalgAlternativDto(FeltvalgAlternativKode.FRITEKST, true))
        return FeltValgDto(valgAlternativer, FeltValgType.SELECT)
    }

    companion object {
        private const val ARBEIDSGIVER_MANGLER_ADRESSE =
            "Finner ikke gyldig adresse til arbeidsgiver(e). Kontroller at arbeidsgiver(e) er lagt inn korrekt i sidemenyen"
        private const val UTENLANDSK_TRYGDEMYNDIGHET_BEHANDLING_MANGLER_LAND =
            "Du må velge land på inngangssteget for å kunne sende brev til utenlandsk trygdemyndighet."

        private fun hentBrevTittelValg(): FeltValgDto {
            return FeltValgDto(
                listOf(
                    FeltvalgAlternativDto(FeltvalgAlternativKode.ORIENTERING_BESLUTNING),
                    FeltvalgAlternativDto(FeltvalgAlternativKode.FRITEKST, true)
                ),
                FeltValgType.SELECT
            )
        }
    }
}
