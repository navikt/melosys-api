package no.nav.melosys.domain;

import java.util.HashSet;
import java.util.Set;
import java.util.Objects;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UtenlandskMyndighet)) return false;
        UtenlandskMyndighet that = (UtenlandskMyndighet) o;
        return institusjonskode.equals(that.institusjonskode) &&
            land.equals(that.land);
    }

    @Override
    public int hashCode() {
        return Objects.hash(institusjonskode, land);
    }
}
