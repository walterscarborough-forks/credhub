package org.cloudfoundry.credhub.integration;

import org.cloudfoundry.credhub.CredentialManagerApp;
import org.cloudfoundry.credhub.constants.CredentialWriteMode;
import org.cloudfoundry.credhub.helper.AuditingHelper;
import org.cloudfoundry.credhub.repository.EventAuditRecordRepository;
import org.cloudfoundry.credhub.repository.RequestAuditRecordRepository;
import org.cloudfoundry.credhub.util.DatabaseProfileResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.cloudfoundry.credhub.audit.AuditingOperationCode.CREDENTIAL_FIND;
import static org.cloudfoundry.credhub.helper.RequestHelper.generateCa;
import static org.cloudfoundry.credhub.helper.RequestHelper.generatePassword;
import static org.cloudfoundry.credhub.util.AuthConstants.*;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@ActiveProfiles(value = "unit-test", resolver = DatabaseProfileResolver.class)
@SpringBootTest(classes = CredentialManagerApp.class)
@TestPropertySource(properties = "security.authorization.acls.enabled=true")
@Transactional
public class CredentialFindTest {

  @Autowired
  private WebApplicationContext webApplicationContext;

  @Autowired
  private RequestAuditRecordRepository requestAuditRecordRepository;

  @Autowired
  private EventAuditRecordRepository eventAuditRecordRepository;

  private AuditingHelper auditingHelper;

  private MockMvc mockMvc;

  private final String credentialName = "/my-namespace/subTree/credential-name";

  @Before
  public void beforeEach() {
    mockMvc = MockMvcBuilders
      .webAppContextSetup(webApplicationContext)
      .apply(springSecurity())
      .build();

    auditingHelper = new AuditingHelper(requestAuditRecordRepository, eventAuditRecordRepository);
  }

  @Test
  public void findCredentials_byNameLike_whenSearchTermContainsNoSlash_returnsCredentialMetadata() throws Exception {
    generatePassword(mockMvc, credentialName, CredentialWriteMode.OVERWRITE.mode, 20);

    String substring = credentialName.substring(4).toUpperCase();
    ResultActions response = findCredentialsByNameLike(substring, UAA_OAUTH2_PASSWORD_GRANT_TOKEN);

    response.andExpect(status().isOk())
      .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
      .andExpect(jsonPath("$.credentials[0].name").value(credentialName));
  }

  @Test
  public void findCredentials_byNameLike_whenSearchTermContainsNoSlash_persistsAnAuditEntry() throws Exception {
    findCredentialsByNameLike(credentialName, UAA_OAUTH2_PASSWORD_GRANT_TOKEN);

    auditingHelper.verifyAuditing(CREDENTIAL_FIND, null, UAA_OAUTH2_PASSWORD_GRANT_ACTOR_ID, "/api/v1/data", 200);
  }

  public void findCredentials_byNameLike_whenUserDoesNotHavePermission_doesNotReturnCredential() throws Exception {
    generateCa(mockMvc, "my-certificate", UAA_OAUTH2_PASSWORD_GRANT_TOKEN);

    ResultActions response = findCredentialsByNameLike("/", UAA_OAUTH2_CLIENT_CREDENTIALS_TOKEN);

    response.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
      .andExpect(jsonPath("$.credentials", hasSize(0)));
  }

  @Test
  public void findCredentials_byPath_whenUserDoesNotHavePermission_doesNotReturnCredential() throws Exception {
    generateCa(mockMvc, "my-certificate", UAA_OAUTH2_PASSWORD_GRANT_TOKEN);

    ResultActions response = findCredentialsByPath("/", UAA_OAUTH2_CLIENT_CREDENTIALS_TOKEN);

    response.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
      .andExpect(jsonPath("$.credentials", hasSize(0)));
  }

  @Test
  public void findCredentials_byPath_returnsCredentialMetaData() throws Exception {
    String substring = credentialName.substring(0, credentialName.lastIndexOf("/"));
    generatePassword(mockMvc, credentialName, CredentialWriteMode.OVERWRITE.mode, 20);

    ResultActions response = findCredentialsByPath(substring, UAA_OAUTH2_PASSWORD_GRANT_TOKEN);

    response.andExpect(status().isOk())
      .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
      .andExpect(jsonPath("$.credentials[0].name").value(credentialName));
  }

