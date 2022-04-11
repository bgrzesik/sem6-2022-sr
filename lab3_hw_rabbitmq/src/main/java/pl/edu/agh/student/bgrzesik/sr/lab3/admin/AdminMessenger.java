package pl.edu.agh.student.bgrzesik.sr.lab3.admin;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import pl.edu.agh.student.bgrzesik.sr.lab3.Channels;
import pl.edu.agh.student.bgrzesik.sr.lab3.Messages;
import pl.edu.agh.student.bgrzesik.sr.lab3.utils.ConnectionUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeoutException;

public class AdminMessenger {

    public static void message(String whom, String what) throws IOException, TimeoutException {
        if ("-".equals(what)) {
            System.out.print("Enter message: ");
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            try {
                what = reader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Connection connection = ConnectionUtils.newLocalhostConnection();
        Channel channel = connection.createChannel();

        Messages.AdminMessage adminMessage = Messages.AdminMessage.newBuilder()
                .setContent(what)
                .build();

        channel.exchangeDeclare(Channels.ADMIN_EXCHANGE, BuiltinExchangeType.DIRECT);
        channel.basicPublish(Channels.ADMIN_EXCHANGE, whom, null, adminMessage.toByteArray());


        channel.close();
        connection.close();
    }

}
