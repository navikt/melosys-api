package no.nav.melosys.integrasjon.kodeverk.impl

import jakarta.ws.rs.NotFoundException
import no.nav.melosys.integrasjon.kodeverk.Kode
import no.nav.melosys.integrasjon.kodeverk.Kodeverk
import no.nav.melosys.integrasjon.kodeverk.KodeverkRegister
import no.nav.melosys.integrasjon.kodeverk.UkjentKodeverkException
import no.nav.melosys.integrasjon.kodeverk.impl.dto.BetydningDto
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
class KodeverkRegisterImpl internal constructor(private val kodeverkConsumer: KodeverkConsumerImpl) : KodeverkRegister {
    @Cacheable("kodeverk")
    override fun hentKodeverk(kodeverkNavn: String): Kodeverk {
        return try {
            val fellesKodeverkDto = kodeverkConsumer.hentKodeverk(kodeverkNavn)
            val koder: MutableMap<String, List<Kode>> = HashMap()
            for ((key, value) in fellesKodeverkDto.betydninger) {
                val termer = value.stream().map { betydning: BetydningDto ->
                    val term = betydning.beskrivelser[BOKMÅL]?.term
                    Kode(key, term!!, betydning.gyldigFra, betydning.gyldigTil)
                }.toList()
                koder[key] = termer
            }
            Kodeverk(kodeverkNavn, koder)
        } catch (e: NotFoundException) {
            throw UkjentKodeverkException("Finner ingen kodeverk med navn $kodeverkNavn")
        }
    }

    companion object {
        const val BOKMÅL = "nb"
    }
}
