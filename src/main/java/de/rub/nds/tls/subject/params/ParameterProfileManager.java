package de.rub.nds.tls.subject.params;

import de.rub.nds.tls.subject.ConnectionRole;
import de.rub.nds.tls.subject.TlsImplementationType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ParameterProfileManager {

    static final Logger LOGGER = LogManager.getLogger(ParameterProfileSerializer.class.getName());

    private static final String RESOURCE_PATH = "/profiles/";

    private final List<ParameterProfile> defaultClientProfileList;

    private final List<ParameterProfile> allProfileList;

    private final List<ParameterProfile> defaultServerProfileList;

    public ParameterProfileManager() {
        defaultServerProfileList = new LinkedList<>();
        defaultClientProfileList = new LinkedList<>();
        allProfileList = new LinkedList<>();

        for (ConnectionRole role : ConnectionRole.values()) {
            try {
                for (String filename : getResourceFiles(RESOURCE_PATH + role.name().toLowerCase() + "/")) {
                    ParameterProfile profile = tryLoadProfile(role, filename);
                    if (profile != null) {
                        LOGGER.debug("Loaded:" + profile.getName() + " : " + profile.getRole().name() + " - " + profile.getDescription());
                        allProfileList.add(profile);
                    }
                }
            } catch (IOException ex) {
                LOGGER.warn("Problem reading profiles", ex);
                ex.printStackTrace();
            }
        }

        for (TlsImplementationType type
                : TlsImplementationType.values()) {
            ParameterProfile profile = tryLoadProfile(ConnectionRole.SERVER, "" + type.name().toLowerCase() + ".profile");
            if (profile != null) {
                LOGGER.debug("Loaded:" + profile.getName() + " : " + profile.getRole().name());
                defaultServerProfileList.add(profile);
            }
            profile = tryLoadProfile(ConnectionRole.CLIENT, "" + type.name().toLowerCase() + ".profile");
            if (profile != null) {
                LOGGER.debug("Loaded:" + profile.getName() + " : " + profile.getRole().name());
                defaultClientProfileList.add(profile);
            }
        }
    }

    private List<String> getResourceFiles(String path) throws IOException {
        List<String> filenames = new ArrayList<>();
        try (
                InputStream in = getResourceAsStream(path);
                BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            String resource;
            while ((resource = br.readLine()) != null) {
                filenames.add(resource);
            }
        }

        return filenames;
    }

    private InputStream getResourceAsStream(String resource) {
        InputStream in = getContextClassLoader().getResourceAsStream(resource);
        return in == null ? getClass().getResourceAsStream(resource) : in;
    }

    private ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();

    }

    private ParameterProfile tryLoadProfile(ConnectionRole role, String filename) {
        try {
            InputStream stream = ParameterProfileManager.class
                    .getResourceAsStream(RESOURCE_PATH + role.name().toLowerCase() + "/" + filename);
            return ParameterProfileSerializer.read(stream);
        } catch (IOException | JAXBException | XMLStreamException E) {
            LOGGER.debug("Could not find other ParameterProfile for: " + RESOURCE_PATH + role.name().toLowerCase() + "/" + filename + ": " + role.name());
            LOGGER.trace(E);
            return null;
        }
    }

    public ParameterProfile getProfile(TlsImplementationType type, String version, ConnectionRole role) {
        for (ParameterProfile profile : allProfileList) {
            if (profile.getRole() == role && profile.getType() == type) {
                if (profile.getVersionList() != null && !profile.getVersionList().isEmpty()) {
                    for (String versionRegex : profile.getVersionList()) {
                        if (version.matches(versionRegex)) {
                            return profile;
                        }
                    }
                }
            }
        }
        return getDefaultProfile(type, role);
    }

    public ParameterProfile getDefaultProfile(TlsImplementationType type, ConnectionRole role) {
        if (null == role) {
            throw new IllegalArgumentException("Unknown ConnectionRole: " + role.name());
        } else {
            switch (role) {
                case CLIENT:
                    for (ParameterProfile profile : defaultClientProfileList) {
                        if (profile.getType() == type) {
                            return profile;
                        }
                    }
                    return null;
                case SERVER:
                    for (ParameterProfile profile : defaultServerProfileList) {
                        if (profile.getType() == type) {
                            return profile;
                        }
                    }
                    return null;
                default:
                    throw new IllegalArgumentException("Unknown ConnectionRole: " + role.name());
            }
        }
    }
}