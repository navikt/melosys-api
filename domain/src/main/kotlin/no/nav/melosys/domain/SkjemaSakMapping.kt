package no.nav.melosys.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "skjema_sak_mapping")
class SkjemaSakMapping(
    @Id
    @Column(name = "skjema_id", nullable = false)
    val skjemaId: UUID,

    @Column(name = "saksnummer", nullable = false)
    val saksnummer: String,

    @Column(name = "mottatte_opplysninger_id")
    var mottatteOpplysningerId: Long? = null,

    @Column(name = "original_data", columnDefinition = "CLOB")
    var originalData: String? = null,

    @Column(name = "journalpost_id")
    var journalpostId: String? = null,

    @Column(name = "innsendt_dato")
    var innsendtDato: Instant? = null,

    @Column(name = "opprettet_dato", nullable = false, updatable = false)
    val opprettetDato: Instant = Instant.now()
)
