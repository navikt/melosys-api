package no.nav.melosys.tjenester.gui.dto.saksbehandling

import no.nav.melosys.service.lovligekombinasjoner.SakstemaKombinasjoner
import no.nav.melosys.service.lovligekombinasjoner.SakstypeKombinasjoner

/**
 * Kombinasjonstreet sakstype -> sakstema -> behandlingstema.
 *
 * Kodene sendes rene, ikke som {kode, term}: klienten som bruker treet sammenligner det
 * mot tekstblokkenes avgrensning, som allerede leveres som rene koder. To former for
 * samme kodeverk i samme skjermbilde ville tvunget fram en mapping hos klienten, og gitt
 * to kilder til visningsnavn som kan divergere mellom deploy av api og web. Termene
 * finnes i kodeverket klienten allerede har lokalt.
 */
data class KombinasjonstreNodeDto(
    val sakstype: String,
    val sakstemaer: List<SakstemaNodeDto>,
) {
    companion object {
        @JvmStatic
        fun av(k: SakstypeKombinasjoner): KombinasjonstreNodeDto = KombinasjonstreNodeDto(
            sakstype = k.sakstype.kode,
            sakstemaer = k.sakstemaer.map(SakstemaNodeDto::av),
        )
    }
}

data class SakstemaNodeDto(
    val sakstema: String,
    val behandlingstemaer: List<String>,
) {
    companion object {
        @JvmStatic
        fun av(k: SakstemaKombinasjoner): SakstemaNodeDto = SakstemaNodeDto(
            sakstema = k.sakstema.kode,
            // Sortert: settet fra domenet har ingen stabil rekkefølge, og en respons som
            // stokker om på seg selv mellom kall gir unødvendig cache-støy hos klienten.
            behandlingstemaer = k.behandlingstemaer.map { it.kode }.sorted(),
        )
    }
}
