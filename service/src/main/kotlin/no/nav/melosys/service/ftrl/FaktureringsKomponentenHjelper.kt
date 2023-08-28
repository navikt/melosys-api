package no.nav.melosys.service.ftrl

class FaktureringsKomponentenHjelper {

    companion object {
        fun konverterTilVedtaksId(saksnummer: String, behandlingsId: Long) = "${saksnummer}-$behandlingsId"

        fun hentSaksnummer(vedtaksId: String) = konverterTilSaksnummerOgBehandlingsId(vedtaksId).first

        fun hentBehandingsId(vedtaksId: String): Long = konverterTilSaksnummerOgBehandlingsId(vedtaksId).second.toLong()

        fun konverterTilSaksnummerOgBehandlingsId(vedtaksId: String): Pair<String, String> {
            val index = vedtaksId.indexOf("-", vedtaksId.indexOf("-") + 1)
            val saksnummer = vedtaksId.substring(0, index)
            val behandlingsId = vedtaksId.substring(index + 1)
            return Pair(saksnummer, behandlingsId)
        }
    }
}
