package no.nav.melosys.tjenester.gui.dto;

public class ServerinfoDto {

    private final String namespace;

    private final String cluster;

    private final String branchName;

    private final String longVersionHash;

    public ServerinfoDto(String namespace, String cluster, String branchName, String longVersionHash) {
        this.namespace = namespace;
        this.cluster = cluster;
        this.branchName = branchName;
        this.longVersionHash = longVersionHash;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getCluster() {
        return cluster;
    }

    public String getBranchName() {
        return branchName;
    }

    public String getLongVersionHash() {
        return longVersionHash;
    }
}
