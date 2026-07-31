package no.nav.melosys.domain.brev.tekstblokk

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Lob
import jakarta.persistence.Table
import java.time.Instant

import no.nav.melosys.domain.RegistreringsInfo
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import org.hibernate.envers.AuditOverride
import org.hibernate.envers.Audited
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@Entity
@Table(name = "TEKSTBLOKK")
// Envers gir versjonshistorikk uten skriveside-kode – alle mutasjoner går gjennom
// TekstblokkService. AuditOverride tar med registreringsfeltene fra superklassen.
@Audited
@AuditOverride(forClass = RegistreringsInfo::class)
@EntityListeners(AuditingEntityListener::class)
class Tekstblokk(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "tittel", nullable = false)
    var tittel: String = "",

    @Lob
    @Column(name = "innhold", nullable = false)
    var innhold: String = "",

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    var type: TekstblokkType = TekstblokkType.TEKSTBLOKK,

    // Denormalisert fra Azure AD ved lagring, så listevisningen slipper oppslag per rad.
    @Column(name = "endret_av_navn")
    var endretAvNavn: String? = null,

    // Null = aktiv. Se V164 – sletting skjuler raden i stedet for å fjerne den.
    @Column(name = "slettet_dato")
    var slettetDato: Instant? = null,

    // Utkast er ukvalitetssikret vedtakstekst og vises kun for administratorer. Se V167.
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: TekstblokkStatus = TekstblokkStatus.PUBLISERT,

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "TEKSTBLOKK_TAG", joinColumns = [JoinColumn(name = "tekstblokk_id")])
    @Column(name = "tag", nullable = false)
    val tags: MutableSet<String> = mutableSetOf(),

    // Tom = gjelder alle sakstyper. Se V165.
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "TEKSTBLOKK_SAKSTYPE", joinColumns = [JoinColumn(name = "tekstblokk_id")])
    @Enumerated(EnumType.STRING)
    @Column(name = "sakstype", nullable = false)
    val sakstyper: MutableSet<Sakstyper> = mutableSetOf(),

    // Tom = gjelder alle behandlingstemaer. Se V165.
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "TEKSTBLOKK_BEHANDLINGSTEMA", joinColumns = [JoinColumn(name = "tekstblokk_id")])
    @Enumerated(EnumType.STRING)
    @Column(name = "behandlingstema", nullable = false)
    val behandlingstemaer: MutableSet<Behandlingstema> = mutableSetOf(),
) : RegistreringsInfo()
