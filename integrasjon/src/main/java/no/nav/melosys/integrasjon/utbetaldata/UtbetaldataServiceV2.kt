package no.nav.melosys.integrasjon.utbetaldata

import no.nav.melosys.domain.Saksopplysning
import no.nav.melosys.domain.dokument.DokumentFactory
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.exception.IntegrasjonException
import no.nav.melosys.exception.SikkerhetsbegrensningException
import no.nav.melosys.integrasjon.KonverteringsUtils
import no.nav.melosys.integrasjon.utbetaldata.utbetaling.UtbetalingConsumer
import no.nav.melosys.integrasjon.utbetaldata.utbetaling.UtbetalingConsumerV2
import no.nav.melosys.integrasjon.utbetaldata.utbetaling.UtbetalingResponse
import no.nav.tjeneste.virksomhet.utbetaling.v1.HentUtbetalingsinformasjonIkkeTilgang
import no.nav.tjeneste.virksomhet.utbetaling.v1.HentUtbetalingsinformasjonPeriodeIkkeGyldig
import no.nav.tjeneste.virksomhet.utbetaling.v1.HentUtbetalingsinformasjonPersonIkkeFunnet
import no.nav.tjeneste.virksomhet.utbetaling.v1.HentUtbetalingsinformasjonResponse
import no.nav.tjeneste.virksomhet.utbetaling.v1.informasjon.*
import no.nav.tjeneste.virksomhet.utbetaling.v1.meldinger.WSHentUtbetalingsinformasjonRequest
import no.nav.tjeneste.virksomhet.utbetaling.v1.meldinger.WSHentUtbetalingsinformasjonResponse
import org.springframework.stereotype.Service
import java.io.StringWriter
import java.time.LocalDate
import java.util.function.Consumer
import javax.xml.bind.JAXBException
import javax.xml.ws.WebServiceException

@Service
class UtbetaldataServiceV2(
    private val utbetalingConsumer: UtbetalingConsumerV2,
    private val dokumentFactory: DokumentFactory
) : UtbetaldataFasade {
    override fun hentUtbetalingerBarnetrygd(fnr: String, fom: LocalDate, tom: LocalDate): Saksopplysning {
        val response: UtbetalingResponse
        response = if (erUtbetalingsDataStøttet(tom)) {
            WSHentUtbetalingsinformasjonResponse()
        } else {
            filtrerYtelserAvTypeBarnetrygd(
                hentUtbetalingsinformasjon(lagRequest(fnr, fom, tom))
            )
        }
        return UtbetaldataMapper.tilSaksopplysning(response, lagXml(response).toString())
    }

    private fun hentUtbetalingsinformasjon(request: WSHentUtbetalingsinformasjonRequest): WSHentUtbetalingsinformasjonResponse {
        return try {
            utbetalingConsumer.hentUtbetalingsInformasjon(request)
        } catch (hentUtbetalingsinformasjonPersonIkkeFunnet: HentUtbetalingsinformasjonPersonIkkeFunnet) {
            throw IkkeFunnetException("Oppgitt person ble ikke funnet")
        } catch (hentUtbetalingsinformasjonPeriodeIkkeGyldig: HentUtbetalingsinformasjonPeriodeIkkeGyldig) {
            throw FunksjonellException("Oppgitt periode er ikke gyldig", hentUtbetalingsinformasjonPeriodeIkkeGyldig)
        } catch (hentUtbetalingsinformasjonIkkeTilgang: HentUtbetalingsinformasjonIkkeTilgang) {
            throw SikkerhetsbegrensningException(
                "Har ikke tilgang til å hente data for person",
                hentUtbetalingsinformasjonIkkeTilgang
            )
        } catch (e: WebServiceException) {
            throw IntegrasjonException(e)
        }
    }

    private fun lagXml(response: WSHentUtbetalingsinformasjonResponse): StringWriter {
        val xmlWriter = StringWriter()
        try {
            val xmlRoot = HentUtbetalingsinformasjonResponse()
            xmlRoot.hentUtbetalingsinformasjonResponse = response
            dokumentFactory.createMarshaller().marshal(xmlRoot, xmlWriter)
        } catch (e: JAXBException) {
            throw IntegrasjonException(e)
        }
        return xmlWriter
    }

    private fun lagRequest(fnr: String, fom: LocalDate, tom: LocalDate): WSHentUtbetalingsinformasjonRequest {
        val request = WSHentUtbetalingsinformasjonRequest()
        request.id = lagIdent(fnr)
        request.periode = lagPeriode(fom, tom)
        return request
    }

    private fun lagPeriode(fom: LocalDate, tom: LocalDate): WSForespurtPeriode {
        var fom = fom
        val periode = WSForespurtPeriode()
        val periodetype = WSPeriodetyper()
        periodetype.value = YTELSESPERIODE
        periode.periodeType = periodetype
        if (datoErEldreEnnTreÅr(fom)) {
            fom = LocalDate.now().minusYears(3)
        }
        periode.fom = KonverteringsUtils.javaLocalDateToJodaDateTime(fom)
        periode.tom = KonverteringsUtils.javaLocalDateToJodaDateTime(tom)
        return periode
    }

    private fun filtrerYtelserAvTypeBarnetrygd(response: WSHentUtbetalingsinformasjonResponse): WSHentUtbetalingsinformasjonResponse {
        taVekkUtbetalingerUtenBarnetrygd(response)
        taVekkYtelserFraUtbetalingerSomIkkeErBarnetrygd(response)
        return response
    }

    private fun taVekkYtelserFraUtbetalingerSomIkkeErBarnetrygd(response: WSHentUtbetalingsinformasjonResponse) {
        response.utbetalingListe.forEach(Consumer { utbetaling: WSUtbetaling ->
            utbetaling.ytelseListe
                .removeIf { ytelse: WSYtelse -> !erBarnetrygdytelse(ytelse) }
        })
    }

    private fun taVekkUtbetalingerUtenBarnetrygd(response: WSHentUtbetalingsinformasjonResponse) {
        response.utbetalingListe.removeIf { utbetaling: WSUtbetaling ->
            utbetaling.ytelseListe.stream()
                .noneMatch { ytelse: WSYtelse -> erBarnetrygdytelse(ytelse) }
        }
    }

    private fun erBarnetrygdytelse(ytelse: WSYtelse): Boolean {
        return ytelse.ytelsestype != null && ytelse.ytelsestype.value != null && ytelse.ytelsestype.value.trim { it <= ' ' }
            .equals(BARNETRYGD, ignoreCase = true)
    }

    private fun erUtbetalingsDataStøttet(tom: LocalDate?): Boolean {
        return tom != null && datoErEldreEnnTreÅr(tom)
    }

    private fun datoErEldreEnnTreÅr(dato: LocalDate): Boolean {
        return dato.isBefore(LocalDate.now().minusYears(3))
    }

    companion object {
        private const val BARNETRYGD = "BARNETRYGD"
        private const val RETTIGHETSHAVER = "Rettighetshaver"
        private const val YTELSESPERIODE = "Ytelsesperiode"
        private fun lagIdent(fnr: String): WSIdent {
            val ident = WSIdent()
            val identrolle = WSIdentroller()
            identrolle.value = RETTIGHETSHAVER
            ident.rolle = identrolle
            ident.ident = fnr
            return ident
        }
    }
}
