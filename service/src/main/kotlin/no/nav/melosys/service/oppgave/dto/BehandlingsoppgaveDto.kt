package no.nav.melosys.service.oppgave.dto

import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.service.felles.dto.SoeknadslandDto

class BehandlingsoppgaveDto(
    val behandling: BehandlingDto,
    val saksnummer: String,
    val sakstype: Sakstyper,
    val sakstema: Sakstemaer,
    val land: SoeknadslandDto?,
    val periode: PeriodeDto?,
    val oppgaveBeskrivelse: String?,
    val sisteNotat: String?,
) : OppgaveDto() {

    companion object {
        fun builder(): Builder = Builder()
    }

    class Builder {
        private lateinit var behandling: BehandlingDto
        private lateinit var saksnummer: String
        private lateinit var sakstype: Sakstyper
        private lateinit var sakstema: Sakstemaer
        private var land: SoeknadslandDto? = null
        private var periode: PeriodeDto? = null
        private var oppgaveBeskrivelse: String? = null
        private var sisteNotat: String? = null

        fun setBehandling(behandling: BehandlingDto): Builder {
            this.behandling = behandling
            return this
        }

        fun setLand(land: SoeknadslandDto): Builder {
            this.land = land
            return this
        }

        fun setSaksnummer(saksnummer: String): Builder {
            this.saksnummer = saksnummer
            return this
        }

        fun setSakstype(sakstype: Sakstyper): Builder {
            this.sakstype = sakstype
            return this
        }

        fun setSakstema(sakstema: Sakstemaer): Builder {
            this.sakstema = sakstema
            return this
        }

        fun setPeriode(periode: PeriodeDto): Builder {
            this.periode = periode
            return this
        }

        fun setOppgaveBeskrivelse(oppgaveBeskrivelse: String?): Builder {
            this.oppgaveBeskrivelse = oppgaveBeskrivelse
            return this
        }

        fun setSisteNotat(sisteNotat: String?): Builder {
            this.sisteNotat = sisteNotat
            return this
        }



        fun build(): BehandlingsoppgaveDto {
            return BehandlingsoppgaveDto(
                behandling,
                saksnummer,
                sakstype,
                sakstema,
                land,
                periode,
                oppgaveBeskrivelse,
                sisteNotat
            )
        }
    }
}
