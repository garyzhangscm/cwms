package com.garyzhangscm.cwms.resources.service;

import com.garyzhangscm.cwms.resources.model.*;
import com.garyzhangscm.cwms.resources.exception.UserOperationException;
import org.springframework.stereotype.Service;
import java.util.Objects;
import java.util.Base64;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.servlet.http.HttpServletRequest;

@Service
public class UserPasswordResetService {
    private final UserService users;
    private final HttpServletRequest httpRequest;
    public UserPasswordResetService(UserService users,HttpServletRequest httpRequest){this.users=users;this.httpRequest=httpRequest;}
    static boolean hasPasswordResetAccess(User actor) {
        if (Boolean.TRUE.equals(actor.getAdmin())) return true;
        return actor.getRoles() != null && actor.getRoles().stream().anyMatch(role ->
                role != null && Boolean.TRUE.equals(role.getEnabled())
                && role.getName() != null && "Admin".equalsIgnoreCase(role.getName().trim())
                && Objects.equals(role.getCompanyId(), actor.getCompanyId()));
    }
    public void reset(Long companyId,Long userId,PasswordResetRequest request){
        // The gateway verifies JWT signatures. Never authorize with a client-supplied username header.
        String username;
        try {
            String authorization=httpRequest.getHeader("Authorization");
            if(authorization==null || !authorization.startsWith("Bearer ")) throw new IllegalArgumentException();
            var claims=new ObjectMapper().readTree(Base64.getUrlDecoder().decode(authorization.substring(7).split("\\.")[1]));
            username=claims.path("sub").asText("");
            long tokenCompany=claims.path("companyId").asLong(Long.MIN_VALUE);
            if(username.isBlank() || companyId==null || companyId<0 || (tokenCompany!=companyId && tokenCompany!=-1)
                    || claims.path("exp").asLong(0)<=System.currentTimeMillis()/1000) throw new IllegalArgumentException();
        } catch(Exception error){throw UserOperationException.raiseException("Please sign in with a valid account.");}
        User actor=users.findByUsername(companyId,username);
        if(actor==null || !hasPasswordResetAccess(actor)
                || (!Objects.equals(actor.getCompanyId(),companyId) && !Boolean.TRUE.equals(actor.getSystemAdmin())))
            throw UserOperationException.raiseException("Only an administrator can reset user passwords.");
        User target=users.findById(userId);
        if(!Objects.equals(target.getCompanyId(),companyId) || target.getCompanyId()<0 || Boolean.TRUE.equals(target.getSystemAdmin()))
            throw UserOperationException.raiseException("This user cannot be reset from the selected company.");
        String password=request.getNewPassword();
        if(password==null || password.isBlank() || password.length()<8 || password.length()>128 || password.startsWith("{"))
            throw UserOperationException.raiseException("The password must contain 8–128 characters and cannot start with {.");
        target.setPassword(password);
        target.setChangePasswordAtNextLogon(request.isChangePasswordAtNextLogon());
        users.changeUser(target);
    }
}
