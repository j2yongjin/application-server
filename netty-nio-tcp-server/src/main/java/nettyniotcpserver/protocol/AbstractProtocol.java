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
public abstract class AbstractProtocol implements Protocol {

    TransactionCode trsactionCode;
    int bodyLength;

    public AbstractProtocol(TransactionCode trsactionCode, int bodyLength) {
        this.trsactionCode = trsactionCode;
        this.bodyLength = bodyLength;
    }
}
