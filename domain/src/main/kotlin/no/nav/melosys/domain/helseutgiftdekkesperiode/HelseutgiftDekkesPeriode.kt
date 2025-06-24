package no.nav.melosys.domain.helseutgiftdekkesperiode

import jakarta.persistence.*
import no.nav.melosys.domain.Behandlingsresultat
import java.time.LocalDate


@Entity
@Table(name = "helseutgift_dekkes_periode")
class HelseutgiftDekkesPeriode(
    @OneToOne(optional = false)
    @JoinColumn(name = "beh_resultat_id", nullable = false)
    val behandlingsresultat: Behandlingsresultat,

    @Column(name = "fom_dato", nullable = false)
    val fomDato: LocalDate,

    @Column(name = "tom_dato", nullable = false)
    val tomDato: LocalDate,

    @Column(name = "bosted_landkode", nullable = false)
    val bostedLandkode: String
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private val id: Long? = null
}
