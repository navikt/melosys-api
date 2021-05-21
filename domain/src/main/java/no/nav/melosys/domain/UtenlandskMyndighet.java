package no.nav.melosys.domain;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.persistence.*;

import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.kodeverk.Landkoder;

@Entity
@Table(name = "utenlandsk_myndighet")
public class UtenlandskMyndighet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public String institusjonskode;

    public String navn;

    public String gateadresse;

    public String postnummer;

    public String poststed;

    public String land;

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(
        name = "utenlandsk_myndighet_pref",
        joinColumns = @JoinColumn(name = "utenlandsk_myndighet_id", updatable = false),
        inverseJoinColumns = @JoinColumn(name = "preferanse_id", updatable = false)
    )
    public Set<Preferanse> preferanser = new HashSet<>();

    @Enumerated(EnumType.STRING)
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

    public StrukturertAdresse getAdresse() {
        StrukturertAdresse adresse = new StrukturertAdresse();
        adresse.gatenavn = gateadresse;
        adresse.postnummer = postnummer;
        adresse.poststed = poststed;
        adresse.landkode = landkode.getKode();
        return adresse;
    }
}
