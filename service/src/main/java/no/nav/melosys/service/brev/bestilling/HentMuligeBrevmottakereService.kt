package no.nav.melosys.service.brev.bestilling

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.brev.Mottaker
import no.nav.melosys.domain.brev.NorskMyndighet
import no.nav.melosys.domain.brev.muligemottakere.Brevmottaker
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.integrasjon.ereg.EregFasade
import no.nav.melosys.service.aktoer.KontaktopplysningService
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.brev.DokumentNavnService
import no.nav.melosys.service.dokument.BrevmottakerService
import no.nav.melosys.service.persondata.PersondataFasade
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class HentMuligeBrevmottakereService(
    private val behandlingService: BehandlingService,
    private val brevmottakerService: BrevmottakerService,
    private val dokumentNavnService: DokumentNavnService,
    private val persondataFasade: PersondataFasade,
    private val eregFasade: EregFasade,
    private val kontaktopplysningService: KontaktopplysningService,
    private val utenlandskMyndighetService: UtenlandskMyndighetService
) {
    @Transactional
    fun hentMuligeBrevmottakere(requestDto: RequestDto): ResponseDto {
        val produserbaredokumenter = requestDto.produserbartdokument
        val mottakerliste = brevmottakerService.hentMottakerliste(produserbaredokumenter, requestDto.behandlingID)

        val behandling = behandlingService.hentBehandlingMedSaksopplysninger(requestDto.behandlingID)
        val hovedMottaker = lagHovedMottakerMuligMottakerDto(
            produserbaredokumenter, behandling, mottakerliste.hovedMottaker,
            requestDto.orgnr.orEmpty(), requestDto.institusjonID.orEmpty()
        )
        val kopiMottakere = lagKopiMottakereMuligMottakerDtos(
            produserbaredokumenter,
            behandling,
            mottakerliste.kopiMottakere,
            mottakerliste.hovedMottaker
        )
        val fasteMottakere =
            lagFasteMottakereMuligMottakerDtos(produserbaredokumenter, behandling, mottakerliste.fasteMottakere)

        return ResponseDto(hovedMottaker, kopiMottakere, fasteMottakere)
    }

    private fun lagHovedMottakerMuligMottakerDto(
        produserbaredokumenter: Produserbaredokumenter, behandling: Behandling,
        hovedmottaker: Mottakerroller, orgnrTilValgtArbeidsgiver: String,
        institusjonID: String
    ): Brevmottaker {
        return Brevmottaker.Builder()
            .medDokumentNavn(
                dokumentNavnService.utledDokumentNavnForProduserbartdokumentOgMottakerrolle(
                    behandling,
                    produserbaredokumenter,
                    hovedmottaker
                )
            )
            .medMottakerNavn(
                hentMottakerNavn(
                    produserbaredokumenter,
                    behandling,
                    hovedmottaker,
                    orgnrTilValgtArbeidsgiver,
                    institusjonID
                )
            )
            .medRolle(hovedmottaker)
            .build()
    }

    private fun lagFasteMottakereMuligMottakerDtos(
        produserbaredokumenter: Produserbaredokumenter,
        behandling: Behandling,
        fasteMottakere: Collection<NorskMyndighet>
    ): MutableList<Brevmottaker?> {
        val brevmottakere: MutableList<Brevmottaker?> = ArrayList<Brevmottaker?>()

        for (fastMottaker in fasteMottakere) {
            val avklartMottaker =
                brevmottakerService.avklarMottaker(produserbaredokumenter, Mottaker.av(fastMottaker), behandling)
            val orgDokument = hentRettOrganisasjonsdokument(behandling, avklartMottaker.orgnr)

            val fastTekst = "Kopi til " + orgDokument.navn
            brevmottakere.add(
                Brevmottaker.Builder()
                    .medDokumentNavn(
                        dokumentNavnService.utledDokumentNavnForProduserbartdokumentOgMottaker(
                            behandling,
                            produserbaredokumenter,
                            avklartMottaker,
                            fastTekst
                        )
                    )
                    .medMottakerNavn(orgDokument.navn)
                    .medRolle(avklartMottaker.rolle)
                    .medOrgnr(orgDokument.orgnummer)
                    .build()
            )
        }
        return brevmottakere
    }

    private fun lagKopiMottakerForBruker(
        produserbaredokumenter: Produserbaredokumenter?,
        behandling: Behandling,
        kopiMottaker: Mottakerroller?,
        hovedmottaker: Mottakerroller?
    ): Brevmottaker {
        val avklartKopi =
            brevmottakerService.avklarMottaker(produserbaredokumenter, Mottaker.medRolle(kopiMottaker), behandling)
        if (avklartKopi.rolle == Mottakerroller.BRUKER || hovedmottaker == kopiMottaker) {
            val aktørID = behandling.fagsak.hentBrukersAktørID()
            return Brevmottaker.Builder()
                .medDokumentNavn("Kopi til bruker")
                .medMottakerNavn(persondataFasade.hentSammensattNavn(aktørID))
                .medRolle(Mottakerroller.BRUKER)
                .medAktørId(aktørID)
                .build()
        } else if (avklartKopi.personIdent != null) {
            return Brevmottaker.Builder()
                .medDokumentNavn("Kopi til brukers fullmektig")
                .medMottakerNavn(persondataFasade.hentSammensattNavn(avklartKopi.personIdent))
                .medRolle(avklartKopi.rolle)
                .build()
        } else {
            val orgDokument = hentRettOrganisasjonsdokument(behandling, avklartKopi.orgnr)
            return Brevmottaker.Builder()
                .medDokumentNavn("Kopi til brukers fullmektig")
                .medMottakerNavn(orgDokument.navn)
                .medRolle(avklartKopi.rolle)
                .medOrgnr(orgDokument.orgnummer)
                .build()
        }
    }

    private fun hentMottakerNavn(
        produserbaredokumenter: Produserbaredokumenter?, behandling: Behandling, hovedmottaker: Mottakerroller,
        orgnr: String, institusjonID: String
    ): String? {
        when (hovedmottaker) {
            Mottakerroller.BRUKER -> {
                val avklartMottaker = brevmottakerService.avklarMottaker(
                    produserbaredokumenter,
                    Mottaker.medRolle(Mottakerroller.BRUKER),
                    behandling
                )
                if (avklartMottaker.rolle == Mottakerroller.BRUKER) {
                    return persondataFasade.hentSammensattNavn(behandling.fagsak.hentBrukersAktørID())
                } else if (avklartMottaker.personIdent != null) {
                    return persondataFasade.hentSammensattNavn(avklartMottaker.personIdent)
                } else {
                    val orgDokument = hentRettOrganisasjonsdokument(behandling, avklartMottaker.orgnr)
                    return orgDokument.navn
                }
            }

            Mottakerroller.ARBEIDSGIVER, Mottakerroller.VIRKSOMHET -> {
                val saksopplysning = eregFasade.finnOrganisasjon(orgnr)
                if (saksopplysning.isPresent) {
                    val orgDokument = saksopplysning.get().dokument as OrganisasjonDokument
                    return orgDokument.navn
                } else {
                    throw IkkeFunnetException("Kan ikke hente mottakernavn, fant ikke orgnr $orgnr")
                }
            }

            Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET -> {
                if (produserbaredokumenter == Produserbaredokumenter.UTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV) {
                    if (institusjonID != null) {
                        val utenlandskMyndighet =
                            utenlandskMyndighetService.hentUtenlandskMyndighetForInstitusjonID(institusjonID)
                        if (!utenlandskMyndighet.getAdresse().erGyldig()) {
                            val landkode = utenlandskMyndighet.landkode.beskrivelse
                            throw FunksjonellException(
                                "Du kan ikke sende brev til trygdemyndigheten i $landkode, fordi korrekt adresse er ukjent."
                            )
                        }
                        return utenlandskMyndighet.navn
                    } else {
                        val avklartMottaker = brevmottakerService.avklarMottaker(
                            produserbaredokumenter,
                            Mottaker.medRolle(Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET),
                            behandling
                        )
                        val utenlandskMyndighet = utenlandskMyndighetService.hentUtenlandskMyndighet(
                            avklartMottaker.hentMyndighetLandkode(),
                            produserbaredokumenter
                        )
                        return utenlandskMyndighet.navn
                    }
                } else {
                    throw FunksjonellException("Melosys støtter ikke hovedmottakere med rollen " + hovedmottaker)
                }
            }

            Mottakerroller.NORSK_MYNDIGHET -> throw FunksjonellException("Hent mottakere for norske mynigheter burde gå gjennom endepunktet mulige-mottakere-norske-myndigheter og støttes derfor ikke her")
            else -> throw FunksjonellException("Melosys støtter ikke hovedmottakere med rollen " + hovedmottaker)
        }
    }

    private fun hentRettOrganisasjonsdokument(behandling: Behandling, orgnr: String?): OrganisasjonDokument {
        val kontaktopplysning =
            kontaktopplysningService.hentKontaktopplysning(behandling.fagsak.saksnummer, orgnr).orElse(null)
        val mottakerOrgnr: String =
            (if (kontaktopplysning != null && kontaktopplysning.kontaktOrgnr != null) kontaktopplysning.kontaktOrgnr else orgnr)!!
        return eregFasade.hentOrganisasjon(mottakerOrgnr).dokument as OrganisasjonDokument
    }


    private fun lagKopiMottakereMuligMottakerDtos(
        produserbaredokumenter: Produserbaredokumenter,
        behandling: Behandling,
        kopiMottakere: Collection<Mottakerroller>,
        hovedmottaker: Mottakerroller?
    ): MutableList<Brevmottaker?> {
        val brevmottakere: MutableList<Brevmottaker?> = ArrayList<Brevmottaker?>()
        for (kopiMottaker in kopiMottakere) {
            when (kopiMottaker) {
                Mottakerroller.BRUKER -> brevmottakere.add(
                    lagKopiMottakerForBruker(
                        produserbaredokumenter,
                        behandling,
                        kopiMottaker,
                        hovedmottaker
                    )
                )

                Mottakerroller.ARBEIDSGIVER -> brevmottakere.addAll(
                    lagKopiMottakereForArbeidsgiver(
                        produserbaredokumenter,
                        behandling,
                        kopiMottaker
                    )
                )

                Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET -> brevmottakere.addAll(
                    lagKopiMottakereForUtenlandskTrygdemyndighet(produserbaredokumenter, behandling, kopiMottaker)
                )

                else -> throw IllegalStateException(kopiMottaker.toString() + " er ikke en gyldig kopiMottakerrolle")
            }
        }
        return brevmottakere
    }

    private fun lagKopiMottakereForArbeidsgiver(
        produserbaredokumenter: Produserbaredokumenter,
        behandling: Behandling,
        kopiMottaker: Mottakerroller?
    ): MutableList<Brevmottaker?> {
        val brevmottakere: MutableList<Brevmottaker?> = ArrayList<Brevmottaker?>()

        val avklarteKopier = brevmottakerService.avklarMottakere(
            produserbaredokumenter,
            Mottaker.medRolle(kopiMottaker),
            behandling,
            false,
            true
        )
        for (avklartKopi in avklarteKopier) {
            val orgDokument = hentRettOrganisasjonsdokument(behandling, avklartKopi.orgnr)
            val fastTekst =
                if (avklartKopi.rolle == Mottakerroller.ARBEIDSGIVER) "Kopi til arbeidsgiver" else "Kopi til arbeidsgivers fullmektig"
            brevmottakere.add(
                Brevmottaker.Builder()
                    .medDokumentNavn(
                        dokumentNavnService.utledDokumentNavnForProduserbartdokumentOgMottaker(
                            behandling,
                            produserbaredokumenter,
                            avklartKopi,
                            fastTekst
                        )
                    )
                    .medMottakerNavn(orgDokument.navn)
                    .medRolle(avklartKopi.rolle)
                    .medOrgnr(orgDokument.orgnummer)
                    .build()
            )
        }
        return brevmottakere
    }

    private fun lagKopiMottakereForUtenlandskTrygdemyndighet(
        produserbaredokumenter: Produserbaredokumenter,
        behandling: Behandling,
        kopiMottaker: Mottakerroller?
    ): MutableList<Brevmottaker?> {
        val brevmottakere: MutableList<Brevmottaker?> = ArrayList<Brevmottaker?>()

        val avklarteKopier =
            brevmottakerService.avklarMottakere(produserbaredokumenter, Mottaker.medRolle(kopiMottaker), behandling)
        for (avklartKopi in avklarteKopier) {
            val fastTekst = "Kopi til utenlandsk trygdemyndighet"
            brevmottakere.add(
                Brevmottaker.Builder()
                    .medDokumentNavn(
                        dokumentNavnService.utledDokumentNavnForProduserbartdokumentOgMottaker(
                            behandling,
                            produserbaredokumenter,
                            avklartKopi,
                            fastTekst
                        )
                    )
                    .medMottakerNavn("Utenlandsk trygdemyndighet")
                    .medRolle(avklartKopi.rolle)
                    .medInstitusjonID(avklartKopi.institusjonID)
                    .build()
            )
        }
        return brevmottakere
    }


    @JvmRecord
    data class RequestDto(
        val produserbartdokument: Produserbaredokumenter,
        val behandlingID: Long,
        val orgnr: String?,
        val institusjonID: String?
    )

    @JvmRecord
    data class ResponseDto(
        @JvmField val hovedMottaker: Brevmottaker?,
        @JvmField val kopiMottakere: MutableList<Brevmottaker?>?,
        @JvmField val fasteMottakere: MutableList<Brevmottaker?>?
    )
}
