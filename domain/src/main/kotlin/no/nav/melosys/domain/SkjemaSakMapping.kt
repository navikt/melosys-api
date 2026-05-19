package no.nav.melosys.domain

import jakarta.persistence.*
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "skjema_sak_mapping") //TODO: rename?
class SkjemaSakMapping(

    @Id
    @Column(name = "skjema_id", nullable = false)
    val skjemaId: UUID,

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "saksnummer", nullable = false, updatable = false)
    val fagsak: Fagsak,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mottatte_opplysninger_id")
    var mottatteOpplysninger: MottatteOpplysninger,

    @Column(name = "original_data", columnDefinition = "CLOB")
    var originalData: String, //TODO: Nødvendig med nullable?

    @Column(name = "journalpost_id")
    var journalpostId: String? = null, //TODO: Nødvendig med nullable?

    @Column(name = "innsendt_dato")
    var innsendtDato: Instant? = null, //TODO: Hvorfor brukes det ikke?

    @Column(name = "opprettet_dato", nullable = false, updatable = false)
    val opprettetDato: Instant = Instant.now()
) {
    val saksnummer: String get() = fagsak.saksnummer
}
