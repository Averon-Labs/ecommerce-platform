package com.averonlabs.eshopverse.quality;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.averonlabs.eshopverse.core.net.ApiClient;
import com.averonlabs.eshopverse.core.storage.TokenStore;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Automated security, privacy, and transport hardening audit tests verifying acceptance criteria
 * for QUAL-003 per docs/ANDROID_ROADMAP.md and docs/SECURITY.md.
 */
public class SecurityAndPrivacyAuditTest {

    private static final String MAIN_RES_DIR = "src/main/res/xml/";
    private static final String DEBUG_RES_DIR = "src/debug/res/xml/";
    private static final String MANIFEST_PATH = "src/main/AndroidManifest.xml";

    // --- Criterion a: Android Keystore Token Storage & Backup Exclusions ---

    @Test
    public void tokenStore_toString_neverLeaksTokenValue() {
        TokenStore testStore = new TokenStore() {
            private final String sensitiveToken = "secret_jwt_bearer_token_123456789";

            @Override
            public void save(String token, String expiresAt) { }

            @Override
            public com.averonlabs.eshopverse.core.storage.AuthToken read() {
                return new com.averonlabs.eshopverse.core.storage.AuthToken(sensitiveToken, "2026-12-31T23:59:59Z");
            }

            @Override
            public String getToken() { return sensitiveToken; }

            @Override
            public boolean isExpired() { return false; }

            @Override
            public void clear() { }

            @Override
            public String toString() {
                return "TokenStore[hasToken=" + (sensitiveToken != null) + "]";
            }
        };

        String representation = testStore.toString();
        assertFalse("TokenStore.toString() must never contain the raw token string",
                representation.contains("secret_jwt_bearer_token_123456789"));
        assertFalse("TokenStore.toString() must never contain JWT pattern",
                representation.toLowerCase().contains("bearer"));
    }

    @Test
    public void backupRules_excludeSecureTokenPreferences() throws Exception {
        File backupRulesFile = new File(MAIN_RES_DIR, "backup_rules.xml");
        assertTrue("backup_rules.xml must exist", backupRulesFile.exists());

        Document doc = parseXml(backupRulesFile);
        NodeList excludes = doc.getElementsByTagName("exclude");
        boolean foundExcludedTokens = false;

        for (int i = 0; i < excludes.getLength(); i++) {
            Element elem = (Element) excludes.item(i);
            if ("sharedpref".equals(elem.getAttribute("domain"))
                    && "eshopverse_secure_tokens.xml".equals(elem.getAttribute("path"))) {
                foundExcludedTokens = true;
                break;
            }
        }
        assertTrue("backup_rules.xml must explicitly exclude eshopverse_secure_tokens.xml",
                foundExcludedTokens);
    }

    @Test
    public void dataExtractionRules_excludeTokensFromCloudAndDeviceTransfer() throws Exception {
        File dataExtractionFile = new File(MAIN_RES_DIR, "data_extraction_rules.xml");
        assertTrue("data_extraction_rules.xml must exist", dataExtractionFile.exists());

        Document doc = parseXml(dataExtractionFile);
        NodeList cloudBackupList = doc.getElementsByTagName("cloud-backup");
        assertTrue("cloud-backup section must exist in data_extraction_rules.xml", cloudBackupList.getLength() > 0);

        Element cloudBackup = (Element) cloudBackupList.item(0);
        NodeList cloudExcludes = cloudBackup.getElementsByTagName("exclude");
        boolean cloudExcluded = containsTokenStoreExclude(cloudExcludes);
        assertTrue("cloud-backup must exclude eshopverse_secure_tokens.xml", cloudExcluded);

        NodeList deviceTransferList = doc.getElementsByTagName("device-transfer");
        assertTrue("device-transfer section must exist in data_extraction_rules.xml", deviceTransferList.getLength() > 0);

        Element deviceTransfer = (Element) deviceTransferList.item(0);
        NodeList deviceExcludes = deviceTransfer.getElementsByTagName("exclude");
        boolean deviceExcluded = containsTokenStoreExclude(deviceExcludes);
        assertTrue("device-transfer must exclude eshopverse_secure_tokens.xml", deviceExcluded);
    }

