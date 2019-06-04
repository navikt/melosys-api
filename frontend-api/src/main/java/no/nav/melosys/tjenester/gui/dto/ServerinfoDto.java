package no.nav.melosys.tjenester.gui.dto;

public class ServerinfoDto {

    private String namespace;

    private String cluster;

    private String branchName;

    private String longVersionHash;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public String getLongVersionHash() {
        return longVersionHash;
    }

    public void setLongVersionHash(String longVersionHash) {
        this.longVersionHash = longVersionHash;
    }
}
