import static net.grinder.script.Grinder.grinder
import static org.junit.Assert.*
import static org.hamcrest.Matchers.*
import net.grinder.plugin.http.HTTPRequest
import net.grinder.plugin.http.HTTPPluginControl
import net.grinder.script.GTest
import net.grinder.script.Grinder
import net.grinder.scriptengine.groovy.junit.GrinderRunner
import net.grinder.scriptengine.groovy.junit.annotation.BeforeProcess
import net.grinder.scriptengine.groovy.junit.annotation.BeforeThread
// import static net.grinder.util.GrinderUtils.* // You can use this if you're using nGrinder after 3.2.3
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

import java.util.Date
import java.util.List
import java.util.ArrayList

import HTTPClient.Cookie
import HTTPClient.CookieModule
import HTTPClient.HTTPResponse
import HTTPClient.NVPair
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

@RunWith(GrinderRunner)
class TestRunner {

	public static GTest test
	public static String ip="123.2.134.162";
	public static int port=17001;


	@BeforeProcess
	public static void beforeProcess() {
		test = new GTest(1, "Test1")

		grinder.logger.info("before process.");
	}

	@BeforeThread
	public void beforeThread() {
		test.record(this, "test")
		grinder.statistics.delayReports=true;
		grinder.logger.info("before thread.");
	}

	@Before
	public void before() {

		grinder.logger.info("before thread. init headers and cookies");
	}

	@Test
	public void test(){

		TcpClient client = new TcpClient(ip,port);
		client.connect();

		client.write(SendData.getSendData());
		String result = client.read();

		client.disconnect();
		assertThat(result, containsString("RETCODE:=0"));
	}
}

class SendData {
	public static getSendData(){
		StringBuilder data = new StringBuilder();
        data.append("CMD:=CMD_PKGPRICE\n");
        data.append("ZONE:=enfax30\n");
        data.append("USERID:=sudisai\n");
        data.append("GUBUN:=1\n");
        data.append("PKGCODE:=1002\n");
        data.append("NEXTPKGCODE:=1003\n");
        return  data.toString();
	}

}

class TcpClient {

    String ip;
    int port;
    ByteBuffer byteBuffer = ByteBuffer.allocate(100);

    SocketChannel socketChannel;

    public TcpClient(String ip , int port){
        this.ip = ip;
        this.port = port;
    }


    public void connect() throws IOException {
        socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(true);
        socketChannel.connect(new InetSocketAddress(ip , port ));
    }

    public void write(String data) throws IOException {
        byte[] contentsByte = data.getBytes();
        byte[] sendByte = new byte[contentsByte.length + 8];
        String dataLength = String.format("%08d",contentsByte.length);

        int destPos = 0;
        System.arraycopy(dataLength.getBytes(),0,sendByte,0,8);
        destPos += 8;
        System.arraycopy(contentsByte,0,sendByte,destPos,contentsByte.length);

        byteBuffer.flip();
        byteBuffer.clear();

        byteBuffer = ByteBuffer.wrap(sendByte);
        int writeCount = socketChannel.write(byteBuffer);
    }

    public String read() throws IOException {
        byteBuffer.clear();
        byteBuffer = ByteBuffer.allocate(200);
        socketChannel.read(byteBuffer);

        byte[] recv = new byte[byteBuffer.position()];
        byteBuffer.rewind();
        byteBuffer.get(recv);

        return new String(recv);
    }

    public void disconnect() throws IOException {
        socketChannel.close();
    }
}




