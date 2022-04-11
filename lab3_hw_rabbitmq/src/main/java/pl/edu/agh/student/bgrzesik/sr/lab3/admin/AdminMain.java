package pl.edu.agh.student.bgrzesik.sr.lab3.admin;

import pl.edu.agh.student.bgrzesik.sr.lab3.Channels;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class AdminMain {

    public static void main(String[] args) throws IOException, TimeoutException {
        if (args.length == 1 && "sniff".equals(args[0])) {
            AdminSniffer.sniff();
        } else if (args.length == 2 && "broadcast".equals(args[0])) {
            AdminMessenger.message(Channels.ADMIN_KEY_ALL, args[1]);
        } else if (args.length == 2 && "sendTeams".equals(args[0])) {
            AdminMessenger.message(Channels.ADMIN_KEY_TEAMS, args[1]);
        } else if (args.length == 2 && "sendSuppliers".equals(args[0])) {
            AdminMessenger.message(Channels.ADMIN_KEY_SUPPLIERS, args[1]);
        } else {
            throw new IllegalArgumentException("usage: sniff|broadcast|sendTeams|sendSuppliers [message]");
        }

    }

}
