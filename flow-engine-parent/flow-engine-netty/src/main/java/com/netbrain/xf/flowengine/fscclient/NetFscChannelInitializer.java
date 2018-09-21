
package com.netbrain.xf.flowengine.fscclient;

import com.netbrain.ngsystem.model.FSCInfo;
import com.netbrain.ngsystem.model.FrontServerController;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;
/**
 * -Djavax.net.ssl.trustStore=d:\mongoStore.ts -Djavax.net.ssl.trustStorePassword=netbrain -Djavax.net.debug=ssl,handshake
 *
 */
public class NetFscChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final SslContext sslCtx;
    private FrontServerController fsc;
    private NetlibResponseHandler responseHandler;

    public NetFscChannelInitializer(SslContext sslCtx, FrontServerController fsc, NetlibResponseHandler responseHandler) {
        this.sslCtx = sslCtx;
        this.fsc = fsc;
        this.responseHandler = responseHandler;
    }

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        FSCInfo activeFscInfo = fsc.getActiveFSCInfo();
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(sslCtx.newHandler(ch.alloc(), activeFscInfo.getIpOrHostname(), activeFscInfo.getPort()));
        pipeline.addLast(responseHandler);
    }
}
