package no.nav.melosys.service.avklartefakta;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.avklartefakta.AvklartefaktaRegistrering;
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper;

public class AvklartefaktaDto {
    private String subjektID;

    private Avklartefaktatyper avklartefaktaType;

    private String referanse;

    private List<String> fakta;

    private List<String> begrunnelseKoder;

    private String begrunnelseFritekst;

    public String getSubjektID() {
        return subjektID;
    }

    public void setSubjektID(String subjektID) {
        this.subjektID = subjektID;
    }

    public Avklartefaktatyper getAvklartefaktaType() {
        return avklartefaktaType;
    }

    @JsonProperty("avklartefaktaKode")
    public String getAvklartefaktakodeKunKode() {
        return avklartefaktaType != null ? avklartefaktaType.getKode() : null;
    }

    @JsonProperty("avklartefaktaKode")
    public void setAvklartefaktaType(Avklartefaktatyper avklartefakta) {
        this.avklartefaktaType = avklartefakta;
    }

    public String getReferanse() {
        return referanse;
    }

    public void setReferanse(String referanse) {
        this.referanse = referanse;
    }

    public List<String> getFakta() {
        return fakta;
    }

    public void setFakta(List<String> fakta) {
        this.fakta = fakta;
    }

    public List<String> getBegrunnelseKoder() {
        return begrunnelseKoder;
    }

    public void setBegrunnelseKoder(List<String> begrunnelseKoder) {
        this.begrunnelseKoder = begrunnelseKoder;
    }

    public boolean harBegrunnelseKoder() {
        return begrunnelseKoder != null && !begrunnelseKoder.isEmpty();
    }

    public String getBegrunnelseFritekst() {
        return begrunnelseFritekst;
    }

    public void setBegrunnelseFritekst(String begrunnelseFritekst) {
        this.begrunnelseFritekst = begrunnelseFritekst;
    }

    @JsonCreator
    public AvklartefaktaDto(@JsonProperty("fakta") List<String> fakta,
                            @JsonProperty("referanse") String referanse) {
        this.fakta = fakta;
        this.referanse = referanse;
    }

    public AvklartefaktaDto(Avklartefakta avklartefakta) {
        this.subjektID = avklartefakta.getSubjekt();
        this.avklartefaktaType = avklartefakta.getType();
        this.referanse = avklartefakta.getReferanse();
        this.begrunnelseFritekst = avklartefakta.getBegrunnelseFritekst();
        this.fakta = Arrays.asList(avklartefakta.getFakta().split(" "));

        Set<AvklartefaktaRegistrering> registreringer = avklartefakta.getRegistreringer();
        this.begrunnelseKoder = registreringer.stream()
                .map(AvklartefaktaRegistrering::getBegrunnelseKode)
                .collect(Collectors.toList());
    }
}