  @Test
  public void findCredentials_byPath_shouldOnlyFindPathsThatBeginWithSpecifiedSubstringCaseInsensitively() throws Exception {
    final String path = "namespace";

    assertTrue(credentialName.contains(path));

    ResultActions response = findCredentialsByPath(path.toUpperCase(), UAA_OAUTH2_PASSWORD_GRANT_TOKEN);

    response.andExpect(status().isOk())
      .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
      .andExpect(jsonPath("$.credentials", hasSize(0)));
  }

  @Test
  public void findCredentials_byPath_shouldReturnAllChildrenPrefixedWithThePathCaseInsensitively() throws Exception {
    final String path = "/my-namespace";

    generatePassword(mockMvc, credentialName, CredentialWriteMode.OVERWRITE.mode, 20);

    assertTrue(credentialName.startsWith(path));

    ResultActions response = findCredentialsByPath(path.toUpperCase(), UAA_OAUTH2_PASSWORD_GRANT_TOKEN);

    response.andExpect(status().isOk())
      .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
      .andExpect(jsonPath("$.credentials", hasSize(1)));
  }

  @Test
  public void findCredentials_byPath_shouldNotReturnCredentialsThatMatchThePathIncompletely() throws Exception {
    final String path = "/my-namespace/subTr";

    assertTrue(credentialName.startsWith(path));

    ResultActions response = findCredentialsByPath(path, UAA_OAUTH2_PASSWORD_GRANT_TOKEN);

    response.andExpect(status().isOk())
      .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
      .andExpect(jsonPath("$.credentials", hasSize(0)));
  }

  @Test
  public void findCredentials_byPath_savesTheAuditLog() throws Exception {
    String substring = credentialName.substring(0, credentialName.lastIndexOf("/"));

    generatePassword(mockMvc, credentialName, CredentialWriteMode.OVERWRITE.mode, 20);

    final MockHttpServletRequestBuilder request = get("/api/v1/data?path=" + substring)
      .header("Authorization", "Bearer " + UAA_OAUTH2_PASSWORD_GRANT_TOKEN)
      .accept(APPLICATION_JSON);

    mockMvc.perform(request);

    auditingHelper.verifyAuditing(CREDENTIAL_FIND, null, UAA_OAUTH2_PASSWORD_GRANT_ACTOR_ID, "/api/v1/data", 200);
  }

  @Test
  public void findCredentials_findingAllPaths_returnsAllPossibleCredentialsPaths() throws Exception {
    generatePassword(mockMvc, "my-namespace/subTree/credential", CredentialWriteMode.OVERWRITE.mode, 20);

    ResultActions response = findAllPaths(UAA_OAUTH2_PASSWORD_GRANT_TOKEN);

    response.andExpect(status().isOk())
      .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
      .andExpect(jsonPath("$.paths[0].path").value("/"))
      .andExpect(jsonPath("$.paths[1].path").value("/my-namespace/"))
      .andExpect(jsonPath("$.paths[2].path").value("/my-namespace/subTree/"));
  }

  @Test
  public void findCredentials_findingAllPaths_returnsNoCredentialsIfUserHasNoPermissions() throws Exception {
    generatePassword(mockMvc, "my-namespace/subTree/credential", CredentialWriteMode.OVERWRITE.mode, 20);

    ResultActions response = findAllPaths(UAA_OAUTH2_CLIENT_CREDENTIALS_TOKEN);

    response.andExpect(status().isOk())
      .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
      .andExpect(jsonPath("$.paths", hasSize(0)));
  }

  private ResultActions findCredentialsByNameLike(String pattern, String permissionsToken) throws Exception {
    final MockHttpServletRequestBuilder get = get("/api/v1/data?name-like=" + pattern)
      .header("Authorization", "Bearer " + permissionsToken)
      .accept(APPLICATION_JSON)
      .contentType(APPLICATION_JSON);

    return mockMvc.perform(get);
  }

  private ResultActions findCredentialsByPath(String path, String permissionsToken) throws Exception {
    final MockHttpServletRequestBuilder get = get("/api/v1/data?path=" + path)
      .header("Authorization", "Bearer " + permissionsToken)
      .accept(APPLICATION_JSON)
      .contentType(APPLICATION_JSON);

    return mockMvc.perform(get);
  }

  private ResultActions findAllPaths(String permissionsToken) throws Exception {
    final MockHttpServletRequestBuilder get = get("/api/v1/data?paths=true")
      .header("Authorization", "Bearer " + permissionsToken)
      .accept(APPLICATION_JSON)
      .contentType(APPLICATION_JSON);

    return mockMvc.perform(get);
  }

}
