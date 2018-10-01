
# JAVA Cuncurrent
참고 자료 : http://tutorials.jenkov.com/java-util-concurrent/index.html

## mutex
뮤텍스는 액세스 카운트가 1 인 세마포어입니다. 은행에서 사물함을 사용하는 상황을 고려하십시오. 일반적으로 한 사람 만이 라커룸에 들어갈 수 있습니다.

    package com.mkyong;
    import java.util.concurrent.Semaphore;
    public class SemaphoreTest {

	// max 1 people
	static Semaphore semaphore = new Semaphore(1);

	static class MyLockerThread extends Thread {

		String name = "";

		MyLockerThread(String name) {
			this.name = name;
		}

		public void run() {

			try {

				System.out.println(name + " : acquiring lock...");
				System.out.println(name + " : available Semaphore permits now: " 
								+ semaphore.availablePermits());

				semaphore.acquire();
				System.out.println(name + " : got the permit!");

				try {

					for (int i = 1; i <= 5; i++) {

						System.out.println(name + " : is performing operation " + i 
								+ ", available Semaphore permits : "
								+ semaphore.availablePermits());

						// sleep 1 second
						Thread.sleep(1000);

					}

				} finally {

					// calling release() after a successful acquire()
					System.out.println(name + " : releasing lock...");
					semaphore.release();
					System.out.println(name + " : available Semaphore permits now: " 
								+ semaphore.availablePermits());
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

  
  
## semapore
스레드간 시그널 전송

    Semaphore semaphore = new Semaphore(1);
    //critical section
    semaphore.acquire();
    ...
    semaphore.release();



