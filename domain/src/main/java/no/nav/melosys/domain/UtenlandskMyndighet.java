package no.nav.melosys.domain;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.*;

import no.nav.melosys.domain.kodeverk.Landkoder;

@Entity
@Table(name = "utenlandsk_myndighet")
public class UtenlandskMyndighet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    public String institusjonskode;

    public String navn;

    public String gateadresse;

    public String postnummer;

    public String poststed;

    public String land;

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(
        name = "utenlandsk_myndighet_preferanse",
        joinColumns = @JoinColumn(name = "utenlandsk_myndighet_id", updatable = false),
        inverseJoinColumns = @JoinColumn(name = "preferanse_id", updatable = false)
    )
    public Set<Preferanse> preferanser = new HashSet<>();

    @Convert(converter = Landkoder.DbKonverterer.class)
    public Landkoder landkode;

}
