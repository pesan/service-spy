package org.github.pesan.tools.servicespy.config;

public class ProxyServer {
	private String host = "0.0.0.0";
    private int port = -1;
    private boolean ssl = false;
    private String jksKeystore;
    private String jksPassword;
    private String pfxKeystore;
    private String pfxPassword;
    private String pemKeyPath;
    private String pemCertPath;

	public String getHost() { return host; }
	public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public boolean getSsl() { return ssl; }
    public void setSsl(boolean ssl) { this.ssl = ssl; }

    public String getJksKeystore() { return jksKeystore; }
    public void setJksKeystore(String jksKeystore) { this.jksKeystore = jksKeystore; }

    public String getJksPassword() { return jksPassword; }
    public void setJksPassword(String jksPassword) { this.jksPassword = jksPassword; }

    public String getPfxKeystore() { return pfxKeystore; }
    public void setPfxKeystore(String pfxKeystore) { this.pfxKeystore = pfxKeystore; }

    public String getPfxPassword() { return pfxPassword; }
    public void setPfxPassword(String pfxPassword) { this.pfxPassword = pfxPassword; }

    public String getPemKeyPath() { return pemKeyPath; }
    public void setPemKeyPath(String pemKeyPath) { this.pemKeyPath = pemKeyPath; }

    public String getPemCertPath() { return pemCertPath; }
    public void setPemCertPath(String pemCertPath) { this.pemCertPath = pemCertPath; }
}
