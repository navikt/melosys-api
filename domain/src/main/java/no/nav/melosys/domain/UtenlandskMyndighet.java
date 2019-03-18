package no.nav.melosys.domain;

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

    @Column(name = "reservert_innvilgelse_brev", updatable = false)
    public boolean reservertMotInnvilgelsesInfo;

    @Convert(converter = Landkoder.DbKonverterer.class)
    public Landkoder landkode;

}
