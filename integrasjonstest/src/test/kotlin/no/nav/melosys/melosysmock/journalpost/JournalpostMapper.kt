package no.nav.melosys.melosysmock.journalpost

import no.nav.melosys.melosysmock.journalpost.intern_modell.*
import no.nav.melosys.melosysmock.journalpost.journalpostapi.Dokument
import no.nav.melosys.melosysmock.journalpost.journalpostapi.DokumentVariant
import no.nav.melosys.melosysmock.journalpost.journalpostapi.OppdaterJournalpostRequest
import no.nav.melosys.melosysmock.journalpost.journalpostapi.OpprettJournalpostRequest
import no.nav.melosys.melosysmock.utils.lagRandomId
import java.time.LocalDate

class JournalpostMapper {

    fun tilModell(request: OpprettJournalpostRequest, forsoekFerdigstill: Boolean?): JournalpostModell {
        return JournalpostModell(
            journalpostId = lagRandomId(),
            avsenderMottaker = AvsenderMottaker(
                id = request.avsenderMottaker?.id,
                type = IdType.valueOf(request.avsenderMottaker?.idType?.name!!),
                navn = request.avsenderMottaker.navn,
                land = request.avsenderMottaker.land
            ),
            dokumentModellList = tilDokumentListe(request.dokumenter),
            sakId = request.sak?.fagsakId,
            sakstype = request.sak?.sakstype,
            journalStatus = if (forsoekFerdigstill == true) JournalStatus.J else JournalStatus.M,
            mottattDato = request.datoMottatt ?: LocalDate.now(),
            arkivtema = request.tema?.let { Tema.valueOf(it) },
            journalposttype = request.journalpostType!!.let { Journalposttype.valueOf(it.name) },
            bruker = request.bruker?.let {
                JournalpostBruker(
                    ident = request.bruker.id,
                    brukerType = request.bruker.idType?.let { IdType.valueOf(it.name) }
                )
            },
            kanal = request.kanal,
            tittel = request.tittel,
            eksternReferanseId = request.eksternReferanseId,
            journalfoerendeEnhet = request.journalfoerendeEnhet
        )
    }

    private fun tilDokumentListe(dokumenter: List<Dokument>?): List<DokumentModell> {
        val dokumentListe = mutableListOf<DokumentModell>()
        dokumenter?.forEachIndexed { i, dok ->
            val dokumentTilknyttetJournalpost =
                if (i == 0) DokumentTilknyttetJournalpost.HOVEDDOKUMENT else DokumentTilknyttetJournalpost.VEDLEGG
            dokumentListe.add(
                DokumentModell(
                    dokumentId = lagRandomId(),
                    tittel = dok.tittel,
                    dokumentkategori = dok.dokumentKategori,
                    brevkode = dok.brevkode,
                    dokumentTilknyttetJournalpost = dokumentTilknyttetJournalpost,
                    dokumentVarianter = lagDokumentvarianter(dok.dokumentvarianter)
                )
            )
        }
        return dokumentListe
    }

    private fun lagDokumentvarianter(dokumentvarianter: List<DokumentVariant>?): List<DokumentVariantInnhold>? {
        return dokumentvarianter?.map {
            DokumentVariantInnhold(
                filType = it.filtype.let { arkivfiltype -> Arkivfiltype.valueOf(arkivfiltype.name) },
                variantFormat = it.variantformat?.let { variantformat -> VariantFormat.valueOf(variantformat) },
                dokumentInnhold = it.fysiskDokument ?: ByteArray(0)
            )
        }
    }

    fun oppdaterModell(
        oppdatering: OppdaterJournalpostRequest,
        journalpostModell: JournalpostModell
    ): JournalpostModell {
        val oppdatertModell = journalpostModell.copy()
        oppdatering.avsenderMottaker?.let {
            oppdatertModell.avsenderMottaker.id = it.id
            oppdatertModell.avsenderMottaker.land = it.land
            oppdatertModell.avsenderMottaker.navn = it.navn
            oppdatertModell.avsenderMottaker.type = IdType.valueOf(it.idType!!.name)
        }

        oppdatering.bruker?.let {
            oppdatertModell.bruker = JournalpostBruker(
                ident = it.id,
                brukerType = IdType.valueOf(it.idType!!.name)
            )
        }

        oppdatering.datoMottatt?.let { oppdatertModell.mottattDato = it }
        oppdatering.dokumenter?.forEach {
            oppdatertModell.dokumentModellList
                .first { d -> d.dokumentId == it.dokumentInfoId }
                .also { d ->
                    d.tittel = it.tittel
                    d.brevkode = it.brevkode
                }
        }

        oppdatering.eksternReferanseId?.let { oppdatertModell.eksternReferanseId = it }
        oppdatering.journalfoerendeEnhet?.let { oppdatertModell.journalfoerendeEnhet = it }
        oppdatering.kanal?.let { oppdatertModell.kanal = it }
        oppdatering.tema?.let { oppdatertModell.arkivtema = Tema.valueOf(it) }
        oppdatering.sak?.let { oppdatertModell.sakId = it.fagsakId }
        oppdatering.tittel?.let { oppdatertModell.tittel = it }
        oppdatering.tilleggsopplysninger?.forEach {
            oppdatertModell.tilleggsoppltsninger.removeIf { t -> t.nokkel == it.nokkel }
            oppdatertModell.tilleggsoppltsninger.add(Tilleggsopplysning(it.nokkel, it.verdi))
        }

        return oppdatertModell
    }
}
