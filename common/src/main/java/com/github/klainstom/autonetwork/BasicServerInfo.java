package com.github.klainstom.autonetwork;

import com.google.gson.annotations.Expose;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class BasicServerInfo {
    @Expose private final String group;
    @Expose private final Address address;
    @Expose private final Version version;
    @Expose private final Version minVersion;
    @Expose private final Players players;

    public BasicServerInfo(String group, Address address, Version version, Version minVersion, Players players) {
        this.group = group;
        this.address = address;
        this.version = version;
        this.minVersion = minVersion;
        this.players = players;
    }

    public String getGroup() { return group; }

    public Address getAddress() {
        return address;
    }

    public Version getVersion() {
        return version;
    }

    public Version getMinVersion() {
        return minVersion;
    }

    public Players getPlayers() {
        return players;
    }

    public String getId() {
        try {
            String rawId = String.format("%s%s%d", group, address.host, address.port);
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] messageDigest = md.digest(rawId.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            StringBuilder hashtext = new StringBuilder(no.toString(16));
            while (hashtext.length() < 32) {
                hashtext.insert(0, "0");
            }
            return group + "-" + hashtext;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return group;
    }

    public static class Address {
        @Expose private final String host;
        @Expose private final int port;

        public Address(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }
    }
    public static class Version {
        @Expose private final String name;
        @Expose private final int protocol;

        public Version(String name, int protocol) {
            this.name = name;
            this.protocol = protocol;
        }

        public String getName() {
            return name;
        }

        public int getProtocol() {
            return protocol;
        }
    }

    public static class Players {
        @Expose private final int max;
        @Expose private final int online;

        public Players(Integer max, int online) {
            if (max == null) max = -1;
            this.max = max;
            this.online = online;
        }

        public int getMax() {
            return max;
        }

        public int getOnline() {
            return online;
        }
    }
}
