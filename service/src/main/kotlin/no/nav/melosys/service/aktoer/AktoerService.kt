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
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.tilgang.Aksesskontroll
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger { }

@Service
class AktoerService(
    private val aktørRepository: AktoerRepository,
    private val fagsakRepository: FagsakRepository,
    private val aksesskontroll: Aksesskontroll,
    private val joarkFasade: JoarkFasade,
    private val persondataFasade: PersondataFasade
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

    /**
     * Journalpostene flyttes i Joark før endringene skrives til databasen, fordi gammel aktørId er det
     * eneste vi har for å finne dem igjen. Skrives aktøren først og Joark-flyttingen feiler, er det ikke
     * lenger mulig å se hvilke journalposter som skulle vært flyttet. Feiler derimot DB-skrivingen etter
     * en vellykket flytting, retter et nytt forsøk kun aktørId — journalpostene filtreres da bort på
     * gammel aktørId, så initierendeJournalpostId på behandlingene må rettes manuelt.
     *
     * Bevisst ikke @Transactional: Joark-kallene er eksterne HTTP-kall som ikke skal holde en
     * DB-transaksjon åpen mens de pågår. Endringene skrives til slutt med ett [FagsakRepository.save],
     * som cascader til både aktør og behandlinger.
     */
    fun endreAktørIdForBruker(saksnummer: String, nyAktørId: String) {
        val fagsak = fagsakRepository.findById(saksnummer)
            .orElseThrow { IkkeFunnetException("Finner ikke fagsak med saksnummer: $saksnummer") }
        val bruker = fagsak.hentBruker()
            ?: throw FunksjonellException("Finner ikke bruker på fagsak $saksnummer")
        val gammelAktørId = bruker.aktørId
            ?: throw FunksjonellException("Bruker på sak $saksnummer mangler aktørId")

        validerNyAktørId(nyAktørId, gammelAktørId, saksnummer)

        aksesskontroll.auditEndringFraAdminConsole(
            nyAktørId,
            "Endring av aktør ID for sak $saksnummer fra $gammelAktørId til $nyAktørId"
        )
        val flyttedeJournalposter = joarkFasade.oppdaterJournalposterMedNyAktørId(
            HentJournalposterTilknyttetSakRequest(fagsak.gsakSaksnummer, fagsak.saksnummer),
            gammelAktørId,
            nyAktørId
        )

        oppdaterInitierendeJournalpostIder(fagsak, flyttedeJournalposter)
        bruker.aktørId = nyAktørId
        fagsakRepository.save(fagsak)
    }

    /**
     * Alt må være avklart før journalpostene flyttes, for flyttingen gir dem nye IDer i arkivet og kan ikke
     * reverseres. Uten oppslaget mot PDL ville en tastefeil på 13 siffer flyttet alle dokumentene på saken
     * til en ukjent aktør; oppslaget kaster [IkkeFunnetException] hvis aktøren ikke finnes.
     */
    private fun validerNyAktørId(nyAktørId: String, gammelAktørId: String, saksnummer: String) {
        if (nyAktørId.length != AKTOER_ID_LENGDE || !nyAktørId.all(Char::isDigit)) {
            throw FunksjonellException("Aktør ID må være $AKTOER_ID_LENGDE siffer, var: $nyAktørId")
        }
        if (nyAktørId == gammelAktørId) {
            throw FunksjonellException("Bruker på sak $saksnummer har allerede aktørId $nyAktørId")
        }
        persondataFasade.hentAktørIdForIdent(nyAktørId)
    }

    /**
     * Journalpostene har fått nye IDer i arkivet, så behandlinger som peker på en flyttet journalpost må
     * oppdateres. Uten dette peker initierendeJournalpostId på en journalpost som ikke lenger er knyttet
     * til saken. Endringene lagres av kalleren.
     */
    private fun oppdaterInitierendeJournalpostIder(fagsak: Fagsak, flyttedeJournalposter: Map<String, String>) {
        if (flyttedeJournalposter.isEmpty()) return

        val oppdaterteBehandlinger = fagsak.behandlinger.mapNotNull { behandling ->
            val nyJournalpostId = flyttedeJournalposter[behandling.initierendeJournalpostId] ?: return@mapNotNull null
            behandling.initierendeJournalpostId = nyJournalpostId
            behandling
        }

        if (oppdaterteBehandlinger.isNotEmpty()) {
            log.info {
                "Oppdaterte initierendeJournalpostId på ${oppdaterteBehandlinger.size} behandling(er) for sak ${fagsak.saksnummer}"
            }
        }
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
