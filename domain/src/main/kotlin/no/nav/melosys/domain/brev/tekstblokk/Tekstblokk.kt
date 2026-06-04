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

import no.nav.melosys.domain.RegistreringsInfo
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@Entity
@Table(name = "TEKSTBLOKK")
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

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "TEKSTBLOKK_TAG", joinColumns = [JoinColumn(name = "tekstblokk_id")])
    @Column(name = "tag", nullable = false)
    val tags: MutableSet<String> = mutableSetOf(),
) : RegistreringsInfo()
