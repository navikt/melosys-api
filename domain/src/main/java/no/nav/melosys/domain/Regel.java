package no.nav.melosys.domain;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "REGEL")
public class Regel {

    @Id
    private Long id;

    private String referanse;

    private String forretningsVersjon;

    private String tekniskVersjon;

    private String type;
}
