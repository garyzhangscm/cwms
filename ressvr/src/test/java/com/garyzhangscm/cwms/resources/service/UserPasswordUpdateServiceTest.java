package com.garyzhangscm.cwms.resources.service;

import com.garyzhangscm.cwms.resources.clients.AuthServiceRestemplateClient;
import com.garyzhangscm.cwms.resources.exception.UserOperationException;
import com.garyzhangscm.cwms.resources.model.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserPasswordUpdateServiceTest {
    UserPasswordUpdateService service;
    AuthServiceRestemplateClient auth;
    EntityManager entities;
    Query query;
    User target;
    List<Role> roles;
    @BeforeEach @SuppressWarnings("unchecked") void setup() {
        auth=mock(AuthServiceRestemplateClient.class);
        entities=mock(EntityManager.class);query=mock(Query.class);
        service=new UserPasswordUpdateService(auth);
        ReflectionTestUtils.setField(service,"entities",entities);
        when(entities.createQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(),any())).thenReturn(query);
        when(query.executeUpdate()).thenReturn(1);
        target=new User();target.setId(10L);target.setCompanyId(1L);target.setUsername("test-user");
        target.setEmail("test@example.invalid");target.setEnabled(true);target.setLocked(false);
        roles=mock(List.class);
        when(roles.iterator()).thenThrow(new IllegalStateException("Detached collection must not be traversed"));
        target.setRoles(roles);
        clearInvocations(roles);
    }
    @Test void passwordResetNeverTouchesDetachedRolesAndWritesOnlyForceFlag() {
        service.update(target,"test-only-password",true);
        assertSame(roles,target.getRoles());verifyNoInteractions(roles);
        assertNull(target.getPassword());assertFalse(target.getChangePasswordAtNextLogon());
        ArgumentCaptor<UserAuth> payload=ArgumentCaptor.forClass(UserAuth.class);
        verify(auth).changeUserAuth(payload.capture());
        assertEquals("test-only-password",payload.getValue().getPassword());
        assertEquals("test-user",payload.getValue().getUsername());assertEquals(1L,payload.getValue().getCompanyId());
        assertTrue(payload.getValue().isEnabled());assertFalse(payload.getValue().isLocked());
        assertEquals("test@example.invalid",payload.getValue().getEmail());
        verify(entities).createQuery("update User u set u.changePasswordAtNextLogon = :forceChange where u.id = :userId and u.companyId = :companyId");
        verify(query).setParameter("forceChange",true);verify(query).setParameter("userId",10L);verify(query).setParameter("companyId",1L);
        verify(entities,never()).merge(any());verify(entities,never()).persist(any());
    }
    @Test void forceChangeCanBeDisabledWithoutChangingOtherAccountFlags() {
        target.setEnabled(false);target.setLocked(true);service.update(target,"test-only-password",false);
        ArgumentCaptor<UserAuth> payload=ArgumentCaptor.forClass(UserAuth.class);verify(auth).changeUserAuth(payload.capture());
        assertFalse(payload.getValue().isEnabled());assertTrue(payload.getValue().isLocked());
        verify(query).setParameter("forceChange",false);verifyNoInteractions(roles);
    }
    @Test void authFailureDoesNotWriteResourceDataOrChangeRoles() {
        when(auth.changeUserAuth(any())).thenThrow(new IllegalStateException("Auth unavailable"));
        assertThrows(IllegalStateException.class,()->service.update(target,"test-only-password",true));
        verifyNoInteractions(entities,roles);assertSame(roles,target.getRoles());
        assertNull(target.getPassword());assertFalse(target.getChangePasswordAtNextLogon());
    }
    @Test void missingResourceRecordReportsFailureWithoutTouchingRoles() {
        when(query.executeUpdate()).thenReturn(0);
        assertThrows(UserOperationException.class,()->service.update(target,"test-only-password",true));
        verifyNoInteractions(roles);verify(entities,never()).merge(any());
    }
    @Test void resourceWriteFailureStillPreservesRoleCollection() {
        when(query.executeUpdate()).thenThrow(new IllegalStateException("Database unavailable"));
        assertThrows(IllegalStateException.class,()->service.update(target,"test-only-password",true));
        assertSame(roles,target.getRoles());verifyNoInteractions(roles);verify(entities,never()).merge(any());
    }
}
