package no.nav.melosys.domain.helseutgiftdekkesperiode

import jakarta.persistence.*
import no.nav.melosys.domain.Behandlingsresultat
import java.time.LocalDate


@Entity
@Table(name = "helseutgift_dekkes_periode")
class HelseutgiftDekkesPeriode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private val id: Long? = null

    @OneToOne(optional = false)
    @JoinColumn(name = "beh_resultat_id", nullable = false, updatable = false)
    var behandlingsresultat: Behandlingsresultat? = null

    @Column(name = "fom_dato", nullable = false)
    lateinit var fomDato: LocalDate

    @Column(name = "tom_dato", nullable = true)
    var tomDato: LocalDate? = null

    @Column(name = "bostedsland", nullable = false)
    lateinit var bostedsland: String
}
