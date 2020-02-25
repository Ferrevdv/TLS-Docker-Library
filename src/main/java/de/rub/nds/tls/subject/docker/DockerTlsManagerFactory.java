package de.rub.nds.tls.subject.docker;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.Image;
import de.rub.nds.tls.subject.ConnectionRole;
import de.rub.nds.tls.subject.HostInfo;
import de.rub.nds.tls.subject.TlsInstance;
import de.rub.nds.tls.subject.TlsImplementationType;
import de.rub.nds.tls.subject.exceptions.DefaultProfileNotFoundException;
import de.rub.nds.tls.subject.exceptions.ImplementationDidNotStartException;
import de.rub.nds.tls.subject.exceptions.PropertyNotFoundException;
import de.rub.nds.tls.subject.params.ParameterProfile;
import de.rub.nds.tls.subject.params.ParameterProfileManager;
import de.rub.nds.tls.subject.properties.ImageProperties;
import de.rub.nds.tls.subject.properties.PropertyManager;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Creates TLS-Server or TLS-Client Instances as Docker Container Holds the
 * Config for each TLS-Server or TLS-Client
 */
public class DockerTlsManagerFactory {

    private static final DockerClient DOCKER = new DefaultDockerClient("unix:///var/run/docker.sock");
    private static final Logger LOGGER = LogManager.getLogger(DockerTlsManagerFactory.class);

    private static final int DEFAULT_PORT = 4433;
    private final ParameterProfileManager parameterManager;
    private final PropertyManager propertyManager;
    private final DockerSpotifyTlsInstanceManager instanceManager;

    private static final int SERVER_POLL_INTERVAL_MILLISECONDS = 50;

    private static final int TIMEOUT_WAIT_FOR_SERVER_SPINUP_MILLISECONDS = 10000;

    public DockerTlsManagerFactory() {
        parameterManager = new ParameterProfileManager();
        propertyManager = new PropertyManager();
        instanceManager = new DockerSpotifyTlsInstanceManager();
    }

    public TlsInstance getClient(TlsImplementationType type, String version, String ip) {
        return getClient(type, version, ip, null);
    }

    public TlsInstance getClient(TlsImplementationType type, String version, String ip, String hostname) {
        return getClient(type, version, ip, hostname, DEFAULT_PORT);
    }

    public TlsInstance getClient(TlsImplementationType type, String version, String ip, int port) {
        return getClient(type, version, ip, null, port);
    }

    public TlsInstance getClient(TlsImplementationType type, String version, String ip, String hostname, int port) {
        return getClient(type, version, ip, hostname, port, null);
    }

    public TlsInstance getClient(TlsImplementationType type, String version, String ip, int port, String additionalParams) {
        return getClient(type, version, ip, null, port, additionalParams);
    }

    public TlsInstance getClient(TlsImplementationType type, String version, String ip, String hostname, int port, String additionalParams) {
        HostInfo hostInfo = new HostInfo(ip, hostname, port);
        return getInstance(ConnectionRole.CLIENT, type, version, hostInfo, additionalParams);
    }

    public TlsInstance getServer(TlsImplementationType type, String version) {
        return getServer(type, version, DEFAULT_PORT);
    }

    public TlsInstance getServer(TlsImplementationType type, String version, String hostname) {
        return getServer(type, version, hostname, DEFAULT_PORT);
    }

    public TlsInstance getServer(TlsImplementationType type, String version, int port) {
        return getServer(type, version, null, port);
    }

    public TlsInstance getServer(TlsImplementationType type, String version, String hostname, int port) {
        return getServer(type, version, hostname, port, null);
    }

    public TlsInstance getServer(TlsImplementationType type, String version, int port, String additionalParams) {
        return getServer(type, version, null, port, additionalParams);
    }

    public TlsInstance getServer(TlsImplementationType type, String version, String hostname, int port, String additionalParams) {
        HostInfo hostInfo = new HostInfo(hostname, port);
        return getInstance(ConnectionRole.SERVER, type, version, hostInfo, additionalParams);
    }

    private TlsInstance getInstance(ConnectionRole role, TlsImplementationType type, String version, HostInfo hostInfo, String additionalParams) {
        ParameterProfile profile = parameterManager.getProfile(type, version, role);
        if (profile == null) {
            throw new DefaultProfileNotFoundException("Could not find a Profile for " + role.name() + ": " + type.name() + ":" + version);
        }
        ImageProperties properties = propertyManager.getProperties(role, type);
        if (properties == null) {
            throw new PropertyNotFoundException("Could not find a Property for " + role.name() + ": " + type.name() + ":" + version);
        }
        TlsInstance instance = null;
        switch (role) {
            case CLIENT:
                instance = instanceManager.getTlsInstance(role, properties, profile, version, hostInfo, additionalParams);
                break;
            case SERVER:
                hostInfo.updatePort(properties.getInternalPort());
                instance = instanceManager.getTlsInstance(role, properties, profile, version, hostInfo, additionalParams);
                waitUntilServerIsOnline(hostInfo.getHostname(), instance.getPort(), instance);
                break;
            default:
                throw new IllegalArgumentException("Unknown ConnectionRole: " + role.name());
        }
        return instance;
    }

    public void waitUntilServerIsOnline(String host, int port, TlsInstance instance) {
        long startTime = System.currentTimeMillis();
        while (!isServerOnline(host, port)) {
            if (startTime + TIMEOUT_WAIT_FOR_SERVER_SPINUP_MILLISECONDS < System.currentTimeMillis()) {
                LOGGER.error("Could not start:\n\n" + instance.getLogs());
                throw new ImplementationDidNotStartException("Timeout");
            }
            try {
                Thread.sleep(SERVER_POLL_INTERVAL_MILLISECONDS);
            } catch (InterruptedException ex) {
                LOGGER.error("Could not start:\n\n" + instance.getLogs());
                throw new ImplementationDidNotStartException("Interrupted while waiting for Server", ex);
            }
        }
    }

    public boolean isServerOnline(String address, int port) {
        try {
            Socket ss = new Socket(address, port);
            if (ss.isConnected()) {
                ss.close();

                return true;
            } else {
                return false;
            }
        } catch (IOException e) {
            //e.printStackTrace();
            LOGGER.debug("Server is not online yet");
            LOGGER.trace(e);
            return false;
        }
    }

    public List<String> getAvailableVersions(ConnectionRole role, TlsImplementationType type) {
        List<String> versionList = new LinkedList<>();
        try {
            List<Image> serverImageList = DOCKER.listImages(DockerClient.ListImagesParam.withLabel(instanceManager.getInstanceLabel(role), type.name().toLowerCase()));
            for (Image image : serverImageList) {
                if (image.labels() != null) {
                    String version = image.labels().get(instanceManager.getInstanceVersionLabel(role));
                    if (version != null) {
                        versionList.add(version);
                    }
                }
            }
            return versionList;
        } catch (DockerException | InterruptedException ex) {
            throw new RuntimeException("Could not retrieve available " + role.name() + " Versions!");
        }
    }
}