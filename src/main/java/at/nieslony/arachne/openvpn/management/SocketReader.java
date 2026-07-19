/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.openvpn.management;

/**
 *
 * @author claas
 */
class SocketReader {
    /*
    public static final Logger log = LoggerFactory.getLogger(SocketReader.class);

    SocketChannel channel;
    ByteBuffer buffer = ByteBuffer.allocate(1024);
    StringBuilder str = new StringBuilder();

    SocketReader(SocketChannel channel) {
        this.channel = channel;
    }

    Optional<String> readLine() throws IOException {
        for (;;) {
            if (buffer.hasRemaining()) {
                while (buffer.hasRemaining()) {
                    byte b = buffer.get();
                    if (b == '\n') {
                        String ret = str.toString();
                        str = new StringBuilder();
                        buffer.compact();
                        return Optional.of(ret);
                    }
                    str.append((char) b);
                }
                log.info("Not yet at EOL, need more data");
                buffer.flip();
            }
            int bytesRead = channel.read(buffer);
            if (bytesRead == -1) {
                return Optional.empty();
            }
            if (bytesRead > 0) {
                buffer.flip();
                log.info("%d bytes read".formatted(bytesRead));
            }
        }
    }*/
}
