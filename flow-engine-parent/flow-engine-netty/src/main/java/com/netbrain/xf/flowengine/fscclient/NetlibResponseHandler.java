package com.netbrain.xf.flowengine.fscclient;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.util.ReferenceCountUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NetlibResponseHandler implements ChannelInboundHandler {
    private static Logger logger = LogManager.getLogger(NetlibResponseHandler.class.getSimpleName());

    private String readOnceResult;

    private FSCResponse fscResponse = new FSCResponse();

    public String getResult() {
        if (fscResponse.hasFullBody()) {
            return fscResponse.getFullResponse();
        } else {
            return "";
        }
    }

    @Override
    public void channelRegistered(ChannelHandlerContext channelHandlerContext) throws Exception {

    }

    @Override
    public void channelUnregistered(ChannelHandlerContext channelHandlerContext) throws Exception {

    }

    @Override
    public void channelActive(ChannelHandlerContext channelHandlerContext) throws Exception {

    }

    @Override
    public void channelInactive(ChannelHandlerContext channelHandlerContext) throws Exception {

    }

    @Override
    public void channelRead(ChannelHandlerContext channelHandlerContext, Object msg) throws Exception {
        ByteBuf in = (ByteBuf) msg;
        try {
            StringBuffer buf = new StringBuffer();
            while (in.isReadable()) {
                buf.append((char) in.readByte());
            }

            readOnceResult = buf.toString();
            logger.debug("received message from FSC: " + readOnceResult);
            fscResponse.appendSegment(readOnceResult);
            fscResponse.parseResponse();

            if (fscResponse.getResponseType() == FSCResponse.fsXFLoginRsp) {
                FSCLoginResponse loginResponse = FSCLoginResponse.parseLoginResBody(fscResponse.getBody());
                if (loginResponse.isSuccess()) {
                    String dtgId = channelHandlerContext.channel().attr(NetlibClient.dtgIdAttrKey).get();
                    Integer commandType = channelHandlerContext.channel().attr(NetlibClient.cmdTypeAttrKey).get();
                    String request;
                    if (commandType == FSCRequest.CMD_fsRunningTaskReq) {
                        request = new FSCRequest().getRunningDTGsReqeust(commandType);
                    } else {
                        request = new FSCRequest().getDTGRequest(dtgId, commandType);
                    }
                    logger.debug("FSC Request: " + request);
                    fscResponse = new FSCResponse();
                    channelHandlerContext.channel().writeAndFlush(Unpooled.copiedBuffer(request.getBytes())).sync();
                } else {
                    logger.info("Failed to login to FSC due to " + loginResponse.getRetCodeDesc());
                }
            } else {
                if (fscResponse != null && fscResponse.hasFullBody()) {
                    channelHandlerContext.channel().close();
                }
            }
        } catch (Exception e) {
            logger.error("Failed to handle FSC response", e);
            channelHandlerContext.channel().close();
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext channelHandlerContext) throws Exception {

    }

    @Override
    public void userEventTriggered(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {

    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext channelHandlerContext) throws Exception {

    }

    @Override
    public void handlerAdded(ChannelHandlerContext channelHandlerContext) throws Exception {

    }

    @Override
    public void handlerRemoved(ChannelHandlerContext channelHandlerContext) throws Exception {

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext channelHandlerContext, Throwable throwable) throws Exception {

    }
}
