package com.netbrain.xf.flowengine.fscclient;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;

import com.netbrain.ngsystem.model.FSCInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.netbrain.ngsystem.model.FrontServerController;
import com.netbrain.xf.flowengine.metric.Metrics;
import com.netbrain.xf.flowengine.utility.CommonUtil;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.AttributeKey;
public class NetlibClient {

    private static Logger logger = LogManager.getLogger(NetlibClient.class.getSimpleName());

    public static AttributeKey<String> dtgIdAttrKey = AttributeKey.valueOf("dtgIdAttrKey");
    public static AttributeKey<Integer> cmdTypeAttrKey = AttributeKey.valueOf("cmdTypeAttrKey");
    public NetlibClient() {
        
    }
    private FrontServerController fsc;
    private Metrics metrics;

    public NetlibClient(FrontServerController fsc, Metrics metrics) {
        this.fsc = fsc;
        this.metrics = metrics;
    }

    /**
     *
     * @param commandType, see FSCRequest class
     * @param dtgId
     * @return
     * @throws InterruptedException
     */
    public String sendCommand(int commandType, String dtgId) throws InterruptedException {
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        InputStream stream = null;

        try {
            NetlibResponseHandler responseHandler = new NetlibResponseHandler();
            Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            b.option(ChannelOption.SO_KEEPALIVE, true);
            if (!fsc.isUseSSL()) {
                b.handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(responseHandler);
                    }
                });
            } else {
                try {
                    if (fsc.isConductCertAuthVerify()) {
                        if (fsc.getCertificateType()==2 ) {
                            if(!StringUtils.isEmpty(fsc.getCertificate())) {
                                stream = new ByteArrayInputStream(fsc.getCertificate().getBytes(StandardCharsets.UTF_8));
                                final SslContext sslCtx = SslContextBuilder.forClient().trustManager(stream).build();
                                b.handler(new NetFscChannelInitializer(sslCtx, fsc, responseHandler));
                            }else {
                                logger.error("fsc.getCertificate() is empty by id:",fsc.getId());
                            }
                        } else if(fsc.getCertificateType()==1) {
                                final SslContext sslCtx = SslContextBuilder.forClient().trustManager(CommonUtil.getTrustManager()).build();
                                b.handler(new NetFscChannelInitializer(sslCtx, fsc, responseHandler));
                        }
                    } else {
                        final SslContext sslCtx = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
                        b.handler(new NetFscChannelInitializer(sslCtx, fsc, responseHandler));
                    }
                } catch (SSLException e) {
                    logger.error("failed to instance SslContext", e);
                }
            }

            // Start the client.
            FSCInfo activeFscInfo = fsc.getActiveFSCInfo();
            ChannelFuture f = b.connect(activeFscInfo.getIpOrHostname(), activeFscInfo.getPort()).sync();
            f.channel().attr(dtgIdAttrKey).set(dtgId);
            f.channel().attr(cmdTypeAttrKey).set(commandType);

            String request = new FSCRequest().getLoginReq(fsc);
            ChannelFuture f2 = f.channel().writeAndFlush(Unpooled.copiedBuffer(request.getBytes())).sync();

            if (metrics != null) {
                if (commandType == FSCRequest.CMD_fsStopDTGReq) {
                    metrics.addDtgStopCount(1);
                } else if (commandType == FSCRequest.CMD_fsTaskgroupStatusReq) {
                    metrics.addDtgQueryCount(1);
                }
            }
            // Wait until the connection is closed.
            f2.channel().closeFuture().sync();
            return responseHandler.getResult();
        }catch(Exception e) {
            logger.error("failed to conntect to fsc", e);
            return null;
        }finally {
            workerGroup.shutdownGracefully();
            if(stream!=null) {
               try {
                stream.close();
                } catch (IOException e) {
                    logger.error("failed to close iostream ",e);
                } 
            }
        }
    }
}
