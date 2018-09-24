/**
 * Created by yjlee on 2018-09-15.
 */
public class ReflectiveChannelFactoryTest {

    public void main(String[] args){



    }
}

abstract class AbstractBootStrapTest<B extends AbstractBootStrapTest<B,C> ,C extends ChannelTest >{

     public B channel(Class<? extends C> channelClass){



         return (B)this;
    }


}

class ChannelTest {

}
