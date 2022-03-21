package com.garyzhangscm.cwms.resources.service;

import com.garyzhangscm.cwms.resources.exception.EmailException;
import com.garyzhangscm.cwms.resources.model.EmailAlertConfiguration;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


@Service
public class EMailService {

    private static final Logger logger = LoggerFactory.getLogger(DepartmentService.class);


    @Autowired
    private EmailAlertConfigurationService emailAlertConfigurationService;

    /**
     * 发送纯文本邮件.
     *
     * @param companyId    Company Id
     * @param to      目标email 地址
     * @param subject 邮件主题
     * @param text    纯文本内容
     */
    public void sendMail(Long companyId,   String to, String subject, String text) {
        // load the java mail configuration
        EmailAlertConfiguration emailAlertConfiguration = emailAlertConfigurationService.findByCompany(companyId);
        if (Objects.isNull(emailAlertConfiguration)) {
            throw EmailException.raiseException("email confirmation for the company " + companyId + " is not setup");
        }
        JavaMailSender javaMailSender = emailAlertConfigurationService.getJavaMailSender(companyId);
        if (Objects.isNull(javaMailSender)) {
            throw EmailException.raiseException("email server for the company " + companyId + "  is not setup");
        }
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(emailAlertConfiguration.getSendFromEmail());
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        logger.debug("start to sent email to {}, from {}, \n subject: {}, text: {}",
                message.getTo(), message.getFrom(), message.getSubject(), message.getText());
        javaMailSender.send(message);
    }


}
