package com.lou.freegpt.utils;

import com.lou.freegpt.domain.MailMessages;
import org.springframework.beans.factory.annotation.Autowired;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Properties;

public class QQMailSender {
    private MailMessages mailMessages;

    public QQMailSender(MailMessages mailMessages){
        this.mailMessages = mailMessages;
    }

    public static void main(String[] args) {

        // 收件人邮箱地址
        String toEmail = "3803217870@qq.com";

        // 发件人邮箱地址
        String fromEmail = "415240147@qq.com";

        // 发件人邮箱密码或授权码
        String password = "kiexyairjfkvbgii";

        String fromName = "木火科技";

        // 设置邮件属性
        Properties properties = new Properties();
        properties.put("mail.smtp.host", "smtp.qq.com");
        properties.put("mail.smtp.port", "587");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");

        // 创建Session对象
        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromEmail, password);
            }
        });

        try {
            // 创建MimeMessage对象
            MimeMessage message = new MimeMessage(session);

            message.setFrom();

            // 设置发件人
            message.setFrom(new InternetAddress(fromEmail,fromName));

            // 设置收件人
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));

            // 设置主题
            message.setSubject("【木火科技】验证码通知");

            // 设置邮件内容
            message.setText("Hello, this is a test email from Java.");

            // 发送邮件
            Transport.send(message);

            System.out.println("Email sent successfully.");

        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
