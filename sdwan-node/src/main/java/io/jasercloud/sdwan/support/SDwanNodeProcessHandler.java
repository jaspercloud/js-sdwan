package io.jasercloud.sdwan.support;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.springframework.beans.factory.InitializingBean;

import java.util.UUID;
import java.util.concurrent.TimeUnit;


@ChannelHandler.Sharable
public class SDwanNodeProcessHandler extends SimpleChannelInboundHandler<SDWanProtos.Message> implements InitializingBean {

    private SDWanNodeProperties properties;

    public SDwanNodeProcessHandler(SDWanNodeProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        ctx.executor().scheduleAtFixedRate(() -> {
            heart(ctx);
        }, 0, 30 * 1000, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SDWanProtos.Message request) throws Exception {
        switch (request.getType().getNumber()) {
            case SDWanProtos.MsgType.HeartType_VALUE: {
                System.out.println("heart success");
                break;
            }
            case SDWanProtos.MsgType.RegRespType_VALUE:
            case SDWanProtos.MsgType.NodeArpRespType_VALUE: {
                AsyncTask.completeTask(request.getReqId(), request);
                break;
            }
        }
    }

    private void heart(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        SDWanProtos.Message message = SDWanProtos.Message.newBuilder()
                .setReqId(UUID.randomUUID().toString())
                .setType(SDWanProtos.MsgType.HeartType)
                .build();
        channel.writeAndFlush(message);
    }
}
