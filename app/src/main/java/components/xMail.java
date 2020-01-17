package components;

import android.app.Activity;
import android.os.AsyncTask;

import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import static russianapp.tools.guitar_tunings.DefaultErrorActivity.startApp;

public class xMail {
    private static String SMTP_SERVER = "smtp.yandex.ru";
    private static String SMTP_Port = "465";
    private static String FILE_PATH = null;
    private static String REPLY_TO = "replyto=java-online@mail.ru";
    public String SMTP_AUTH_USER = "";
    public String SMTP_AUTH_PWD = "";
    public String EMAIL_FROM = "";
    public String EMAIL_TO = "";

    public Message message = null;

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    public xMail() {
        // do nothing
    }

    private static MimeBodyPart createFileAttachment(String filepath) throws MessagingException {
        // Создание MimeBodyPart
        MimeBodyPart mbp = new MimeBodyPart();

        // Определение файла в качестве контента
        FileDataSource fds = new FileDataSource(filepath);
        mbp.setDataHandler(new DataHandler(fds));
        mbp.setFileName(fds.getName());
        return mbp;
    }

    public void initialize(final String thema) {
        // Настройка SMTP SSL
        Properties properties = new Properties();
        //properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", SMTP_SERVER);
        properties.put("mail.smtp.port", SMTP_Port);
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");

        try {
            Authenticator auth = new EmailAuthenticator(SMTP_AUTH_USER, SMTP_AUTH_PWD);
            Session session = Session.getDefaultInstance(properties, auth);
            session.setDebug(false);

            InternetAddress email_from = new InternetAddress(EMAIL_FROM);
            InternetAddress email_to = new InternetAddress(EMAIL_TO);
            InternetAddress reply_to = (REPLY_TO != null) ? new InternetAddress(REPLY_TO) : null;

            message = new MimeMessage(session);
            message.setFrom(email_from);
            message.setRecipient(Message.RecipientType.TO, email_to);
            message.setSubject(thema);
            if (reply_to != null)
                message.setReplyTo(new Address[]{reply_to});

        } catch (AddressException e) {
            System.err.println(e.getMessage());
        } catch (MessagingException e) {
            System.err.println(e.getMessage());
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public static class sendMessageAsync extends AsyncTask<String, Integer, Boolean> {

        Activity activity;
        Message message;

        public sendMessageAsync(Activity activity, Message message) {
            super();
            this.activity = activity;
            this.message = message;
        }

        @Override
        protected Boolean doInBackground(String... arg) {
            return sendMessage(arg[0]);
        }

        @Override
        protected void onPostExecute(Boolean res) {
            if (res)
                startApp(activity);
        }

        boolean sendMessage(final String text) {
            boolean result = false;
            try {
                // Содержимое сообщения
                Multipart mmp = new MimeMultipart();

                // Текст сообщения
                MimeBodyPart bodyPart = new MimeBodyPart();
                bodyPart.setContent(text, "text/plain; charset=utf-8");
                mmp.addBodyPart(bodyPart);

                // Вложение файла в сообщение
                if (FILE_PATH != null) {
                    MimeBodyPart mbr = createFileAttachment(FILE_PATH);
                    mmp.addBodyPart(mbr);
                }
                // Определение контента сообщения
                message.setContent(mmp);

                // Отправка сообщения
                Transport.send(message);
                result = true;

            } catch (Exception e) {
                // Ошибка отправки сообщения
                System.err.println(e.getMessage());
                e.printStackTrace();
            }
            return result;
        }
    }

    public class EmailAuthenticator extends javax.mail.Authenticator {
        private String login;
        private String password;

        EmailAuthenticator(final String login, final String password) {
            this.login = login;
            this.password = password;
        }

        public PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(login, password);
        }
    }
}