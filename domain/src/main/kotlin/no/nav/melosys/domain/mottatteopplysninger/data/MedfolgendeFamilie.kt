package no.nav.melosys.domain.mottatteopplysninger.data

import no.nav.commons.foedselsnummer.FoedselsNr
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.TekniskException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*
import java.util.stream.Stream


class MedfolgendeFamilie {
    var uuid: String? = null
        private set
    var fnr: String? = null
        private set
    var navn: String? = null
        private set
    var relasjonsrolle: Relasjonsrolle? = null
        private set

    constructor()
    private constructor(uuid: String, fnr: String?, navn: String?, relasjonsrolle: Relasjonsrolle) {
        this.uuid = uuid
        this.fnr = fnr
        this.navn = navn
        this.relasjonsrolle = relasjonsrolle
    }

    enum class Relasjonsrolle {
        BARN,
        EKTEFELLE_SAMBOER
    }

    fun erBarn(): Boolean {
        return relasjonsrolle == Relasjonsrolle.BARN
    }

    fun erEktefelleSamboer(): Boolean {
        return relasjonsrolle == Relasjonsrolle.EKTEFELLE_SAMBOER
    }

    fun utledIdentType(): IdentType {
        if (fnr!!.length < 11) {
            return IdentType.DATO
        }
        val foedselsNr = FoedselsNr(fnr!!)
        if (!foedselsNr.gyldigeKontrollsiffer) {
            throw FunksjonellException("$fnr er ikke et gyldig fødselsnummer, kontrollsiffer er ikke riktig")
        }
        return if (foedselsNr.dNummer) IdentType.DNR else IdentType.FNR
    }

    fun datoFraFnr(): LocalDate {
        if (utledIdentType() != IdentType.DATO) {
            return FoedselsNr(fnr!!).foedselsdato
        }
        val datoMedKlartÅrstall = finnFørsteMatch(
            Stream.of(
                "ddMMyyyy", "dd.MM.yyyy", "dd/MM/yyyy", "dd-MM-yyyy"
            )
        )
        if (datoMedKlartÅrstall != null) return datoMedKlartÅrstall
        val datoMedUklartÅrhundre = finnFørsteMatch(
            Stream.of(
                "ddMMyy", "dd.MM.yy", "dd/MM/yy", "dd-MM-yy"
            )
        ) ?: throw TekniskException("fnr: $fnr kan ikke parsers til fødselsdato")
        return if (datoMedUklartÅrhundre.isAfter(LocalDate.now())) {
            // om en person har en dato frem i tiden velger vi å plassere han på 1900 tallet
            datoMedUklartÅrhundre.minusYears(100)
        } else datoMedUklartÅrhundre
    }

    private fun finnFørsteMatch(stream: Stream<String>): LocalDate? {
        return stream.map { pattern: String ->
            tryParseFnr(
                pattern
            )
        }
            .filter { obj: LocalDate? -> Objects.nonNull(obj) }
            .findFirst()
            .orElse(null)
    }

    private fun tryParseFnr(pattern: String): LocalDate? {
        return try {
            LocalDate.parse(fnr, DateTimeFormatter.ofPattern(pattern))
        } catch (e: DateTimeParseException) {
            null
        }
    }

    companion object {
        fun tilBarnFraFnrOgNavn(fnr: String?, navn: String?): MedfolgendeFamilie {
            return tilMedfolgendeFamilie(UUID.randomUUID().toString(), fnr, navn, Relasjonsrolle.BARN)
        }

        fun tilEktefelleSamboerFraFnrOgNavn(fnr: String?, navn: String?): MedfolgendeFamilie {
            return tilMedfolgendeFamilie(UUID.randomUUID().toString(), fnr, navn, Relasjonsrolle.EKTEFELLE_SAMBOER)
        }

        fun tilMedfolgendeFamilie(uuid: String, fnr: String?, navn: String?, rolle: Relasjonsrolle): MedfolgendeFamilie {
            return MedfolgendeFamilie(uuid, fnr, navn, rolle)
        }
    }
}

