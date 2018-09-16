package niotcpserver.socket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * application-server
 *
 * @auther : yjlee
 * @date : 2018-09-17
 * @desc :
 */
public class ServerSocket {

    SocketChannel socketChannel;

    public ServerSocket(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    ByteBuffer byteBuffer = ByteBuffer.allocate(100);

    public String read(int size){

        try {
            byteBuffer.flip();
            byteBuffer = ByteBuffer.allocate(size);
            socketChannel.read(byteBuffer);

            byte[] recv = new byte[byteBuffer.position()];
            byteBuffer.rewind();
            byteBuffer.get(recv);
            return new String(recv);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return "";

    }

    public int write(byte[] writebyte){

        int writeByteSize = 0;
        try {
            byteBuffer.flip();
            byteBuffer.clear();
            byteBuffer = ByteBuffer.wrap(writebyte);
            while (writeByteSize < writebyte.length)
                writeByteSize+= socketChannel.write(byteBuffer);
            return writeByteSize;
        }catch (Exception e){
            System.out.println(" write error : " + e);
        }

        return 0;
    }



}
