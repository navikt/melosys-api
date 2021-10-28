package no.nav.melosys.integrasjon.dokgen.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import no.nav.melosys.domain.brev.InnvilgelseBrevbestilling;
import no.nav.melosys.domain.brev.storbritannia.AttestStorbritanniaBrevbestilling;
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.*;

import java.time.Instant;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public class TrygdeavteleAtestDto extends DokgenDto {

    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = STRING)
    private final Instant datoMottatt;

    private final List<FamiliemedlemInfo> omfattetFamilie;
    private final List<IkkeOmfattetBarn> ikkeOmfattetBarn;
    private final IkkeOmfattetEktefelle ikkeOmfattetEktefelle;
    private final String saksbehandlerNavn;

    public TrygdeavteleAtestDto(AttestStorbritanniaBrevbestilling brevbestilling,
                                List<FamiliemedlemInfo> omfattetFamilie,
                                List<IkkeOmfattetBarn> ikkeOmfattetBarn,
                                IkkeOmfattetEktefelle ikkeOmfattetEktefelle) {
        super(brevbestilling);
        this.datoMottatt = brevbestilling.getForsendelseMottatt();
        this.saksbehandlerNavn = brevbestilling.getSaksbehandlerNavn();
        this.omfattetFamilie = omfattetFamilie;
        this.ikkeOmfattetBarn = ikkeOmfattetBarn;
        this.ikkeOmfattetEktefelle = ikkeOmfattetEktefelle;
    }

    public Instant getDatoMottatt() {
        return datoMottatt;
    }

    public List<FamiliemedlemInfo> getOmfattetFamilie() {
        return omfattetFamilie;
    }

    public List<IkkeOmfattetBarn> getIkkeOmfattetBarn() {
        return ikkeOmfattetBarn;
    }

    public IkkeOmfattetEktefelle getIkkeOmfattetEktefelle() {
        return ikkeOmfattetEktefelle;
    }

    public String getSaksbehandlerNavn() {
        return saksbehandlerNavn;
    }
}
