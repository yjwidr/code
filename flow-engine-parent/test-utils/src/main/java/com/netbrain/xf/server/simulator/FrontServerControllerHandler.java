package com.netbrain.xf.server.simulator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class FrontServerControllerHandler extends ChannelInboundHandlerAdapter {
    private final static String HEADER_DELIM = "\r\n\r\n";

    private void handleLogin(ChannelHandlerContext ctx, String body) {
        String simpleResult = "{\"version\":1,\"protocol\":8100,\"bodysize\":37}" + HEADER_DELIM +
                "{\"heartbeatInterval\":20,\"retCode\":0}\n";
        final ByteBuf response = ctx.alloc().buffer();
        response.writeBytes(simpleResult.getBytes());
        ctx.writeAndFlush(response);
    }

    /**
     * Generate a long (longer than 1024 bytes) string as result to test if the client can handle it.
     * See ENG-43833.
     * @param ctx
     */
    private void handleGetRunningDtgs(ChannelHandlerContext ctx) {
        String resultBody = "{\"data_task_group_id\":[";
        for (int i = 0; i < 49; i++) {
            resultBody += "\"" + UUID.randomUUID().toString() + "\",";
        }
        resultBody += "\"" + UUID.randomUUID().toString();
        resultBody += "\"],\"protocol_version\":1}\n";

        String simpleResult = "{\"version\":1,\"protocol\":8104,\"bodysize\":" + resultBody.length() + "}" + HEADER_DELIM + resultBody;

        final ByteBuf response = ctx.alloc().buffer();
        response.writeBytes(simpleResult.getBytes());
        ctx.writeAndFlush(response);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf in = (ByteBuf) msg;
        try {
            StringBuffer buf = new StringBuffer();
            while (in.isReadable()) {
                buf.append((char) in.readByte());
            }

            String request = buf.toString();
            String[] segments = request.split(HEADER_DELIM);
            if (segments.length == 2) {
                System.out.println("header:" + segments[0]);
                ObjectMapper mapper = new ObjectMapper();

                Map<String, Object> headerMap = null;
                try {
                    headerMap = mapper.readValue(segments[0], new TypeReference<Map<String, Object>>() { });
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (headerMap.get("protocol") != null && headerMap.get("protocol") instanceof Integer) {
                    switch (((Integer) headerMap.get("protocol")).intValue()) {
                        case 8000:
                            handleLogin(ctx, segments[1]);
                            break;
                        case 8004:
                            handleGetRunningDtgs(ctx);
                            break;
                        case 8002:
                            ctx.close();
                            break;
                    }
                }
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
