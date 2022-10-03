package no.nav.melosys.integrasjon.eessi

import no.nav.melosys.domain.arkiv.Vedlegg
import no.nav.melosys.domain.eessi.BucType
import no.nav.melosys.domain.eessi.Institusjon
import no.nav.melosys.domain.eessi.SedType
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.eessi.sed.SedDataDto
import no.nav.melosys.domain.eessi.sed.SedGrunnlagDto
import no.nav.melosys.integrasjon.eessi.dto.*
import no.nav.melosys.integrasjon.felles.JsonRestIntegrasjon
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

class EessiConsumerWebClient(private val webClient: WebClient) : EessiConsumer, JsonRestIntegrasjon {

    override fun opprettBucOgSed(
        sedDataDto: SedDataDto,
        vedlegg: Collection<Vedlegg>,
        bucType: BucType,
        sendAutomatisk: Boolean,
        oppdaterEksisterendeOmFinnes: Boolean
    ) =
        webClient.post()
            .uri("/buc/$bucType?sendAutomatisk=$sendAutomatisk&oppdaterEksisterende=$oppdaterEksisterendeOmFinnes")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(OpprettBucOgSedDto(sedDataDto, vedlegg))
            .retrieve()
            .bodyToMono(OpprettSedDto::class.java)
            .block()

    override fun sendSedPåEksisterendeBuc(sedDataDto: SedDataDto, rinaSaksnummer: String, sedType: SedType) {
        webClient.post()
            .uri("/buc/$rinaSaksnummer/sed/$sedType")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(sedDataDto)
            .retrieve()
            .bodyToMono(Void::class.java)
            .block()
    }

    override fun hentMottakerinstitusjoner(bucType: String, landkoder: Collection<String>) =
        webClient.get()
            .uri("/buc/$bucType/institusjoner?land=${landkoder.joinToString(",")}")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(typeReference<List<InstitusjonDto>>())
            .block()?.map { Institusjon(it.id, it.navn, it.landkode) }!!.toList()

    override fun hentMelosysEessiMeldingFraJournalpostID(journalpostID: String): MelosysEessiMelding =
        webClient.get()
            .uri("/journalpost/$journalpostID/eessimelding")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(MelosysEessiMelding::class.java)
            .block()!!

    override fun lagreSaksrelasjon(saksrelasjonDto: SaksrelasjonDto) {
        webClient.post()
            .uri("/sak")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(saksrelasjonDto)
            .retrieve()
            .bodyToMono(Void::class.java)
            .block()
    }

    override fun hentSakForRinasaksnummer(rinaSaksnummer: String) =
        webClient.get()
            .uri("/sak?rinaSaksnummer=$rinaSaksnummer")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(typeReference<List<SaksrelasjonDto>>())
            .block()

    override fun genererSedPdf(sedDataDto: SedDataDto, sedType: SedType) =
        webClient.post()
            .uri("/sed/$sedType/pdf")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(sedDataDto)
            .retrieve()
            .bodyToMono(ByteArray::class.java)
            .block()

    override fun hentTilknyttedeBucer(gsakSaksnummer: Long, statuser: List<String>) =
        webClient.get()
            .uri("/sak/$gsakSaksnummer/bucer?statuser=${statuser.joinToString(",")}")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(typeReference<List<BucinfoDto>>())
            .block()?.map { it.tilDomene() }?.toList()

    override fun hentSedGrunnlag(rinaSaksnummer: String, rinaDokumentID: String) =
        webClient.get()
            .uri("/buc/$rinaSaksnummer/sed/$rinaDokumentID/grunnlag")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(SedGrunnlagDto::class.java)
            .block()

    override fun lukkBuc(rinaSaksnummer: String) {
        webClient.post()
            .uri("/buc/$rinaSaksnummer/lukk")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(Void::class.java)
            .block()
    }

    override fun hentMuligeAksjoner(rinaSaksnummer: String) =
        webClient.get()
            .uri("/buc/$rinaSaksnummer/aksjoner")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(typeReference<List<String>>())
            .block()

    private inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}
}
