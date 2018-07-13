package org.cloudfoundry.credhub.integration;

import org.cloudfoundry.credhub.CredentialManagerApp;
import org.cloudfoundry.credhub.data.CredentialVersionDataService;
import org.cloudfoundry.credhub.entity.EncryptionKeyCanary;
import org.cloudfoundry.credhub.repository.EncryptionKeyCanaryRepository;
import org.cloudfoundry.credhub.util.DatabaseProfileResolver;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.hamcrest.core.IsEqual;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Arrays;
import java.util.List;

import static org.cloudfoundry.credhub.helper.RequestHelper.getCertificateId;
import static org.cloudfoundry.credhub.util.AuthConstants.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@ActiveProfiles(value = {"unit-test", "unit-test-permissions"}, resolver = DatabaseProfileResolver.class)
@SpringBootTest(classes = CredentialManagerApp.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class BulkRegenerateTest {

  private static final String API_V1_DATA_ENDPOINT = "/api/v1/data";
  private static final String API_V1_BULK_REGENERATE_ENDPOINT = "/api/v1/bulk-regenerate";
  private static final String API_V1_PERMISSION_ENDPOINT = "/api/v1/permissions";

  @Autowired
  private WebApplicationContext webApplicationContext;

  @Autowired
  private CredentialVersionDataService credentialVersionDataService;

  @Autowired
  private Flyway flyway;

  @Autowired
  private EncryptionKeyCanaryRepository encryptionKeyCanaryRepository;

  private MockMvc mockMvc;
  private List<EncryptionKeyCanary> canaries;

  @Before
  public void beforeEach() throws Exception {
    canaries = encryptionKeyCanaryRepository.findAll();
    mockMvc = MockMvcBuilders
        .webAppContextSetup(webApplicationContext)
        .apply(springSecurity())
        .build();

    generateRootCA("/ca-to-rotate", "original ca");
    generateRootCA("/other-ca", "other ca");

    generateSignedCertificate("/cert-to-regenerate", "cert to regenerate", "/ca-to-rotate");
    generateSignedCertificate("/cert-to-regenerate", "cert to regenerate", "/ca-to-rotate");
    generateSignedCertificate("/cert-to-regenerate-as-well", "cert to regenerate as well", "/ca-to-rotate");
    generateSignedCertificate("/cert-not-to-regenerate", "cert not to regenerate", "/other-ca");
  }

  @After
  public void afterEach() {
    flyway.clean();
    flyway.setTarget(MigrationVersion.LATEST);
    flyway.migrate();

    encryptionKeyCanaryRepository.saveAll(canaries);
    encryptionKeyCanaryRepository.flush();
  }

  @Test
  public void regeneratingCertificatesSignedByCA_shouldRegenerateCertificates() throws Exception {
    MockHttpServletRequestBuilder regenerateCertificatesRequest = post(API_V1_BULK_REGENERATE_ENDPOINT)
        .header("Authorization", "Bearer " + USER_A_TOKEN)
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        //language=JSON
        .content("{\n"
            + "  \"signed_by\" : \"ca-to-rotate\"\n"
            + "}");

    String regenerateCertificatesResult = this.mockMvc.perform(regenerateCertificatesRequest)
        .andDo(print())
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    final JSONArray regeneratedCredentials = (new JSONObject(regenerateCertificatesResult)).getJSONArray("regenerated_credentials");
    final List<String> result = Arrays.asList(regeneratedCredentials.getString(0), regeneratedCredentials.getString(1));

    assertThat(regeneratedCredentials.length(), equalTo(2));
    assertThat(result, containsInAnyOrder("/cert-to-regenerate", "/cert-to-regenerate-as-well"));
  }

  @Test
  public void regeneratingCertificatesSignedByCA_shouldRegenerateCertificatesInAlphabeticalOrder() throws Exception {
    generateSignedCertificate("/z-cert-to-regenerate", "cert to regenerate", "/ca-to-rotate");
    generateSignedCertificate("/a-cert-to-regenerate", "cert to regenerate", "/ca-to-rotate");

    MockHttpServletRequestBuilder regenerateCertificatesRequest = post(API_V1_BULK_REGENERATE_ENDPOINT)
        .header("Authorization", "Bearer " + USER_A_TOKEN)
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        //language=JSON
        .content("{\n"
            + "  \"signed_by\" : \"/ca-to-rotate\"\n"
            + "}");

    String regenerateCertificatesResult = this.mockMvc.perform(regenerateCertificatesRequest)
        .andDo(print())
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    final JSONArray regeneratedCredentials = (new JSONObject(regenerateCertificatesResult)).getJSONArray("regenerated_credentials");
    final List<String> result = Arrays.asList(regeneratedCredentials.getString(0), regeneratedCredentials.getString(1), regeneratedCredentials.getString(2), regeneratedCredentials.getString(3));

    assertThat(regeneratedCredentials.length(), equalTo(4));
    assertThat(result.get(0), equalTo("/a-cert-to-regenerate"));
    assertThat(result.get(1), equalTo("/cert-to-regenerate"));
    assertThat(result.get(2), equalTo("/cert-to-regenerate-as-well"));
    assertThat(result.get(3), equalTo("/z-cert-to-regenerate"));
  }

  @Test
  public void regeneratingCertificatesSignedByCA_whenUserCannotReadCa_shouldFailAndNotRotateAnyCertificates() throws Exception {
    MockHttpServletRequestBuilder revokeCaReadAccess = delete(API_V1_PERMISSION_ENDPOINT + "?credential_name=/ca-to-rotate&actor=" + USER_A_ACTOR_ID)
        .header("Authorization", "Bearer " + ALL_PERMISSIONS_TOKEN)
        .accept(APPLICATION_JSON);

    mockMvc.perform(revokeCaReadAccess)
        .andExpect(status().isNoContent());

    assertThat(credentialVersionDataService.findAllByName("/cert-to-regenerate").size(), equalTo(2));
    assertThat(credentialVersionDataService.findAllByName("/cert-to-regenerate-as-well").size(), equalTo(1));

    MockHttpServletRequestBuilder regenerateCertificatesRequest = post(API_V1_BULK_REGENERATE_ENDPOINT)
        .header("Authorization", "Bearer " + USER_A_TOKEN)
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        //language=JSON
        .content("{\n"
            + "  \"signed_by\" : \"/ca-to-rotate\"\n"
            + "}");

    mockMvc.perform(regenerateCertificatesRequest)
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error", IsEqual.equalTo("The request could not be completed because the credential does not exist or you do not have sufficient authorization.")));

    assertThat(credentialVersionDataService.findAllByName("/cert-to-regenerate").size(), equalTo(2));
    assertThat(credentialVersionDataService.findAllByName("/cert-to-regenerate-as-well").size(), equalTo(1));
  }

  @Test
  public void regeneratingCertificatesSignedByCA_whenUserCannotWriteToOneOfTheCertificates_shouldFailAndNotRotateAnyCertificates() throws Exception {
    MockHttpServletRequestBuilder revokeWriteAccessRequest =
        delete(API_V1_PERMISSION_ENDPOINT + "?credential_name=/cert-to-regenerate&actor=" + USER_A_ACTOR_ID)
        .header("Authorization", "Bearer " + ALL_PERMISSIONS_TOKEN)
        .accept(APPLICATION_JSON);

    mockMvc.perform(revokeWriteAccessRequest)
        .andExpect(status().isNoContent());

    MockHttpServletRequestBuilder regenerateCertificatesRequest = post(API_V1_BULK_REGENERATE_ENDPOINT)
        .header("Authorization", "Bearer " + USER_A_TOKEN)
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        //language=JSON
        .content("{\n"
            + "  \"signed_by\" : \"/ca-to-rotate\"\n"
            + "}");

    mockMvc.perform(regenerateCertificatesRequest)
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error", IsEqual.equalTo("The request could not be completed because the credential does not exist or you do not have sufficient authorization.")));

    assertThat(credentialVersionDataService.findAllByName("/cert-to-regenerate").size(), equalTo(2));
    assertThat(credentialVersionDataService.findAllByName("/cert-to-regenerate-as-well").size(), equalTo(1));
  }

  @Test
  public void regeneratingByCA_PersistsAnAuditEntry_whenRegenerationFails() throws Exception {
    MockHttpServletRequestBuilder revokeWriteAccessRequest =
        delete(API_V1_PERMISSION_ENDPOINT + "?credential_name=/cert-to-regenerate&actor=" + USER_A_ACTOR_ID)
            .header("Authorization", "Bearer " + ALL_PERMISSIONS_TOKEN)
            .accept(APPLICATION_JSON);

    mockMvc.perform(revokeWriteAccessRequest)
        .andExpect(status().isNoContent());

    MockHttpServletRequestBuilder regenerateCertificatesRequest = post(API_V1_BULK_REGENERATE_ENDPOINT)
        .header("Authorization", "Bearer " + USER_A_TOKEN)
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        //language=JSON
        .content("{\n"
            + "  \"signed_by\" : \"/ca-to-rotate\"\n"
            + "}");

    mockMvc.perform(regenerateCertificatesRequest)
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error", IsEqual.equalTo("The request could not be completed because the credential does not exist or you do not have sufficient authorization.")));
  }

  @Test
  public void regeneratingCertificatesSignedByCA_whenUserCannotWriteToAllOfTheCertificates_shouldFailAndNotRotateAnyCertificates() throws Exception {
    MockHttpServletRequestBuilder revokeCaReadAccess = delete(API_V1_PERMISSION_ENDPOINT + "?credential_name=/cert-to-regenerate-as-well&actor=" + USER_A_ACTOR_ID)
      .header("Authorization", "Bearer " + ALL_PERMISSIONS_TOKEN)
        .accept(APPLICATION_JSON);

    mockMvc.perform(revokeCaReadAccess)
        .andExpect(status().isNoContent());

    MockHttpServletRequestBuilder regenerateCertificatesRequest = post(API_V1_BULK_REGENERATE_ENDPOINT)
        .header("Authorization", "Bearer " + USER_A_TOKEN)
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        //language=JSON
        .content("{\n"
            + "  \"signed_by\" : \"/ca-to-rotate\"\n"
            + "}");

    mockMvc.perform(regenerateCertificatesRequest)
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error", IsEqual.equalTo("The request could not be completed because the credential does not exist or you do not have sufficient authorization.")));

    assertThat(credentialVersionDataService.findAllByName("/cert-to-regenerate").size(), equalTo(2));
    assertThat(credentialVersionDataService.findAllByName("/cert-to-regenerate-as-well").size(), equalTo(1));
  }

  @Test
  public void regeneratingCertificatesSignedByCA_whenSignedByIsMissing_returns400() throws Exception {
    MockHttpServletRequestBuilder regenerateCertificatesRequest = post(API_V1_BULK_REGENERATE_ENDPOINT)
        .header("Authorization", "Bearer " + ALL_PERMISSIONS_TOKEN)
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        //language=JSON
        .content("{}");

    mockMvc.perform(regenerateCertificatesRequest)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error", IsEqual.equalTo("You must specify a signing CA. Please update and retry your request.")));
  }

  @Test
  public void regeneratingCertificatesSignedByCa_recursivelyRegeneratesLeafCertificatesInChain() throws Exception  {
    generateRootCA("/ca-cert", "cert");
    generateIntermediateCA("/intermediate-cert", "cert to regenerate", "/ca-cert");
    generateSignedCertificate("/leaf-cert", "cert to regenerate", "/intermediate-cert");

    MockHttpServletRequestBuilder regenerateCertificatesRequest = post(API_V1_BULK_REGENERATE_ENDPOINT)
        .header("Authorization", "Bearer " + ALL_PERMISSIONS_TOKEN)
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        //language=JSON
        .content("{\n"
            + "  \"signed_by\" : \"/ca-cert\"\n"
            + "}");

    String regenerateCertificatesResult = this.mockMvc.perform(regenerateCertificatesRequest)
        .andDo(print())
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    final JSONArray regeneratedCredentials = (new JSONObject(regenerateCertificatesResult)).getJSONArray("regenerated_credentials");

    assertThat(regeneratedCredentials.length(), equalTo(2));
    assertThat(regeneratedCredentials.getString(0), equalTo("/intermediate-cert"));
    assertThat(regeneratedCredentials.getString(1), equalTo("/leaf-cert"));

    verifyVersionCountForCertificate("/intermediate-cert", 2);
    verifyVersionCountForCertificate("/leaf-cert", 2);
  }


  @Test
  public void regeneratingCertificatesSignedByCa_willFailIfAnyChildCertificateIsNotWritable() throws Exception  {
    generateRootCA("/ca-cert", "cert");
    generateIntermediateCA("/intermediate-cert", "cert to regenerate", "/ca-cert");
    generateSignedCertificate("/leaf-cert", "cert to regenerate", "/intermediate-cert");

    MockHttpServletRequestBuilder revokeReadAccess = delete(API_V1_PERMISSION_ENDPOINT + "?credential_name=/leaf-cert&actor=" + USER_A_ACTOR_ID)
        .header("Authorization", "Bearer " + ALL_PERMISSIONS_TOKEN)
        .accept(APPLICATION_JSON);

    mockMvc.perform(revokeReadAccess)
        .andExpect(status().isNoContent());

    MockHttpServletRequestBuilder regenerateCertificatesRequest = post(API_V1_BULK_REGENERATE_ENDPOINT)
        .header("Authorization", "Bearer " +  USER_A_TOKEN)
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        //language=JSON
        .content("{\n"
            + "  \"signed_by\" : \"/ca-cert\"\n"
            + "}");

    this.mockMvc.perform(regenerateCertificatesRequest)
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error", IsEqual.equalTo("The request could not be completed because the credential does not exist or you do not have sufficient authorization.")));

    verifyVersionCountForCertificate("/intermediate-cert", 1);
    verifyVersionCountForCertificate("/leaf-cert", 1);
  }

  private void verifyVersionCountForCertificate(String certificateName, int expectedVersionCount) throws Exception {
    String certificateId = getCertificateId(mockMvc, certificateName);
    MockHttpServletRequestBuilder getVersionsRequest = get("/api/v1/certificates/" + certificateId + "/versions")
        .header("Authorization", "Bearer " +  ALL_PERMISSIONS_TOKEN)
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON);

    String versions = this.mockMvc.perform(getVersionsRequest)
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    assertThat(new JSONArray(versions).length(), equalTo(expectedVersionCount));
  }

  private String generateRootCA(String caName, String caCommonName) throws Exception {
    MockHttpServletRequestBuilder generateCAToRotateRequest = post(API_V1_DATA_ENDPOINT)
        .header("Authorization", "Bearer " + ALL_PERMISSIONS_TOKEN)
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        //language=JSON
        .content("{\n"
            + "  \"name\" : \"" + caName + "\",\n"
            + "  \"type\" : \"certificate\",\n"
            + "  \"parameters\" : {\n"
            + "     \"is_ca\": true,\n"
            + "     \"common_name\": \"" + caCommonName + "\"\n"
            + "   },\n"
            + "\"additional_permissions\": [{"
            + "   \"actor\": \"" + USER_A_ACTOR_ID + "\",\n"
            + "   \"operations\": [\"read\"]\n"
            + "}]}");

    return this.mockMvc.perform(generateCAToRotateRequest)
        .andDo(print())
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
  }

  private void generateSignedCertificate(String certificateName, String certificatCN, String signingCA) throws Exception {
    MockHttpServletRequestBuilder generateCertSignedByOriginalCARequest = post(API_V1_DATA_ENDPOINT)
        .header("Authorization", "Bearer " + ALL_PERMISSIONS_TOKEN)
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        //language=JSON
        .content("{\n"
            + "  \"name\" : \"" + certificateName + "\",\n"
            + "  \"type\" : \"certificate\",\n"
            + "  \"parameters\" : {\n"
            + "    \"ca\": \"" + signingCA + "\",\n"
            + "    \"common_name\": \"" + certificatCN + "\"\n"
            + "  },\n"
            + "  \"overwrite\": true,\n"
            + "  \"additional_permissions\": \n"
            + "    [\n"
            + "      {\n"
            + "        \"actor\": \"" + USER_A_ACTOR_ID + "\",\n"
            + "        \"operations\": [\"read\", \"write\"]\n"
            + "      }\n"
            + "    ]\n"
            + "}");

    String certGenerationResult = this.mockMvc.perform(generateCertSignedByOriginalCARequest)
        .andDo(print())
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    assertThat((new JSONObject(certGenerationResult)).getString("value"), notNullValue());
  }

  private String generateIntermediateCA(String certificateName, String certificatCN, String signingCA) throws Exception {
    MockHttpServletRequestBuilder generateCertSignedByOriginalCARequest = post(API_V1_DATA_ENDPOINT)
        .header("Authorization", "Bearer " + ALL_PERMISSIONS_TOKEN)
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        //language=JSON
        .content("{\n"
            + "  \"name\" : \"" + certificateName + "\",\n"
            + "  \"type\" : \"certificate\",\n"
            + "  \"parameters\" : {\n"
            + "  \"is_ca\": true,\n"
            + "    \"ca\": \"" + signingCA + "\",\n"
            + "    \"common_name\": \"" + certificatCN + "\"\n"
            + "  },\n"
            + "  \"overwrite\": true,\n"
            + "  \"additional_permissions\": \n"
            + "    [\n"
            + "      {\n"
            + "        \"actor\": \"" + USER_A_ACTOR_ID + "\",\n"
            + "        \"operations\": [\"write\", \"read\"]\n"
            + "      }\n"
            + "    ]\n"
            + "}");

    String certGenerationResult = this.mockMvc.perform(generateCertSignedByOriginalCARequest)
        .andDo(print())
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    assertThat((new JSONObject(certGenerationResult)).getString("value"), notNullValue());
    return certGenerationResult;
  }
}