/**
 * Copyright 2019
 *
 * @author gzhang
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.garyzhangscm.cwms.resources.service;

import com.garyzhangscm.cwms.resources.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.resources.model.Department;
import com.garyzhangscm.cwms.resources.model.EmailAlertConfiguration;
import com.garyzhangscm.cwms.resources.repository.DepartmentRepository;
import com.garyzhangscm.cwms.resources.repository.EmailAlertConfigurationRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

@Service
public class EmailAlertConfigurationService {
    private static final Logger logger = LoggerFactory.getLogger(EmailAlertConfigurationService.class);
    @Autowired
    private EmailAlertConfigurationRepository emailAlertConfigurationRepository;

    public EmailAlertConfiguration findById(Long id) {
        EmailAlertConfiguration emailAlertConfiguration =  emailAlertConfigurationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("Email alert configuration not found by id: " + id));
        return emailAlertConfiguration;
    }


    public List<EmailAlertConfiguration> findAll(Long companyId) {

        return emailAlertConfigurationRepository.findAll(
                (Root<EmailAlertConfiguration> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("companyId"), companyId));


                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );
    }


    public EmailAlertConfiguration save(EmailAlertConfiguration emailAlertConfiguration) {



        EmailAlertConfiguration newEmailAlertConfiguration =
                emailAlertConfigurationRepository.save(emailAlertConfiguration);

        // everytime we change the email configuration, we will change the
        // javaMailSender bean to read the new configuration
        reloadJavaMailSender(newEmailAlertConfiguration);
        return newEmailAlertConfiguration;
    }

    private JavaMailSender reloadJavaMailSender(EmailAlertConfiguration newEmailAlertConfiguration) {

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(newEmailAlertConfiguration.getHost());
        mailSender.setPort(newEmailAlertConfiguration.getPort());

        mailSender.setUsername(newEmailAlertConfiguration.getUsername());
        mailSender.setPassword(newEmailAlertConfiguration.getPassword());

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", newEmailAlertConfiguration.getTransportProtocol());
        props.put("mail.smtp.auth", newEmailAlertConfiguration.getAuthFlag());
        props.put("mail.smtp.starttls.enable", newEmailAlertConfiguration.getStarttlsEnableFlag());
        props.put("mail.debug", newEmailAlertConfiguration.getDebugFlag());

        return mailSender;
    }

    public JavaMailSender reloadJavaMailSender(Long companyId) {
        EmailAlertConfiguration emailAlertConfiguration = findByCompany(companyId);
        if (Objects.nonNull(emailAlertConfiguration)) {
            return reloadJavaMailSender(emailAlertConfiguration);
        }
        return null;
    }

    public EmailAlertConfiguration findByCompany(Long companyId) {

        return emailAlertConfigurationRepository.findByCompanyId(companyId);
    }

    public EmailAlertConfiguration saveOrUpdate(EmailAlertConfiguration emailAlertConfiguration) {
        if (Objects.isNull(emailAlertConfiguration.getId()) &&
                !Objects.isNull(findByCompany(emailAlertConfiguration.getCompanyId()))) {
            emailAlertConfiguration.setId(findByCompany(emailAlertConfiguration.getCompanyId()).getId());
        }
        return save(emailAlertConfiguration);
    }


    public EmailAlertConfiguration addEmailAlertConfiguration(EmailAlertConfiguration emailAlertConfiguration) {
        return saveOrUpdate(emailAlertConfiguration);

    }

    public EmailAlertConfiguration changeEmailAlertConfiguration(Long id, EmailAlertConfiguration emailAlertConfiguration) {
        emailAlertConfiguration.setId(id);
        return saveOrUpdate(emailAlertConfiguration);

    }

    public void delete(Long id) {
        emailAlertConfigurationRepository.deleteById(id);

    }
}
