package no.nav.melosys.tjenester.gui.dto;

public class ServerinfoDto {

    private final String namespace;

    private final String cluster;

    private final String branchName;

    private final String longVersionHash;

    private final String veraUrl;

    public ServerinfoDto(String namespace, String cluster, String branchName, String longVersionHash, String veraUrl) {
        this.namespace = namespace;
        this.cluster = cluster;
        this.branchName = branchName;
        this.longVersionHash = longVersionHash;
        this.veraUrl = veraUrl;
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

    public String getVeraUrl() {
        return veraUrl;
    }
}
