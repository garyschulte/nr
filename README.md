# gary-schulte

May-15-2017 11am initial commit to verify I have write access to this repo. yes I know what year it is :)


Assumptions:
  * unsigned ints
  * to achieve ~2 million numbers processed per 10 seconds the clients need to saturate the server
    (not disconnect and reconnect rapidly only sending a few numbers)


That is about it.  I really didn't expect the hamfisted array read/write locking to be as performant as it seems to be.

It took a bit of reading to get familiar with netty, but it looks like it paid off.  I suspect I would have had a hard
 time getting similar performance without a NIO socket implementation.