package nettyniotcpserver.protocol;

/**
 * application-server
 *
 * @auther : yjlee
 * @date : 2018-09-10
 * @desc :
 */
public class User extends AbstractProtocol {

    public User(int type, int length) {
        super(type, length);
    }

    String id;
    String password;
}
