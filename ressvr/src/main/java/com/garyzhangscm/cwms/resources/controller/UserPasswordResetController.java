package com.garyzhangscm.cwms.resources.controller;

import com.garyzhangscm.cwms.resources.ResponseBodyWrapper;
import com.garyzhangscm.cwms.resources.model.PasswordResetRequest;
import com.garyzhangscm.cwms.resources.service.UserPasswordResetService;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserPasswordResetController {
    private final UserPasswordResetService service;
    public UserPasswordResetController(UserPasswordResetService service){this.service=service;}
    @PostMapping("/users/{id}/password-reset")
    public ResponseBodyWrapper<String> reset(@PathVariable Long id,@RequestParam Long companyId,@RequestBody PasswordResetRequest request){
        service.reset(companyId,id,request);
        return ResponseBodyWrapper.success("Password reset successfully.");
    }
}
