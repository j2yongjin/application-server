package nettyniotcpserver.protocol;

/**
 * application-server
 *
 * @auther : yjlee
 * @date : 2018-09-10
 * @desc :
 */
public class User extends AbstractProtocol {

    public User(TransactionCode trsactionCode, int bodyLength, String id, String password) {
        super(trsactionCode, bodyLength);
        this.id = id;
        this.password = password;
    }

    String id;
    String password;

    @Override
    public TransactionCode getTrCode() {
        return null;
    }
}
