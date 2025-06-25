package no.nav.melosys.domain.helseutgiftdekkesperiode

import jakarta.persistence.*
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.kodeverk.Land_iso2
import java.time.LocalDate


@Entity
@Table(name = "helseutgift_dekkes_periode")
class HelseutgiftDekkesPeriode(
    @OneToOne(optional = false)
    @JoinColumn(name = "beh_resultat_id", nullable = false)
    val behandlingsresultat: Behandlingsresultat,

    @Column(name = "fom_dato", nullable = false)
    var fomDato: LocalDate,

    @Column(name = "tom_dato", nullable = false)
    var tomDato: LocalDate,

    @Enumerated(EnumType.STRING)
    @Column(name = "bosted_landkode", nullable = false)
    var bostedLandkode: Land_iso2
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private val id: Long? = null
}
