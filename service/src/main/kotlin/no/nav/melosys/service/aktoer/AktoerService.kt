package no.nav.melosys.service.aktoer

import mu.KotlinLogging
import no.nav.melosys.domain.Aktoer
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.integrasjon.joark.HentJournalposterTilknyttetSakRequest
import no.nav.melosys.integrasjon.joark.JoarkFasade
import no.nav.melosys.repository.AktoerRepository
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.service.tilgang.Aksesskontroll
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger { }

@Service
class AktoerService(
    private val aktørRepository: AktoerRepository,
    private val fagsakRepository: FagsakRepository,
    private val aksesskontroll: Aksesskontroll,
    private val joarkFasade: JoarkFasade
) {
    fun hentfagsakAktører(fagsak: Fagsak, aktoersrolle: Aktoersroller?): List<Aktoer> {
        if (aktoersrolle == null) {
            return aktørRepository.findByFagsak(fagsak)
        }
        return aktørRepository.findByFagsakAndRolle(fagsak, aktoersrolle)
    }

    @Transactional
    fun lagEllerOppdaterAktoer(fagsak: Fagsak, aktoerDto: AktoerDto): Long {
        if (aktoerDto.rolleKode == null) {
            throw FunksjonellException("Kan ikke lagre aktør uten rolle. Saksnummer: " + fagsak.saksnummer)
        }
        if (aktoerDto.fullmakter != null && aktoerDto.fullmakter.isNotEmpty()) {
            validerFullmakter(fagsak, aktoerDto)
        }


        val aktoer = if (aktoerDto.databaseID == null) {
            Aktoer()
        } else {
            aktørRepository.findById(aktoerDto.databaseID)
                .orElseThrow { IkkeFunnetException("Finner ikke aktør med id " + aktoerDto.databaseID) }
        }

        aktoer.fagsak = fagsak
        aktoer.institusjonID = aktoerDto.institusjonsID
        aktoer.utenlandskPersonId = aktoerDto.utenlandskPersonID
        aktoer.orgnr = aktoerDto.orgnr
        aktoer.rolle = Aktoersroller.valueOf(aktoerDto.rolleKode)
        aktoer.aktørId = aktoerDto.aktoerID
        aktoer.personIdent = aktoerDto.personIdent
        if (aktoerDto.fullmakter != null) {
            aktoer.setFullmaktstyper(aktoerDto.fullmakter)
        }

        return aktørRepository.save(aktoer).id
    }

    private fun validerFullmakter(fagsak: Fagsak, aktoerDto: AktoerDto) {
        val fullmektiger = aktørRepository.findByFagsakAndFullmakterIsNotEmpty(fagsak)
        val fullmektigMedLikFullmakt = fullmektiger.find { it.fullmaktstyper.intersect(aktoerDto.fullmakter).isNotEmpty() }
        if (fullmektigMedLikFullmakt != null && fullmektigMedLikFullmakt.id != aktoerDto.databaseID) {
            throw FunksjonellException("Det skal kun være en fullmektig per fullmakttype. Saksnummer: " + fagsak.saksnummer)
        }
    }

    @Transactional
    fun slettAktoer(databaseID: Long) {
        val aktoer =
            aktørRepository.findById(databaseID).orElseThrow { TekniskException("Klarte ikke slette aktøren. Fant ingen aktør på id: $databaseID") }

        if (aktoer.rolle == Aktoersroller.BRUKER) {
            throw FunksjonellException("Aktøren er en bruker. Det er ikke lov til å slette denne")
        }
        val fagsak = aktoer.fagsak
        fagsak.aktører.remove(aktoer)
        aktørRepository.deleteById(databaseID)
    }

    @Transactional
    fun erstattEksisterendeArbeidsgiveraktører(fagsak: Fagsak, orgnumre: List<String>) {
        aktørRepository.deleteAllByFagsakAndRolle(fagsak, Aktoersroller.ARBEIDSGIVER)
        aktørRepository.flush()

        for (orgnummer in orgnumre) {
            lagArbeidsgiveraktør(fagsak, orgnummer)
        }
    }

    @Transactional
    fun endreAktørIdForBruker(saksnummer: String, nyAktørId: String?) {
        val validertNyAktørId = nyAktørId?.takeIf { it.length == AKTOER_ID_LENGDE }
            ?: throw FunksjonellException("Aktør ID kan ikke være null og må være 13 tegn lang $nyAktørId")
        val fagsak = fagsakRepository.findById(saksnummer)
            .orElseThrow { IkkeFunnetException("Finner ikke fagsak med saksnummer: $saksnummer") }
        val gammelAktørId = fagsak.hentBrukersAktørID()

        aksesskontroll.auditEndringFraAdminConsole(
            validertNyAktørId,
            "Endring av aktør ID for sak $saksnummer fra $gammelAktørId til $validertNyAktørId"
        )

        joarkFasade.oppdaterJournalposterMedNyAktørId(
            HentJournalposterTilknyttetSakRequest(fagsak.gsakSaksnummer, fagsak.saksnummer),
            gammelAktørId,
            validertNyAktørId
        )

        endreAktørIdForBruker(fagsak, validertNyAktørId)
    }

    @Transactional
    fun endreAktørIdForBruker(fagsak: Fagsak, nyAktørId: String) {
        val eksisterendeBrukerAktør = fagsak.aktører.firstOrNull { it.rolle == Aktoersroller.BRUKER }
            ?: throw IllegalArgumentException("Finner ikke BRUKER aktør for ${fagsak.saksnummer}")

        eksisterendeBrukerAktør.aktørId = nyAktørId
        aktørRepository.save(eksisterendeBrukerAktør)
    }

    private fun lagArbeidsgiveraktør(fagsak: Fagsak, orgnummer: String) {
        val aktør = Aktoer().apply {
            this.fagsak = fagsak
            rolle = Aktoersroller.ARBEIDSGIVER
            orgnr = orgnummer
        }

        aktørRepository.save(aktør)
    }

    private companion object {
        const val AKTOER_ID_LENGDE = 13
    }
}
