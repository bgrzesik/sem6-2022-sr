package pl.edu.agh.student.bgrzesik.sr.lab3.admin;

import pl.edu.agh.student.bgrzesik.sr.lab3.Messages;

public interface IAdminMessageHandler {
    void onAdminMessage(Messages.AdminMessage message, String replyTo);
}
