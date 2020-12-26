import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.*;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.GlobalEventExecutor;

public class ServerHandler extends ChannelInboundHandlerAdapter {
    enum ClientState {
        AUTORIZATION,
        INCHAT
    }

    private class Client {
        private ClientState state;
        private String name;

        public Client(ClientState s) {
            state = s;
        }

        public void setName(String n) {
            name = n;
        }

        public String name() {
            return name;
        }

        public ClientState state() {
            return state;
        }

        public void toChat() {
            state = ClientState.INCHAT;
        }
    }

    private static final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private static final Map<String, Client> clients = new HashMap<String, Client>();

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        clients.put(ctx.channel().remoteAddress().toString(), new Client(ClientState.AUTORIZATION));
        ctx.channel().writeAndFlush("Enter your name:\n");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) throws Exception {
        String msg = (String)message;
        Channel channel = ctx.channel();
        Client client = clients.get(channel.remoteAddress().toString());
        if (client.state() == ClientState.AUTORIZATION) {
            System.out.println(msg.toString());
            clients.get(channel.remoteAddress().toString()).setName(msg);
            clients.get(channel.remoteAddress().toString()).toChat();
            for (Channel ch : channels) {
                ch.writeAndFlush("[CHAT-BOT] - " + client.name() + " has joined\n");
            }
            channels.add(channel);
            channel.writeAndFlush("[CHAT-BOT] - Welcome!\n");
        } else if (client.state() == ClientState.INCHAT) {
            for (Channel ch : channels) {
                if (ch != channel)
                    ch.writeAndFlush( client.name() + ": " + msg + "\n");
            }
        }
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        Client client = clients.get(channel.remoteAddress().toString());
        channels.remove(ctx.channel());
        for (Channel ch : channels) {
            ch.writeAndFlush("[CHAT-BOT] - " + client.name() + " leave\n");
        }
        clients.remove(channel.remoteAddress().toString());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
