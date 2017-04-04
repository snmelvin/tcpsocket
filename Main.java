package com.company;

import java.util.*;

public class Main {

    public static void main(String[] args) {
	// write your code here
        final String UDPPORT = args[0].substring(10);
        final String LOSSRATE = args[1].substring(11);
        String program = args[2];

        Properties prop = new Properties();
        System.setProperty("UDPPORT", UDPPORT);
        System.setProperty("LOSSRATE", LOSSRATE);

        if (program.equals("server1")) {
            String port = args[3];
            System.setProperty("PORT", port);
            server1 server1 = new server1();
            String[] serverArgs = new String[1];
            serverArgs[0] = port;
            server1.main(serverArgs);
        }
        else if (program.equals("server2")) {
            String port = args[3];
            System.setProperty("PORT", port);
            server2 server2 = new server2();
            String[] serverArgs = new String[1];
            serverArgs[0] = port;
            server2.main(serverArgs);
        }
        else if (program.equals("client1")) {
            String hostName = args[3];
            System.setProperty("HOSTNAME", hostName);
            String port = args[4];
            System.setProperty("PORT", port);
            String[] serverArgs = new String[2];
            serverArgs[0] = hostName;
            serverArgs[1] = port;
            client1.main(serverArgs);
        }
        else if (program.equals("client2")) {
            String hostName = args[3];
            System.setProperty("HOSTNAME", hostName);
            String port = args[4];
            System.setProperty("PORT", port);
            String[] serverArgs = new String[2];
            serverArgs[0] = hostName;
            serverArgs[1] = port;
            client2.main(serverArgs);
        }

    }
}