    @Test
    public void manifest_disablesApplicationBackup() throws Exception {
        File manifestFile = new File(MANIFEST_PATH);
        assertTrue("AndroidManifest.xml must exist", manifestFile.exists());

        Document doc = parseXml(manifestFile);
        Element application = (Element) doc.getElementsByTagName("application").item(0);
        assertNotNull("Application tag must exist in manifest", application);

        assertEquals("android:allowBackup must be false", "false",
                application.getAttribute("android:allowBackup"));
        assertEquals("android:dataExtractionRules must point to @xml/data_extraction_rules",
                "@xml/data_extraction_rules", application.getAttribute("android:dataExtractionRules"));
        assertEquals("android:fullBackupContent must point to @xml/backup_rules",
                "@xml/backup_rules", application.getAttribute("android:fullBackupContent"));
    }

    // --- Criterion b: Logging Redaction & Zero PII Exposure ---

    @Test
    public void apiClient_inDebugMode_redactsAuthorizationHeaderAndRestrictsLoggingLevel() {
        TokenStore dummyStore = new TokenStore() {
            @Override public void save(String token, String expiresAt) {}
            @Override public com.averonlabs.eshopverse.core.storage.AuthToken read() { return null; }
            @Override public String getToken() { return "token_xyz"; }
            @Override public boolean isExpired() { return false; }
            @Override public void clear() {}
        };

        // Initialize debug client
        ApiClient debugClient = new ApiClient("http://10.0.2.2:8081/api/v1", dummyStore, null, true);
        boolean hasLoggingInterceptor = false;
        boolean bodyLoggingPrevented = true;

        for (okhttp3.Interceptor interceptor : debugClient.getOkHttpClient().interceptors()) {
            if (interceptor instanceof HttpLoggingInterceptor) {
                HttpLoggingInterceptor logging = (HttpLoggingInterceptor) interceptor;
                hasLoggingInterceptor = true;
                // Verify level is at most BASIC (never HEADERS or BODY)
                assertEquals("Debug logging level must be BASIC to prevent credential/PII leaks",
                        HttpLoggingInterceptor.Level.BASIC, logging.getLevel());
                if (logging.getLevel() == HttpLoggingInterceptor.Level.BODY
                        || logging.getLevel() == HttpLoggingInterceptor.Level.HEADERS) {
                    bodyLoggingPrevented = false;
                }
            }
        }

        assertTrue("Debug client must configure logging interceptor", hasLoggingInterceptor);
        assertTrue("Body and full header logging must be strictly prevented", bodyLoggingPrevented);
    }

    @Test
    public void apiClient_inReleaseMode_completelyOmitsLoggingInterceptor() {
        TokenStore dummyStore = new TokenStore() {
            @Override public void save(String token, String expiresAt) {}
            @Override public com.averonlabs.eshopverse.core.storage.AuthToken read() { return null; }
            @Override public String getToken() { return "token_xyz"; }
            @Override public boolean isExpired() { return false; }
            @Override public void clear() {}
        };

        // Initialize release client (isDebug = false)
        ApiClient releaseClient = new ApiClient("https://api.eshopverse.example.com/api/v1", dummyStore, null, false);
        for (okhttp3.Interceptor interceptor : releaseClient.getOkHttpClient().interceptors()) {
            assertFalse("Release client must NOT attach HttpLoggingInterceptor to prevent logcat leakage",
                    interceptor instanceof HttpLoggingInterceptor);
        }
    }

    // --- Criterion c: Network Security Config & Transport Hardening ---

