package org.example.classesRenan;

public class ParametrosServidor {
    public Double maxCpuUso;
    public Double maxCpuTemp;
    public Double maxRam;
    public Double maxDiscoUso;
    public Double maxDiscoTemp;
    public Long maxRedeDownload;
    public Long maxRedeUpload;


    public ParametrosServidor() {

    }

    @Override
    public String toString() {
        return "ParametrosServidor{" +
                "maxCpuUso=" + maxCpuUso +
                ", maxCpuTemp=" + maxCpuTemp +
                ", maxRam=" + maxRam +
                ", maxDiscoUso=" + maxDiscoUso +
                ", maxDiscoTemp=" + maxDiscoTemp +
                ", maxRedeDownload=" + maxRedeDownload +
                ", maxRedeUpload=" + maxRedeUpload +
                '}';
    }

    public Double getMaxCpuUso() {
        return maxCpuUso;
    }

    public void setMaxCpuUso(Double maxCpuUso) {
        this.maxCpuUso = maxCpuUso;
    }

    public Long getMaxRedeUpload() {
        return maxRedeUpload;
    }

    public void setMaxRedeUpload(Long maxRedeUpload) {
        this.maxRedeUpload = maxRedeUpload;
    }

    public Long getMaxRedeDownload() {
        return maxRedeDownload;
    }

    public void setMaxRedeDownload(Long maxRedeDownload) {
        this.maxRedeDownload = maxRedeDownload;
    }

    public Double getMaxDiscoTemp() {
        return maxDiscoTemp;
    }

    public void setMaxDiscoTemp(Double maxDiscoTemp) {
        this.maxDiscoTemp = maxDiscoTemp;
    }

    public Double getMaxDiscoUso() {
        return maxDiscoUso;
    }

    public void setMaxDiscoUso(Double maxDiscoUso) {
        this.maxDiscoUso = maxDiscoUso;
    }

    public Double getMaxRam() {
        return maxRam;
    }

    public void setMaxRam(Double maxRam) {
        this.maxRam = maxRam;
    }

    public Double getMaxCpuTemp() {
        return maxCpuTemp;
    }

    public void setMaxCpuTemp(Double maxCpuTemp) {
        this.maxCpuTemp = maxCpuTemp;
    }
}