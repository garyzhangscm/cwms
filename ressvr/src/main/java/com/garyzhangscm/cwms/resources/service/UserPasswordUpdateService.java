package com.garyzhangscm.cwms.resources.service;

import com.garyzhangscm.cwms.resources.clients.AuthServiceRestemplateClient;
import com.garyzhangscm.cwms.resources.exception.UserOperationException;
import com.garyzhangscm.cwms.resources.model.User;
import com.garyzhangscm.cwms.resources.model.UserAuth;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Password updates must never merge a User or replace its role collection. */
@Service
public class UserPasswordUpdateService {
    private final AuthServiceRestemplateClient auth;
    @PersistenceContext private EntityManager entities;

    public UserPasswordUpdateService(AuthServiceRestemplateClient auth) { this.auth = auth; }

    @Transactional
    public void update(User target, String password, boolean forceChange) {
        // The authorized target was loaded from the database, including its auth flags.
        UserAuth credentials = target.getUserAuth();
        credentials.setPassword(password);
        auth.changeUserAuth(credentials);
        // Update only one scalar column; do not read, clear or re-save user_role.
        int updated = entities.createQuery(
                "update User u set u.changePasswordAtNextLogon = :forceChange " +
                "where u.id = :userId and u.companyId = :companyId")
                .setParameter("forceChange", forceChange)
                .setParameter("userId", target.getId())
                .setParameter("companyId", target.getCompanyId())
                .executeUpdate();
        if (updated != 1) throw UserOperationException.raiseException("The password-change setting could not be saved. Please refresh the user record.");
    }
}
