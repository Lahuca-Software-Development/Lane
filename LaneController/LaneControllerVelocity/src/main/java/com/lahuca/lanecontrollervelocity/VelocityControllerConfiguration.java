package com.lahuca.lanecontrollervelocity;

public class VelocityControllerConfiguration {

    private final Connection connection;
    private final DataManager dataManager;
    private final Commands commands;

    public VelocityControllerConfiguration() {
        connection = new Connection();
        dataManager = new DataManager();
        commands = new Commands();
    }

    public Connection getConnection() {
        return connection;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public Commands getCommands() {
        return commands;
    }

    public static class Connection {

        private final int port;
        private final Type type;
        private final Socket socket;

        public Connection() {
            port = 3306;
            type = Type.SOCKET;
            socket = new Socket();
        }

        public int getPort() {
            return port;
        }

        public Type getType() {
            return type;
        }

        public Socket getSocket() {
            return socket;
        }

        public enum Type {

            SOCKET

        }

        public static class Socket {

            private final boolean ssl;

            public Socket() {
                ssl = true;
            }

            public boolean isSsl() {
                return ssl;
            }

        }

    }

    public static class DataManager {

        private final Type type;
        private final File file;
        private final MySQL mysql;

        public DataManager() {
            type = Type.FILE;
            file = new File();
            mysql = new MySQL();
        }

        public Type getType() {
            return type;
        }

        public File getFile() {
            return file;
        }

        public MySQL getMysql() {
            return mysql;
        }

        public enum Type {

            FILE, MYSQL

        }

        public static class File {

            private final String name;

            public File() {
                name = "data";
            }

            public String getName() {
                return name;
            }

        }

        public static class MySQL {

            private final String host;
            private final int port;
            private final String username;
            private final String password;
            private final String database;
            private final String prefix;

            public MySQL() {
                host = "localhost";
                port = 3306;
                username = "username";
                password = "password";
                database = "database";
                prefix = "lane";
            }

            public String getHost() {
                return host;
            }

            public int getPort() {
                return port;
            }

            public String getUsername() {
                return username;
            }

            public String getPassword() {
                return password;
            }

            public String getDatabase() {
                return database;
            }

            public String getPrefix() {
                return prefix;
            }

        }

    }

    public static class Commands {

        private final boolean friend;
        private final boolean party;

        public Commands() {
            friend = true;
            party = true;
        }

        public boolean isFriend() {
            return friend;
        }

        public boolean isParty() {
            return party;
        }

    }

}
