package eu.asylum.common.cloud.servers;

import lombok.Getter;
import lombok.Setter;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

@Getter
@Setter
public final class Pinger implements Serializable, Cloneable {

    private String address;
    private int port, timeout, pingVersion, protocolVersion, playersOnline, maxPlayers;
    private String gameVersion, motd, lastResponse;

    public Pinger(final String address, final int port) {
        this.address = "localhost";
        this.port = 25565;
        this.timeout = 2300;
        this.pingVersion = -1;
        this.protocolVersion = -1;
        this.playersOnline = -1;
        this.maxPlayers = -1;
        this.setAddress(address);
        this.setPort(port);
    }


    public boolean ping() {
        try {
            Socket socket = new Socket();
            socket.setSoTimeout(this.timeout);
            socket.connect(new InetSocketAddress(this.getAddress(), this.getPort()), this.getTimeout());
            final OutputStream outputStream = socket.getOutputStream();
            final DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
            final InputStream inputStream = socket.getInputStream();
            final InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_16BE);

            dataOutputStream.write(0xFE);

            int b;
            final StringBuilder str = new StringBuilder();
            while ((b = inputStream.read()) != -1) {
                if (b > 16 && b != 255 && b != 23 && b != 24) {
                    str.append((char) b);
                }
            }

            this.lastResponse = str.toString();
            if (lastResponse.startsWith("\u00A7")) {
                final String[] data = lastResponse.split("\u0000");
                this.setPingVersion(Integer.parseInt(data[0].substring(1)));
                this.setProtocolVersion(Integer.parseInt(data[1]));
                this.setGameVersion(data[2]);
                this.setMotd(data[3]);
                this.setPlayersOnline(Integer.parseInt(data[4]));
                this.setMaxPlayers(Integer.parseInt(data[5]));
            } else {
                final String[] data = lastResponse.split("\u00A7");
                this.setMotd(data[0]);
                this.setPlayersOnline(Integer.parseInt(data[1]));
                this.setMaxPlayers(Integer.parseInt(data[2]));
            }
            dataOutputStream.close();
            outputStream.close();
            inputStreamReader.close();
            inputStream.close();
            socket.close();
        } catch (IOException exception) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        String sb = "Pinger{" + "address='" + address + '\'' +
                ", port=" + port +
                ", timeout=" + timeout +
                ", pingVersion=" + pingVersion +
                ", protocolVersion=" + protocolVersion +
                ", playersOnline=" + playersOnline +
                ", maxPlayers=" + maxPlayers +
                ", gameVersion='" + gameVersion + '\'' +
                ", motd='" + motd + '\'' +
                ", lastResponse='" + lastResponse + '\'' +
                '}';
        return sb;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

}
