package no.nav.melosys.domain;


import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "FULLMEKTIG")
public class Fullmektig extends Aktoer {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "idGen")
    @SequenceGenerator(name= "idGen", sequenceName = "SEQ_FULLMEKTIG")
    private Long id;
}