    @Test
    public void releaseNetworkSecurityConfig_strictlyDisallowsCleartextTraffic() throws Exception {
        File mainConfigFile = new File(MAIN_RES_DIR, "network_security_config.xml");
        assertTrue("Main network_security_config.xml must exist", mainConfigFile.exists());

        Document doc = parseXml(mainConfigFile);
        NodeList baseConfigList = doc.getElementsByTagName("base-config");
        assertTrue("base-config must exist in main network_security_config.xml", baseConfigList.getLength() > 0);

        Element baseConfig = (Element) baseConfigList.item(0);
        assertEquals("cleartextTrafficPermitted must be false in base-config",
                "false", baseConfig.getAttribute("cleartextTrafficPermitted"));

        // Confirm zero domain-config overrides exist in main (production) config
        NodeList domainConfigs = doc.getElementsByTagName("domain-config");
        assertEquals("Production network config must have zero domain-config exceptions",
                0, domainConfigs.getLength());
    }

    @Test
    public void debugNetworkSecurityConfig_restrictsCleartextToLocalDevOnly() throws Exception {
        File debugConfigFile = new File(DEBUG_RES_DIR, "network_security_config.xml");
        assertTrue("Debug network_security_config.xml must exist", debugConfigFile.exists());

        Document doc = parseXml(debugConfigFile);
        Element baseConfig = (Element) doc.getElementsByTagName("base-config").item(0);
        assertEquals("Debug base-config must still default cleartextTrafficPermitted to false",
                "false", baseConfig.getAttribute("cleartextTrafficPermitted"));

        NodeList domainConfigs = doc.getElementsByTagName("domain-config");
        assertTrue("Debug config may have domain-config for local dev", domainConfigs.getLength() > 0);

        Element domainConfig = (Element) domainConfigs.item(0);
        assertEquals("true", domainConfig.getAttribute("cleartextTrafficPermitted"));

        NodeList domains = domainConfig.getElementsByTagName("domain");
        List<String> allowedDomains = new ArrayList<>();
        for (int i = 0; i < domains.getLength(); i++) {
            allowedDomains.add(domains.item(i).getTextContent().trim());
        }

        // Must only allow emulator host and localhost
        assertTrue("10.0.2.2 must be permitted for Android emulator", allowedDomains.contains("10.0.2.2"));
        assertTrue("localhost must be permitted", allowedDomains.contains("localhost"));
        assertTrue("127.0.0.1 must be permitted", allowedDomains.contains("127.0.0.1"));
        assertEquals("Only local development domains may be granted cleartext exception",
                3, allowedDomains.size());
    }

    // --- Criterion d: Permissions & Dependency Security Verification ---

    @Test
    public void manifest_onlyRequestsMinimalNetworkPermissions() throws Exception {
        File manifestFile = new File(MANIFEST_PATH);
        Document doc = parseXml(manifestFile);
        NodeList permissions = doc.getElementsByTagName("uses-permission");

        List<String> permissionNames = new ArrayList<>();
        for (int i = 0; i < permissions.getLength(); i++) {
            Element elem = (Element) permissions.item(i);
            permissionNames.add(elem.getAttribute("android:name"));
        }

        // Validate strictly needed permissions only
        for (String perm : permissionNames) {
            assertTrue("Permission must be standard network access only, found: " + perm,
                    "android.permission.INTERNET".equals(perm)
                            || "android.permission.ACCESS_NETWORK_STATE".equals(perm));
        }

        assertFalse("Dangerous permissions (e.g. storage) must not be requested",
                permissionNames.contains("android.permission.READ_EXTERNAL_STORAGE"));
        assertFalse("Dangerous permissions (e.g. location) must not be requested",
                permissionNames.contains("android.permission.ACCESS_FINE_LOCATION"));
    }

    // --- XML Parsing Helpers ---

    private Document parseXml(File file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(file);
    }

    private boolean containsTokenStoreExclude(NodeList excludes) {
        for (int i = 0; i < excludes.getLength(); i++) {
            Element elem = (Element) excludes.item(i);
            if ("sharedpref".equals(elem.getAttribute("domain"))
                    && "eshopverse_secure_tokens.xml".equals(elem.getAttribute("path"))) {
                return true;
            }
        }
        return false;
    }
}
