package no.nav.melosys.domain

import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import java.time.Instant

object FagsakTestFactory {
    const val SAKSNUMMER = "MEL-test"
    const val GSAK_SAKSNUMMER = 123456L
    val SAKSTYPE = Sakstyper.EU_EOS
    val SAKSTEMA = Sakstemaer.MEDLEMSKAP_LOVVALG
    val SAKSSTATUS = Saksstatuser.OPPRETTET

    // Disse burde flyttes til en AktørTestFactory om den kommer
    const val BRUKER_AKTØR_ID = "12345678901"
    const val ORGNR = "123456789"
    const val INSTITUSJON_ID = "SE:id"

    @JvmStatic
    fun builder() = Builder()

    @JvmStatic
    fun lagFagsak() = builder().build()

    class Builder(
        var saksnummer: String = SAKSNUMMER,
        var gsakSaksnummer: Long? = null,
        var type: Sakstyper = SAKSTYPE,
        var tema: Sakstemaer = SAKSTEMA,
        var status: Saksstatuser = SAKSSTATUS,
        var aktører: MutableSet<Aktoer> = mutableSetOf(),
        var behandlinger: MutableList<Behandling> = mutableListOf()
    ) {
        fun saksnummer(saksnummer: String) = apply { this.saksnummer = saksnummer }
        fun gsakSaksnummer(gsakSaksnummer: Long?) = apply { this.gsakSaksnummer = gsakSaksnummer }
        fun medGsakSaksnummer() = apply { this.gsakSaksnummer = GSAK_SAKSNUMMER }
        fun type(type: Sakstyper) = apply { this.type = type }
        fun tema(tema: Sakstemaer) = apply { this.tema = tema }
        fun status(status: Saksstatuser) = apply { this.status = status }
        fun aktører(aktører: Set<Aktoer>) = apply { this.aktører = aktører.toMutableSet() }
        fun aktører(aktører: Aktoer) = apply { this.aktører = mutableSetOf(aktører) }

        fun medBruker() = apply {
            leggTilAktør(Aktoer().apply {
                aktørId = BRUKER_AKTØR_ID
                rolle = Aktoersroller.BRUKER
            })
        }
        fun medVirksomhet() = apply {
            leggTilAktør(Aktoer().apply {
                orgnr = ORGNR
                rolle = Aktoersroller.VIRKSOMHET
            })
        }
        fun medTrygdemyndighet() = apply {
            leggTilAktør(Aktoer().apply {
                institusjonID = INSTITUSJON_ID
                rolle = Aktoersroller.TRYGDEMYNDIGHET
            })
        }

        fun behandlinger(behandlinger: List<Behandling>) = apply { this.behandlinger = behandlinger.toMutableList() }
        fun behandlinger(behandlinger: Behandling) = apply { this.behandlinger = mutableListOf(behandlinger) }

        fun leggTilAktør(aktør: Aktoer) = apply { this.aktører.add(aktør) }
        fun leggTilBehandling(behandling: Behandling) = apply { this.behandlinger.add(behandling) }

        fun build(): Fagsak {
            val fagsak = Fagsak(
                saksnummer,
                gsakSaksnummer,
                type,
                tema,
                status,
                aktører,
                behandlinger,
            )
            fagsak.registrertDato = Instant.now()
            fagsak.endretDato = Instant.now()
            return fagsak
        }
    }
}
