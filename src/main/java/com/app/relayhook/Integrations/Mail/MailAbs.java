package com.app.relayhook.Integrations.Mail;

import java.util.List;
import java.util.Map;

public interface MailAbs {


    abstract Object sendTemplateMail(String to[], String subject, String templateName,Map<String,Object> payload);
}
