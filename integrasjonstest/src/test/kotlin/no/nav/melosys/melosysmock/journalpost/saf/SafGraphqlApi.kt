package no.nav.melosys.melosysmock.journalpost.saf

import no.nav.melosys.generated.graphql.api.DokumentoversiktFagsakQueryResolver
import no.nav.melosys.generated.graphql.api.JournalpostQueryResolver
import no.nav.melosys.generated.graphql.model.*
import no.nav.melosys.melosysmock.journalpost.JournalpostRepo
import no.nav.melosys.melosysmock.journalpost.intern_modell.*
import no.nav.melosys.melosysmock.sak.SakRepo
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class SafGraphqlResolver : JournalpostQueryResolver, DokumentoversiktFagsakQueryResolver {

    override fun journalpost(journalpostId: String): JournalpostDto? = JournalpostRepo.repo[journalpostId]
        ?.let (::tilJournalpostDto) ?: throw NoSuchElementException("Finner ikke journalpost med id $journalpostId")

    override fun dokumentoversiktFagsak(
        fagsak: FagsakInputDto,
        fraDato: String?,
        tema: List<TemaDto?>?,
        journalposttyper: List<JournalposttypeDto?>?,
        journalstatuser: List<JournalstatusDto?>?,
        foerste: Int?,
        etter: String?
    ) = DokumentoversiktDto(
            journalposter = JournalpostRepo.finnVedSaksnummer(fagsak.fagsakId).map(::tilJournalpostDto),
            sideInfo = SideInfoDto(sluttpeker = "blabla", finnesNesteSide = false)
    )

    private fun tilJournalpostDto(modell: JournalpostModell) = JournalpostDto.builder()
            .setJournalpostId(modell.journalpostId)
            .setTittel(modell.tittel)
            .setJournalposttype(mapJournalposttype(modell.journalposttype))
            .setJournalstatus(mapJournalstatus(modell.journalStatus))
            .setTema(mapTema(modell.arkivtema))
            .setTemanavn(mapTema(modell.arkivtema)?.name)
            .setSak(mapSak(modell.sakId))
            .setBruker(mapBruker(modell.bruker))
            .setAvsenderMottaker(mapAvsenderMottaker(modell.avsenderMottaker))
            .setJournalfoerendeEnhet(modell.journalfoerendeEnhet)
            .setKanal(mapKanal(modell.kanal))
            .setDatoOpprettet(modell.mottattDato?.atStartOfDay() ?: LocalDateTime.now())
            .setRelevanteDatoer(mapRelevanteDatoer(modell))
            .setEksternReferanseId(modell.eksternReferanseId)
            .setTilleggsopplysninger(modell.tilleggsoppltsninger.map(::mapTilleggsopplysning))
            .setDokumenter(modell.dokumentModellList.map(::mapDokumentInfo))
            .build()


    private fun mapJournalposttype(journalposttype: Journalposttype?): JournalposttypeDto? =
        when (journalposttype) {
            Journalposttype.INNGAAENDE -> JournalposttypeDto.I
            Journalposttype.UTGAAENDE -> JournalposttypeDto.U
            Journalposttype.NOTAT -> JournalposttypeDto.N
            else -> null
        }

    private fun mapJournalstatus(journalStatus: JournalStatus?): JournalstatusDto? =
        when (journalStatus) {
            JournalStatus.J -> JournalstatusDto.JOURNALFOERT
            JournalStatus.MO, JournalStatus.M -> JournalstatusDto.MOTTATT
            JournalStatus.A -> JournalstatusDto.AVBRUTT
            else -> null
        }

    private fun mapTema(arkivtema: Tema?): TemaDto? =
        when (arkivtema) {
            Tema.MED -> TemaDto.MED
            Tema.UFM -> TemaDto.UFM
            else -> null
        }

    private fun mapSak(sakId: String?): SakDto? =
        sakId?.toLongOrNull()?.let {
            SakRepo.repo[it]?.let { sak ->
                SakDto.builder()
                    .setFagsakId(sak.fagsakNr)
                    .setFagsaksystem(sak.applikasjon)
                    .setArkivsaksnummer(sak.id.toString())
                    .build()
            }
        }

    private fun mapBruker(bruker: JournalpostBruker?): BrukerDto =
        BrukerDto.builder()
            .setId(bruker?.ident)
            .setType(mapBrukertype(bruker?.brukerType))
            .build()

    private fun mapBrukertype(brukerType: IdType?): BrukerIdTypeDto? =
        when (brukerType) {
            IdType.FNR -> BrukerIdTypeDto.FNR
            IdType.ORGNR -> BrukerIdTypeDto.ORGNR
            IdType.AKTOERID -> BrukerIdTypeDto.AKTOERID
            else -> null
        }

    private fun mapAvsenderMottaker(avsenderMottaker: AvsenderMottaker): AvsenderMottakerDto =
        AvsenderMottakerDto.builder()
            .setId(avsenderMottaker.id)
            .setNavn(avsenderMottaker.navn)
            .setLand(avsenderMottaker.land)
            .setType(mapAvsenderMottakerType(avsenderMottaker.type))
            .build()

    private fun mapAvsenderMottakerType(type: IdType?): AvsenderMottakerIdTypeDto {
        return when (type) {
            IdType.FNR -> AvsenderMottakerIdTypeDto.FNR
            IdType.ORGNR -> AvsenderMottakerIdTypeDto.ORGNR
            IdType.UTL_ORG -> AvsenderMottakerIdTypeDto.UTL_ORG
            IdType.AKTOERID -> AvsenderMottakerIdTypeDto.UKJENT // Saf støtter ikke AktørID
            else -> AvsenderMottakerIdTypeDto.NULL
        }
    }

    private fun mapKanal(kanal: String?): KanalDto? = kanal?.let { KanalDto.valueOf(it) }

    private fun mapRelevanteDatoer(journalpostModell: JournalpostModell): List<RelevantDatoDto?> =
        listOfNotNull(
            journalpostModell.journalfoertDato?.let { relevantDatoDto(it, DatotypeDto.DATO_JOURNALFOERT) },
            journalpostModell.mottattDato?.let { relevantDatoDto(it.atStartOfDay(), DatotypeDto.DATO_REGISTRERT) }
        )

    private fun relevantDatoDto(localDateTime: LocalDateTime, datotypeDto: DatotypeDto) =
        RelevantDatoDto.builder()
            .setDato(localDateTime)
            .setDatotype(datotypeDto)
            .build()

    private fun mapTilleggsopplysning(tilleggsopplysning: Tilleggsopplysning) =
        TilleggsopplysningDto.builder()
            .setNokkel(tilleggsopplysning.nokkel)
            .setVerdi(tilleggsopplysning.verdi)
            .build()

    private fun mapDokumentInfo(dokumentModell: DokumentModell): DokumentInfoDto =
        DokumentInfoDto.builder()
            .setDokumentInfoId(
                dokumentModell.dokumentId ?: throw IllegalArgumentException("Krever dokumentId for å opprette dokument")
            )
            .setTittel(dokumentModell.tittel)
            .setDokumentvarianter(dokumentModell.dokumentVarianter?.map(::mapDokumentvarianter) ?: emptyList())
            .setLogiskeVedlegg(emptyList())
            .setBrevkode(dokumentModell.brevkode)
            .build()

    private fun mapDokumentvarianter(dokumentVariant: DokumentVariantInnhold): DokumentvariantDto {
        return DokumentvariantDto.builder()
            .setFiltype(dokumentVariant.filType.name)
            .setVariantformat(
                dokumentVariant.variantFormat.let { variantFormat ->
                    when (variantFormat) {
                        VariantFormat.ARKIV -> VariantformatDto.ARKIV
                        VariantFormat.FULLVERSJON -> VariantformatDto.FULLVERSJON
                        VariantFormat.ORIGINAL -> VariantformatDto.ORIGINAL
                        else -> throw IllegalArgumentException("Ukjent variantformat $variantFormat")
                    }
                })
            .setSaksbehandlerHarTilgang(true)
            .build()
    }
}
