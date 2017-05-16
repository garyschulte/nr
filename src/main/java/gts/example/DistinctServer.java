package gts.example;

import gts.example.store.BooleanArrayStore;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @since 5/15/17.
 */
public class DistinctServer {

    //ASSUMPTION: maximum character size of 9 digit int representation (including leading zeroes) is set to:
    static final int MAX_FRAME_SIZE = 20;
    static final int PORT = 4000;
    static final Logger LOG = LoggerFactory.getLogger(DistinctServer.class);

    public static void main(String [] args) {

        try {
            // log writer
            Writer fw = new FileWriter("numbers.log", false);

            // store implementation
            DistinctStore storeImpl = new BooleanArrayStore();

            // scheduled stats writer
            ScheduledExecutorService statsService = Executors.newScheduledThreadPool(1);
            statsService.scheduleAtFixedRate(new StatsThread(storeImpl), 10, 10, SECONDS);

            // run the netty server
            new DistinctServer().run(PORT, MAX_FRAME_SIZE, storeImpl, fw);

            //flush log and shutdown scheduled stats
            shutdown(fw, statsService);

        } catch (InterruptedException e) {
            LOG.error("error shutting down server", e);
        } catch (IOException ioex) {
            LOG.error("failed to create numbers.log file", ioex);
        }
    }

    public void run(int port, int maxFrameSize, DistinctStore store, Writer log) throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup(5);

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast("frame decoder", new LineBasedFrameDecoder(maxFrameSize))
                                    .addLast("string decoder", new StringDecoder())
                                    .addLast("number decoder", new DistinctNumberDecoder(log, store));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(port).sync();


            // Wait until the server socket is closed.
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    static void shutdown(Writer fw, ExecutorService statsService) throws IOException {
        fw.close();
        statsService.shutdown();
    }

    /**
     * cheapo inner class to parse integers from strings and handle
     */
    class DistinctNumberDecoder extends ChannelInboundHandlerAdapter {

        private final DistinctStore storeImpl;
        private final Writer log;

        public DistinctNumberDecoder(Writer log, DistinctStore storeImpl) {
            this.storeImpl = storeImpl;
            this.log = log;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            try {
                String line = (String) msg;
                if (line.equals("terminate")) {
                    ctx.channel().close();
                    ctx.channel().parent().close();
                } else {
                    Integer intVal = Integer.parseInt((String) msg);
                    if (!storeImpl.getSetPresent(intVal)) {
                        log.write(String.format("%09d\n", intVal));
                        log.flush();
                    }
                }
            } catch (NumberFormatException | ClassCastException ex) {
                throw new DecoderException(ex);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            //disregard and move along, according to reqs
        }
    }

    /**
     * lightweight thread to periodically write stats about the number store
     */
    public static class StatsThread implements Runnable {

        private final DistinctStore storeImpl;

        public StatsThread(DistinctStore storeImpl) {
            this.storeImpl = storeImpl;
        }
        @Override
        public void run() {
            Triple<Integer, Long, Integer> stats = storeImpl.checkpoint();
            System.out.println(String.format("Received %d unique numbers, %d duplicates.  Unique total: %d"
                    , stats.getLeft()
                    , stats.getMiddle()
                    , stats.getRight()));
        }
    }

}
