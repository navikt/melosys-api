package no.nav.melosys.service.avklartefakta;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.melosys.domain.Avklartefakta;
import no.nav.melosys.domain.AvklartefaktaType;
import no.nav.melosys.domain.AvklartefaktaRegistrering;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AvklartefaktaDto {
    private String subjektID;

    private AvklartefaktaType avklartefaktaKode;

    private String referanse;

    private List<String> fakta;

    private List<String> begrunnelsekoder;

    private String begrunnelsefritekst;

    public String getSubjektID() {
        return subjektID;
    }

    public void setSubjektID(String subjektID) {
        this.subjektID = subjektID;
    }

    public AvklartefaktaType getAvklartefaktaKode() {
        return avklartefaktaKode;
    }

    public void setAvklartefaktaKode(AvklartefaktaType avklartefaktaKode) {
        this.avklartefaktaKode = avklartefaktaKode;
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

    public List<String> getBegrunnelsekoder() {
        return begrunnelsekoder;
    }

    public void setBegrunnelsekoder(List<String> begrunnelsekoder) {
        this.begrunnelsekoder = begrunnelsekoder;
    }

    public boolean harBegrunnelsefritekst() {
        return begrunnelsefritekst != null && !begrunnelsefritekst.isEmpty();
    }

    public boolean harBegrunnelseKoder() {
        return begrunnelsekoder != null && !begrunnelsekoder.isEmpty();
    }

    public String getBegrunnelsefritekst() {
        return begrunnelsefritekst;
    }

    public void setBegrunnelsefritekst(String begrunnelsefritekst) {
        this.begrunnelsefritekst = begrunnelsefritekst;
    }

    @JsonCreator
    public AvklartefaktaDto(@JsonProperty("fakta") List<String> fakta,
                            @JsonProperty("referanse") String referanse) {
        this.fakta = fakta;
        this.referanse = referanse;
    }

    public AvklartefaktaDto(Avklartefakta avklartefakta) {
        this.subjektID = avklartefakta.getSubjekt();
        this.avklartefaktaKode = avklartefakta.getAvklartefaktakode();
        this.referanse = avklartefakta.getReferanse();
        this.begrunnelsefritekst = avklartefakta.getBegrunnelseFritekst();

        String[] fakta = avklartefakta.getFakta().split(" ");
        this.fakta = Arrays.asList(fakta);

        Set<AvklartefaktaRegistrering> registreringer = avklartefakta.getRegistreringer();
        this.begrunnelsekoder = registreringer.stream()
                .map(AvklartefaktaRegistrering::getBegrunnelseKode)
                .collect(Collectors.toList());
    }
}
