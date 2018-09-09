package nettyniotcpserver.protocol;

import lombok.Data;

/**
 * application-server
 *
 * @auther : yjlee
 * @date : 2018-09-10
 * @desc :
 */
@Data
public abstract class AbstractProtocol {

    int type;
    int length;

    public AbstractProtocol(int type, int length) {
        this.type = type;
        this.length = length;
    }

}
