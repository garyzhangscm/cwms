package com.garyzhangscm.cwms.resources.model;

/** Passwords are request-only and deliberately excluded from toString. */
public class PasswordResetRequest {
    private String newPassword;
    private boolean changePasswordAtNextLogon = true;
    public String getNewPassword(){return newPassword;}
    public void setNewPassword(String value){newPassword=value;}
    public boolean isChangePasswordAtNextLogon(){return changePasswordAtNextLogon;}
    public void setChangePasswordAtNextLogon(boolean value){changePasswordAtNextLogon=value;}
}
