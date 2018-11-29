package no.nav.melosys.domain;

import javax.persistence.*;

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

    @Column(name = "omraade")
    @Convert(converter = Omraade.DbKonverterer.class)
    public Omraade område;

}
