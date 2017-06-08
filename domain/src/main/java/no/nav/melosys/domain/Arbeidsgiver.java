package no.nav.melosys.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "ARBEIDSGIVER")
public class Arbeidsgiver extends Aktoer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

}
