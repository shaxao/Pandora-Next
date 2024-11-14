package com.lou.freegpt.domain;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.mail.Session;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.Properties;
import java.util.UUID;

@Slf4j
@Component
public class MailMessages {
     private Properties properties;
     private javax.mail.Session session;
     private MimeMessage mimeMessage;
     @Value("${mail.user}") String user;
     @Value("${mail.smtp.fromName}") String fromName;

     public MailMessages(@Value("${mail.smtp.host}") String host,
                         @Value("${mail.smtp.port}") String port,
                         @Value("${mail.user}") String user,
                         @Value("${mail.smtp.auth}") String auth,
                         @Value("${mail.smtp.ssl.enable}") String enable,
                         @Value("${mail.smtp.from}") String mailfrom,
                         @Value("${mail.password}") String password){
          createProperties(host,port,auth,enable,mailfrom);
          createSession(user,password);
          final String messageIDValue = genMessageID(user);
          mimeMessage = new MimeMessage(session){
               @Override
               protected void updateMessageID() throws MessagingException {
                    //设置自定义Message-ID值
                    setHeader("Message-ID", messageIDValue);//创建Message-ID
               }
          };
     }


     private void createProperties(String host,String port,String auth,String enable,String mailfrom){
          properties = new Properties();
          properties.put("mail.smtp.host",host);
          properties.put("mail.smtp.port",port);
          properties.put("mail.smtp.auth",auth);
          properties.put("mail.smtp.from",mailfrom);
          properties.setProperty("mail.smtp.ssl.enable",enable);
     }

     private javax.mail.Session createSession(String fromEmail, String password){
        session = Session.getInstance(properties, new Authenticator() {
               @Override
               protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(fromEmail, password);
               }
          });
        return session;
     }

     public void createMessages(String code,String toEmail){
          try {
               mimeMessage.setFrom();
               mimeMessage.setFrom(new InternetAddress(user,fromName));
               mimeMessage.setSentDate(new Date()); //设置时间
               mimeMessage.addRecipients(Message.RecipientType.TO,toEmail);
               //设置主题
               mimeMessage.setSubject("【木火科技】验证码通知");
               //设置邮件内容
               mimeMessage.setText("安全验证码"+code+"，有效期十分钟!");
               System.out.println("验证码邮件已经准备好:" + code);
          }catch (Exception e){
               e.printStackTrace();
          }
     }

     public void createMessagesBySelf(String toEmail,String subject,String text){
          try {
               mimeMessage.setFrom();
               mimeMessage.setFrom(new InternetAddress(user,fromName));
               mimeMessage.addRecipients(Message.RecipientType.TO,toEmail);
               //设置主题
               mimeMessage.setSubject(subject);
               //设置邮件内容
               mimeMessage.setText(text);
          }catch (Exception e){
               e.printStackTrace();
          }
     }

     protected static String genMessageID(String mailFrom) {
          // message-id 必须符合 first-part@last-part
          String[] mailInfo = mailFrom.split("@");
          String domain = mailFrom;
          int index = mailInfo.length - 1;
          if (index >= 0) {
               domain = mailInfo[index];
          }
          UUID uuid = UUID.randomUUID();
          StringBuffer messageId = new StringBuffer();
          messageId.append('<').append(uuid.toString()).append('@').append(domain).append('>');
          return messageId.toString();
     }


     //发送验证码
     public boolean send(){
          try {
               Transport.send(mimeMessage);
               log.info("验证码发送成功");
               return true;
          } catch (MessagingException e) {
               log.info("验证码发送失败");
               e.printStackTrace();
               return false;
          }
     }
}
