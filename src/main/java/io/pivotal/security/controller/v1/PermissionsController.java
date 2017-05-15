package io.pivotal.security.controller.v1;

import io.pivotal.security.audit.EventAuditLogService;
import io.pivotal.security.audit.RequestUuid;
import io.pivotal.security.auth.UserContext;
import io.pivotal.security.data.AccessControlDataService;
import io.pivotal.security.handler.AccessControlHandler;
import io.pivotal.security.request.AccessControlOperation;
import io.pivotal.security.request.AccessEntriesRequest;
import io.pivotal.security.view.PermissionsView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static io.pivotal.security.audit.AuditingOperationCode.ACL_ACCESS;
import static io.pivotal.security.audit.AuditingOperationCode.ACL_DELETE;
import static io.pivotal.security.audit.AuditingOperationCode.ACL_UPDATE;
import static io.pivotal.security.audit.EventAuditRecordParametersFactory.createPermissionEventAuditRecordParameters;
import static io.pivotal.security.audit.EventAuditRecordParametersFactory.createPermissionsEventAuditParameters;

@RestController
@RequestMapping(path = "/api/v1/permissions", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class PermissionsController {
  private final AccessControlHandler accessControlHandler;
  private final EventAuditLogService eventAuditLogService;
  private AccessControlDataService accessControlDataService;

  @Autowired
  public PermissionsController(
      AccessControlHandler accessControlHandler,
      EventAuditLogService eventAuditLogService,
      AccessControlDataService accessControlDataService
  ) {
    this.accessControlHandler = accessControlHandler;
    this.eventAuditLogService = eventAuditLogService;
    this.accessControlDataService = accessControlDataService;
  }

  @GetMapping
  @ResponseStatus(HttpStatus.OK)
  public PermissionsView getAccessControlList(
    @RequestParam("credential_name") String credentialName,
    RequestUuid requestUuid,
    UserContext userContext
  ) throws Exception {
    return eventAuditLogService.auditEvent(requestUuid, userContext, eventAuditRecordParameters -> {
      eventAuditRecordParameters.setCredentialName(credentialName);
      eventAuditRecordParameters.setAuditingOperationCode(ACL_ACCESS);

      final PermissionsView response = accessControlHandler.getAccessControlListResponse(userContext, credentialName);
      eventAuditRecordParameters.setCredentialName(response.getCredentialName());

      return response;
    });
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public PermissionsView setAccessControlEntries(
      RequestUuid requestUuid,
      UserContext userContext,
      @Validated @RequestBody AccessEntriesRequest accessEntriesRequest
  ) {
    return eventAuditLogService.auditEvents(requestUuid, userContext, parametersList -> {
      parametersList.addAll(createPermissionsEventAuditParameters(
          ACL_UPDATE,
          accessEntriesRequest.getCredentialName(),
          accessEntriesRequest.getPermissions())
      );
      return accessControlHandler.setAccessControlEntries(
          userContext,
          accessEntriesRequest.getCredentialName(),
          accessEntriesRequest.getPermissions()
      );
    });
  }

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteAccessControlEntry(
      @RequestParam("credential_name") String credentialName,
      @RequestParam("actor") String actor,
      RequestUuid requestUuid,
      UserContext userContext

  ) {
    eventAuditLogService.auditEvents(requestUuid, userContext, parameterList -> {
      List<AccessControlOperation> operationList = accessControlDataService.getAllowedOperations(credentialName, actor);

      parameterList.addAll(createPermissionEventAuditRecordParameters(
          ACL_DELETE,
          credentialName,
          actor,
          operationList
      ));

      accessControlHandler.deleteAccessControlEntry(userContext, credentialName, actor);

      return true;
    });
  }
}
