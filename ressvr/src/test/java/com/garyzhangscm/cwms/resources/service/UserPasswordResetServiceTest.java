package com.garyzhangscm.cwms.resources.service;
import com.garyzhangscm.cwms.resources.model.*;
import com.garyzhangscm.cwms.resources.exception.UserOperationException;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class UserPasswordResetServiceTest {
    javax.servlet.http.HttpServletRequest httpRequest;UserService users;UserPasswordResetService service;User actor,target;PasswordResetRequest request;
    @BeforeEach void setup(){
        users=mock(UserService.class);httpRequest=mock(javax.servlet.http.HttpServletRequest.class);service=new UserPasswordResetService(users,httpRequest);
        String claims=java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("{\"sub\":\"admin\",\"companyId\":1,\"exp\":9999999999}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer header."+claims+".signature");
        actor=new User();actor.setCompanyId(1L);actor.setAdmin(true);
        target=new User();target.setId(10L);target.setCompanyId(1L);target.setUsername("test-user");
        request=new PasswordResetRequest();request.setNewPassword("test-only-password");
        when(users.findByUsername(1L,"admin")).thenReturn(actor);when(users.findById(10L)).thenReturn(target);
    }
    @Test void nonAdminOrUnknownActorCannotWrite(){
        actor.setAdmin(false);assertThrows(UserOperationException.class,()->service.reset(1L,10L,request));
        when(users.findByUsername(1L,"admin")).thenReturn(null);assertThrows(UserOperationException.class,()->service.reset(1L,10L,request));
        verify(users,never()).changeUser(any());
    }
    @Test void crossCompanyAndSystemAccountsCannotWrite(){
        target.setCompanyId(2L);assertThrows(UserOperationException.class,()->service.reset(1L,10L,request));
        target.setCompanyId(1L);target.setSystemAdmin(true);assertThrows(UserOperationException.class,()->service.reset(1L,10L,request));
        verify(users,never()).changeUser(any());
    }
    @Test void invalidAndEncodedPasswordsCannotWrite(){
        for(String password:new String[]{"", "short", "        ","{bcrypt}encoded", "x".repeat(129)}){
            request.setNewPassword(password);assertThrows(UserOperationException.class,()->service.reset(1L,10L,request));
        }
        verify(users,never()).changeUser(any());
    }
    @Test void adminResetsThroughExistingEncodingFlowWithForceFlag(){
        service.reset(1L,10L,request);verify(users).changeUser(target);
        assertEquals("test-only-password",target.getPassword());assertEquals(true,target.getChangePasswordAtNextLogon());
    }
    @Test void enabledCompanyAdminRoleCanResetWithoutAccountAdminFlag(){
        actor.setAdmin(false);Role role=new Role();role.setName("Admin");role.setEnabled(true);role.setCompanyId(1L);
        actor.setRoles(java.util.List.of(role));service.reset(1L,10L,request);verify(users).changeUser(target);
    }
    @Test void disabledForeignAndOrdinaryRolesCannotReset(){
        actor.setAdmin(false);Role role=new Role();role.setName("Admin");role.setEnabled(false);role.setCompanyId(1L);
        actor.setRoles(java.util.List.of(role));assertThrows(UserOperationException.class,()->service.reset(1L,10L,request));
        role.setEnabled(true);role.setCompanyId(2L);assertThrows(UserOperationException.class,()->service.reset(1L,10L,request));
        role.setCompanyId(1L);role.setName("WarehouseManager");assertThrows(UserOperationException.class,()->service.reset(1L,10L,request));
        verify(users,never()).changeUser(any());
    }
    @Test void roleAdminStillCannotResetCrossCompanyOrProtectedTargets(){
        actor.setAdmin(false);Role role=new Role();role.setName("ADMIN");role.setEnabled(true);role.setCompanyId(1L);
        actor.setRoles(java.util.List.of(role));target.setCompanyId(2L);
        assertThrows(UserOperationException.class,()->service.reset(1L,10L,request));
        target.setCompanyId(1L);target.setSystemAdmin(true);assertThrows(UserOperationException.class,()->service.reset(1L,10L,request));
        verify(users,never()).changeUser(any());
    }
    @Test void optionalForceFlagCanBeDisabled(){
        request.setChangePasswordAtNextLogon(false);service.reset(1L,10L,request);
        assertEquals(false,target.getChangePasswordAtNextLogon());
    }
}
